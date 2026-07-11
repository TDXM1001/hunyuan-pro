# BPM 模块 M1：流程建模与编译平台设计

- 日期：2026-07-11
- 状态：模块设计基线
- 优先级：P0
- 前置条件：审批数据治理完成验收

## 1. 结论

本模块把 Hunyuan BPM 从“authored `userTask` 顺序数组 + 固定多人片段”升级为“受控树形 AST + 可组合 BPMN 片段编译平台”。首期支持用户任务、抄送任务、排他分支、并行分支和包容分支；开始、结束和汇合由模型与编译器显式管理。

该模块不是完整 BPMN 编辑器。它只开放经过 Hunyuan 校验、可解释、可冻结和可验收的企业审批结构，不开放自由连线、任意循环、任意 EL 或脚本。

## 2. 当前事实

- `SimpleModelBpmnCompiler` 按 `nodes[]` 顺序循环。
- 普通节点只生成 `userTask`。
- 顺序多人展开为连续 user task。
- `parallelAll` 展开为固定 split、成员 task、join。
- 编译快照已经保存 authored/compiled JSON，是演进的重要基础。
- 发布校验、候选预检、更新认领和定义版本已经存在。

主要问题不是功能数量，而是编译器控制流结构只能表达一个入口和一个出口的线性片段，继续增加网关会导致条件分支、快照、ID 和连接逻辑混杂在单个类中。

## 3. 参考借鉴与优化

| 参考机制 | 决策 | Hunyuan 优化 |
| --- | --- | --- |
| Yudao 树形节点设计器 | `ADAPT` | 保留树形易用性，使用 Hunyuan 类型和页面规范 |
| 条件、并行、包容、路由节点 | `ADAPT` | 统一为受控 branch AST，不复制数字节点类型 |
| `SimpleModelUtils` 递归构建 BPMN | `ADAPT` | 拆成 NodeCompiler 和 FragmentComposer，避免工具类膨胀 |
| 条件表达式模式 | 部分 `REJECT` | 普通用户只配置类型化规则；登记表达式使用受控 key |
| 完整 BPMN 自由设计 | `DEFER` | 先完成企业审批常用结构和兼容基线 |

## 4. 目标模型

### 4.1 根模型

```json
{
  "schemaVersion": 2,
  "nodes": [],
  "settings": {
    "maxBranchDepth": 3
  }
}
```

`nodes` 是有序片段。分支节点内部拥有自己的 `branches[].nodes[]`，分支完成后固定汇合，再进入父级下一个节点。该结构允许受控嵌套，但不允许任意节点引用和回边。

### 4.2 节点种类

首期公共节点类型：

```text
USER_TASK
HANDLE_TASK
COPY_TASK
EXCLUSIVE_BRANCH
PARALLEL_BRANCH
INCLUSIVE_BRANCH
```

后续 M4、M5 通过相同扩展点加入：

```text
DELAY
EXTERNAL_TRIGGER
CALL_PROCESS
```

开始和结束是根流程及分支片段的编译边界，不作为普通用户可删除节点。

### 4.3 人工、办理和抄送节点

- `USER_TASK` 表达审批决定，允许通过、拒绝、退回及节点策略允许的高级动作。
- `HANDLE_TASK` 表达必须由指定人员完成的业务办理，只允许完成、退回、转办和委派，不产生审批通过/拒绝结论。
- `COPY_TASK` 是非阻塞通知片段，进入节点时创建结构化抄送记录后立即继续；抄送失败进入通知重试，不阻塞流程令牌。
- 三类节点复用候选来源，但动作、结果枚举和 trace 文案必须分开。

### 4.4 分支契约

- `EXCLUSIVE_BRANCH`：按顺序匹配第一条规则；必须有默认分支。
- `PARALLEL_BRANCH`：所有分支同时进入，全部完成后汇合。
- `INCLUSIVE_BRANCH`：进入所有匹配分支；没有匹配时进入默认分支；实际进入的分支集合决定汇合条件。
- 每个分支拥有稳定 `branchKey`、展示名、条件和内嵌节点。
- 节点 key 在整个定义 AST 中全局唯一，不因嵌套层级放宽。

