# BPM 模块 M5：子流程与流程组合设计

- 日期：2026-07-11
- 状态：模块设计基线
- 优先级：P1
- 前置条件：M1、M3、M4

## 1. 结论

本模块为 Hunyuan 增加可复用流程组合能力：父定义在发布时冻结子定义版本，运行时创建结构化父子实例关系，通过类型化输入输出映射传递数据，并明确完成、拒绝、取消、超时和重提的传播策略。

首期同步等待子流程完成，不支持任意跨流程跳转。多子流程实例在单子流程稳定后建设，复用 M2 的完成策略和 M4 的超时机制。

## 2. 当前事实

- 当前定义和实例相互独立，没有 call activity 或父子实例投影。
- 定义发布、当前版本和可发起版本已有稳定治理。
- 实例已经有业务键、数据快照、运行状态和结果状态。
- 取消、退回重提、回调和任务投影已经形成 Hunyuan 语义。

这些能力可复用，但不能只在 Flowable 中创建 callActivity 而不建立 Hunyuan 父子关系，否则详情、取消、业务回写和历史解释都会失真。

## 3. 参考借鉴与优化

| 参考能力 | 决策 | Hunyuan 优化 |
| --- | --- | --- |
| callActivity | `ADAPT` | 父定义发布时冻结子定义 ID/版本 |
| 输入输出变量映射 | `ADAPT` | 基于发布 schema 的类型化映射和版本合并 |
| 主流程/表单决定子流程发起人 | `ADAPT` | 使用身份网关和明确空值策略 |
| 子流程超时 | `ADAPT` | 复用 M4 SLA/计时器和运营处置 |
| 子流程 multi-instance | `ADAPT` 后置 | 复用 M2 成员集合与完成策略 |
| 子流程拒绝直接结束主流程 | 不固定照搬 | 由父节点传播策略显式配置 |

## 4. 模型契约

```text
CallProcessNode
  childDefinitionKey
  versionPolicy: LOCK_AT_PARENT_PUBLISH
  startEmployeePolicy
  inputMappings[]
  outputMappings[]
  completionPolicy
  failurePolicy
  cancellationPolicy
  timeoutPolicy
  multiInstancePolicy nullable
```

父定义发布时解析 `childDefinitionKey` 的当前可用版本，并把 `childDefinitionId + version + schema snapshot hash` 冻结到 compiled snapshot。子定义后来发布新版本不影响已发布父定义。

## 5. 输入输出映射

```text
VariableMapping
  sourceFieldKey
  targetFieldKey
  transformKey nullable
  required
  writePolicy
```

- 输入在子流程启动前从父实例确定版本读取并校验。
- 子流程拥有独立表单快照和版本历史。
- 输出只在子流程达到允许终态时合并。
- 合并提交父实例预期版本并写字段变更审计。
- 并行子流程默认禁止写相同父字段；冲突必须在发布时阻断。
- transformKey 是登记转换器，不接受脚本。

## 6. 父子实例投影

新增 `t_bpm_instance_link`：

```text
link_id
parent_instance_id
parent_engine_process_instance_id
parent_node_key
child_instance_id
child_engine_process_instance_id
child_definition_id/version
child_index/child_total
link_state
completion_result
propagation_policy_snapshot
create/complete/cancel/timeout_time
```

该表是 Hunyuan 父子关系事实。Flowable execution/callActivity ID 仅作为内部定位字段。

## 7. 状态传播

### 子流程通过

校验并合并输出，写父实例数据版本和审计，然后完成父 call node，继续主流程。

### 子流程拒绝

父节点可配置：

```text
REJECT_PARENT
RETURN_PARENT_TO_INITIATOR
MANUAL_INTERVENTION
```

不提供静默忽略拒绝。

### 主流程取消或撤回

按父节点冻结策略取消所有活动子实例，关闭子任务、审批组、计时器和外部等待，再关闭父节点。传播必须幂等。

### 子流程超时

复用 M4：提醒、取消子流程并转人工、拒绝父流程。首期不允许超时自动通过高风险子流程。

### 退回重提

父流程进入 `WAIT_RESUBMIT` 时关闭当前执行代的活动子流程。重提产生新引擎实例和新的 link；旧 link 保留历史事实。

## 8. 多子流程实例

单子流程闭环后支持：

```text
source: FIXED_QUANTITY | NUMBER_FIELD | LIST_FIELD
mode: SEQUENTIAL | PARALLEL_ALL | ANY | RATIO
itemMapping
resultAggregation
```

成员集合和完成策略复用 M2，输出聚合使用登记 aggregator。禁止多个子实例无策略地覆盖同一父字段。

## 9. 前端体验

- 设计器选择可调用的已发布子定义，不允许自由输入 key。
- 显示冻结版本、输入输出映射、发起人、传播和超时策略。
- 发布检查验证循环引用、版本可用性、字段类型和输出冲突。
- 实例详情以树形关系展示父子流程、当前状态和结果。
- 子流程页面可返回父流程上下文，但权限分别校验。
- trace 将父节点和子流程关键事件关联，不把所有子任务平铺成不可读列表。

## 10. 循环与安全

- 发布时构建定义依赖图，阻断直接和间接循环引用。
- 设置最大调用深度，首期固定为 5。
- 子流程发起人必须通过身份网关验证。
- 父实例权限不自动授予子流程业务数据权限。
- 输入输出按字段白名单，隐藏字段展示继续遵循各定义权限。
- 取消传播、输出合并和父节点完成遵循固定锁顺序。

## 11. 交付块

1. 定义依赖、版本冻结和循环校验。
2. 单子流程编译、父子投影和同步完成。
3. 输入输出映射、数据版本和字段审计。
4. 拒绝、取消、撤回、重提和超时传播。
5. 多子流程实例、完成策略和结果聚合。
6. 父子详情、真实业务组合验收和基线回写。

## 12. 验收矩阵

覆盖版本冻结、循环阻断、输入类型、正常完成、输出合并、版本冲突、子拒绝、父取消、撤回重提、超时、重复事件、服务重启、多子流程并发和权限隔离。

## 13. 完成定义

只有单/多子流程、版本冻结、映射、父子投影、状态传播、超时、运行详情、并发幂等和真实组合业务全部闭环后，M5 才完成。仅能启动 callActivity 不算完成。