## 5. 条件规则

### 5.1 普通规则

条件只能引用发布表单 schema 字段和受控运行上下文：

```text
FORM_FIELD
START_EMPLOYEE
START_DEPARTMENT
INSTANCE_CONTEXT
```

字段操作符按值类型白名单开放：等于、不等于、大于、大于等于、小于、小于等于、包含、不包含、为空、不为空、集合包含。

### 5.2 登记表达式

复杂业务判断不保存用户 EL，只保存 `expressionKey + version + parameters`。表达式实现由后端登记、测试、授权和发布，返回布尔值及可审计原因。

M1 只交付登记协议、注册表、发布期存在性/版本校验和运行期审计，不内置面向具体业务的通用表达式。没有后端登记实现的 `expressionKey + version` 必须阻断发布；后续业务表达式按对应业务模块独立注册，不扩大普通设计者的执行权限。

### 5.3 决定事实

运行时新增结构化路由决定，至少记录：实例、定义版本、路由节点、输入数据版本、匹配分支集合、默认分支使用情况、计算时间和错误状态。员工视图按字段权限裁剪条件值，管理端可查看完整诊断。

## 6. 编译架构

### 6.1 核心接口

```text
ProcessAstParser
ProcessAstValidator
NodeCompiler<TNode>
BpmnFragment
FragmentComposer
CompiledDefinitionArtifact
```

`BpmnFragment` 不只返回 XML 字符串，必须描述：

```text
entryElementIds
exitElementIds
generatedElements
sequenceFlows
compiledNodeSnapshots
runtimeRequirements
```

### 6.2 编译流程

```text
解析 schemaVersion
-> AST 结构校验
-> 业务语义校验
-> 节点递归编译
-> 片段连接
-> 全局 ID 冲突校验
-> BPMN 模型校验
-> 生成 XML 与编译快照
-> 部署认领
```

### 6.3 ID 命名

所有生成 ID 使用固定命名空间和稳定转义规则。authored key、gateway key、branch flow key、timer/event key 全局检查长度、字符和冲突。运行时通过 compiled snapshot 映射 authored 身份，不解析 ID 字符串猜测业务含义。

### 6.4 路由执行钩子

排他和包容分支在 split gateway 前编译一个 Hunyuan 内部 service task，固定使用 Spring delegate expression `${hunyuanRouteDecisionDelegate}`。service task 只携带经过校验和 XML 转义的 `routeNodeKey`，delegate 根据发布定义快照、Hunyuan 实例 ID 和当前数据版本计算分支；用户条件和值不得进入 delegate expression 或 Flowable EL。

启动流程前先在同一事务内建立或更新 Hunyuan 实例事实，再把 `hunyuanInstanceId` 作为受控变量传入 Flowable。这样根路由、嵌套路由、汇合后紧接路由和退回重提都通过同一个执行钩子处理，不要求任务服务猜测“下一个节点”。delegate 与触发它的 Flowable 推进共享事务；计算、持久化或快照校验失败时，令牌不得越过路由节点。

## 7. 发布治理

发布检查至少覆盖：

- schemaVersion 是否支持。
- 节点类型及嵌套深度。
- 节点和分支 key 唯一性。
- 分支数量、默认分支和空分支。
- 条件字段、值类型和操作符。
- 包容分支可能的空匹配。
- 并行/包容分支中的人工节点不得配置 `EDITABLE` 字段；M1 不定义并发写合并语义。
- 候选人和连接器的确定性预检。
- 登记表达式 key、版本和参数契约是否存在。
- 编译后 BPMN 和全部生成 ID。

错误使用稳定 code、nodeKey、branchKey、fieldKey 和修复提示进入现有发布报告。

## 8. 运行时数据流

```text
实例启动或前置节点完成
-> 锁定实例和当前数据版本
-> 读取冻结路由快照
-> Hunyuan 计算匹配分支
-> 写路由决定
-> 写受控 Flowable 变量
-> 驱动引擎进入相应分支
-> 任务投影携带 authored branch 身份
-> 汇合后继续父级下一个节点
```

用户条件不直接编译为任意 EL。Flowable 只比较 Hunyuan 预先计算的受控分支 key 或分支集合。

路由事实的幂等边界是 `(instanceId, engineProcessInstanceId, routeNodeKey)`；`formDataVersion` 是决定输入而不是实例代际。当前模型禁止回边，因此同一 Flowable 实例内同一路由节点只能产生一个有效决定。退回重提会创建新的 `engineProcessInstanceId`，即使表单版本未变化也必须写入新一代路由事实，旧事实保持只读。

## 9. 前端设计器

- 在现有模型编辑器内升级，不创建第二套相互竞争的设计器。
- 分支节点显示清晰的分叉、分支条件、默认分支和汇合。
- 节点属性继续承载候选人、字段权限和运行规则。
- 条件字段从当前表单 schema 选择，操作符随字段类型变化。
- 无效历史字段保留为可见错误，不静默删除。
- 模型导入导出使用带 `schemaVersion` 的 Hunyuan JSON；导入先完整校验，不接受未识别节点或覆盖当前草稿前的静默转换。
- 只读预览和运行图共享 authored 结构，但运行状态来自后端结构化投影。

## 10. 兼容与迁移

- `schemaVersion` 缺失的现有模型按 v1 线性模型读取。
- 发布新版本时可将 v1 规范化为 v2，但不重写历史定义快照。
- 现有单人、顺序多人和 `parallelAll` 编译结果保持语义兼容。
- 旧实例继续使用原部署和编译快照。
- 编译器重构必须先建立旧模型黄金测试，证明 XML 关键结构和运行行为不退化。

## 11. 交付块

1. AST v2、递归遍历、静态校验、登记表达式协议和旧模型兼容。
2. NodeCompiler/FragmentComposer 重构、稳定 ID 命名及旧节点黄金回归。
3. 路由执行 delegate、路由事实账本、实例启动/重提事务顺序和结构化 trace。
4. 排他分支编译、运行和真实双路径/默认路径验收。
5. 独立并行分支、固定汇合、结构化分支身份和并发验证。
6. 包容分支、实际进入分支集合、动态汇合和幂等验证。
7. `HANDLE_TASK` 的独立完成语义和 `COPY_TASK` 的非阻塞抄送事实。
8. 设计器、带 `schemaVersion` 的导入导出、只读预览和运行详情。
9. 样板业务多路径、全量兼容、真实验收和三份基线回写。

这些批次属于同一个模块，不在批次之间重新决定总体架构。

## 12. 验收矩阵

| 场景 | 必须证明 |
| --- | --- |
| 旧线性模型 | 发布和运行行为不变 |
| 排他分支 | 只进入一个匹配分支，默认分支可用 |
| 并行分支 | 所有分支同时进入且全部完成后只汇合一次 |
| 包容分支 | 进入全部匹配分支，仅等待实际进入分支 |
| 条件安全 | 员工无法提交分支结果，任意 EL 不可进入模型 |
| 数据一致 | 路由使用确定的 `formDataVersion` |
| 重试 | 同一版本同一路由节点只产生一个有效决定 |
| 退回重提 | 新引擎实例按最新数据重新路由；即使数据版本未变化也产生新一代事实，旧事实保留 |
| 办理任务 | 只能完成、退回、转办和委派，不产生审批通过/拒绝结论 |
| 设计时抄送 | 生成一次结构化抄送事实后立即继续，通知失败不阻塞令牌 |
| 登记表达式 | 未登记 key/version 阻断发布，运行时只执行后端登记实现并记录原因 |
| 导入导出 | v2 资产完整往返；未知版本、未知节点和无效引用在覆盖草稿前失败 |
| 并发写边界 | 并行和包容分支中的人工节点不能配置可编辑字段 |
| 详情 | 页面能解释走了哪些分支及原因 |
| Flowable 兼容 | 部署、流转、取消和汇合通过集成验证 |

## 13. 完成定义

模块只有在 AST、编译器、发布检查、登记表达式协议、三种分支运行时、结构化路由事实、办理/抄送节点、设计器、导入导出、运行详情、旧模型兼容和真实多路径业务验收全部通过后关闭。只完成排他条件分支不算 M1 完成。
