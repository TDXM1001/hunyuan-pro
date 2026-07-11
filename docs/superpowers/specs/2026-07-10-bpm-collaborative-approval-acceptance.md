# BPM 并行全员会签验收记录

- 验收日期：2026-07-10
- 最终门禁时间：2026-07-11 01:17（Asia/Shanghai）
- 适用仓库：`E:\my-project\hunyuan-pro`
- 对应设计：`docs/superpowers/specs/2026-07-10-bpm-collaborative-approval-design.md`
- 结论：`approvalMode=parallelAll` 受限并行全员会签已完成代码、数据库兼容、自动化测试和真实运行态验收

## 交付范围

本轮完成 Hunyuan 原生“审批组”能力：

- 设计器使用现有 `userTask` 配置 `parallelAll`，只接受至少两名有效且不重复的指定员工。
- 编译器生成固定的 `parallelGateway split -> N 个独立 userTask -> parallelGateway join`。
- 新增审批组投影，并由成员任务通过 `approval_group_id` 归组。
- 全员通过后收敛；任一拒绝、退回、实例撤回或取消时关闭整组并取消其他待办。
- 待办、已办、任务详情、实例详情和 trace 输出结构化审批组摘要或详情。
- 会签成员允许转办、委派和实例级撤回；加签、减签由前后端共同阻止。
- 员工任务详情改走 `/app/bpm/task/detail/{taskId}`，仅允许当前员工读取本人待办；管理端详情接口和权限码保持不变。
- 只读设计器预览补齐 BPMN DI，可稳定导入并显示固定分叉、成员和汇聚拓扑。

明确未交付：

- 或签、比例审批、Flowable multi-instance。
- 动态成员、角色或部门展开为会签成员。
- 通用网关 DSL、条件路由、子流程、定时器。
- 通用 BPMN 图编辑器和运行态图形高亮。

## 一次架构自审

| 审查项 | 结论 |
| --- | --- |
| 范围边界 | 仅扩展 `parallelAll` 固定片段，不新增通用节点类型或通用网关 DSL。 |
| 领域边界 | Flowable 网关、execution 和 engine task ID 留在编译器与运行时网关内；公共契约只暴露 Hunyuan 审批组、任务、员工和实例。 |
| 数据事实 | 审批组保存稳定组状态与计数；成员事实仍由独立 `t_bpm_task` 表达，不从任务名或页面 JSON 推导。 |
| 兼容策略 | 新表和新字段均为增量；普通任务的 `approval_group_id`、任务摘要和实例审批组列表保持空值，不改既有路径和基础字段。 |
| 并发边界 | 审批组是成员动作的串行化边界；组关闭或任务已处理后不再驱动第二次 Flowable 动作。 |
| 权限边界 | 员工详情只读取本人任务，未把管理端 `bpm:task:detail` 权限下放给普通员工。 |
| 前端数据源 | 页面消费后端结构化审批组，不解析 `runtimeAssignmentSnapshotJson`、节点 key 后缀或任务名称。 |
| 依赖与迁移 | 未新增 Maven、pnpm 或运行时依赖；未整体迁移 Yudao/RuoYi 代码或契约。 |

审查结论：设计中的重要决定、公共契约、权限、并发和兼容风险均已在同一模块内闭环，没有发现需要重新打开模块设计的架构冲突。

## 并发与锁顺序

成员通过、拒绝和退回统一执行以下锁顺序：

```text
普通查询取得 groupId
-> selectByIdForUpdate(approvalGroup)
-> selectByIdForUpdate(task)
-> 锁同组待办
```

普通查询只用于定位审批组，不改变状态。取得组锁后再次检查组仍为 `PENDING`，取得任务锁后再次检查任务仍为待办且属于当前员工，再锁定同组待办并执行成员动作。聚焦测试覆盖“先锁组、后锁任务”、非最后成员保持待处理、最后成员关闭组、拒绝或退回只终止一次，以及重复动作不重复驱动 Flowable。

审批组创建由 `(engine_process_instance_id, approval_group_key)` 唯一约束兜底；并发创建冲突后回查同一组。重提会生成新的 Flowable process instance，因此旧组与新组不会合并。

## SQL 与兼容

用户已执行：

```text
数据库SQL脚本/mysql/sql-update-log/v3.43.0.sql
```

脚本创建 `t_bpm_approval_group`，并为 `t_bpm_task` 增加可空 `approval_group_id` 与 `(approval_group_id, task_state)` 索引。脚本不回填历史任务、不修改已发布定义、不重写 Flowable 数据。

普通流程的任务摘要为 `approvalGroup=null`，实例详情与 trace 的审批组列表为空。L8 真实流程已证明普通任务创建、通知、审批和结束均不受影响。

## 真实验收对象

```text
categoryId=5
formId=4
modelId=5
definitionId=7

admin=1
huke=2
zhuoda=44
qinjiu=48
shanyi=47
```

### L1：设计、校验、发布与画布

- 模型节点为 `finance_review`，模式为 `parallelAll`，成员为 `[1, 2, 44]`。
- 模型发布成功，生成 `definitionId=7`，发起草稿可读取。
- `validate` 请求成功但返回 `data=null`。
- `simulate` 请求成功但返回 `data=null`，因此本记录不把 simulate 当作 split、member、join 文本证据。
- 拓扑由三类独立证据共同证明：
  - `SimpleModelBpmnCompilerTest` 验证编译后的 split、成员任务和 join。
  - 设计器真实画布导入 start、split、3 个成员、join、end 和 8 条 sequence flow，显示“当前节点数：1”，本次加载控制台 0 error。
  - L2 真实实例同时产生三条独立成员待办。

设计器证据：

```text
G:\code-mcp\playwright-mcp-temp\runtime\bpm-collaborative-approval-designer-ui-20260710.png
```

### L2：三条并发成员任务与同组投影

- `instanceId=57`。
- 成员任务为 `61`、`62`、`63`，分别属于管理员、胡克、卓大。
- 三条任务均关联 `approvalGroupId=1`。
- 初始组状态为 `PENDING`，进度为 `0/3`。

### L3：全员通过

- 第一、第二名成员通过后，审批组仍为 `PENDING`，进度依次为 `1/3`、`2/3`。
- 第三名成员通过后，审批组为 `APPROVED/ALL_APPROVED`，进度为 `3/3`。
- 实例 `57` 最终通过，当前任务数为 `0`。
- 三名成员均收到“流程会签待办提醒”。

### L4：任一成员拒绝

- `instanceId=58`，`approvalGroupId=2`。
- 胡克在 `taskId=65` 拒绝。
- 审批组变为 `REJECTED/MEMBER_REJECTED`。
- 其他成员任务转为取消，当前任务数为 `0`，实例最终拒绝。

### L5：退回发起人与重提

- `instanceId=59`，卓大在 `taskId=69` 退回。
- 旧组 `approvalGroupId=3` 变为 `RETURNED/MEMBER_RETURNED`，实例进入 `WAIT_RESUBMIT`。
- 管理员、胡克、卓大均无遗留待办。
- 重提后同一 Hunyuan 实例产生新组 `approvalGroupId=4` 和新任务 `70`、`71`、`72`；旧组保留为历史事实。

### L6：实例撤回与取消

- 撤回：`instanceId=60`、`approvalGroupId=5`，组状态为 `CANCELLED/INSTANCE_RECALLED`，实例进入 `WAIT_RESUBMIT`。
- 取消：`instanceId=61`、`approvalGroupId=6`，组状态为 `CANCELLED/INSTANCE_CANCELLED`，实例最终取消。
- 两条路径均取消整组待办，没有遗留活动成员任务。

### L7：高级动作、任务详情与页面

- `instanceId=62`、`approvalGroupId=7`，任务为 `79`、`80`、`81`。
- `taskId=79` 经转办、委派后仍保持同一任务和审批组身份，当前处理人为善逸；组成员实际显示为善逸、胡克、卓大。
- 转办和委派不改变成员总数、进度或组状态。
- 加签、减签 API 均返回业务码 `30001`，消息为“并行全员会签成员不支持加签或减签”。
- 待办页面显示“财务会签（2/3）”和“财务会签，0/3 已处理”。
- 任务详情显示审批组、`0/3` 进度和三名当前成员。
- 高级菜单显示转办、委派、撤回，不显示加签、减签。

员工端详情权限修复：

- 新增 `GET /app/bpm/task/detail/{taskId}`。
- `BpmTaskService.getMyDetail()` 校验任务处理人等于当前员工。
- 管理端 `getDetail()` 与 `bpm:task:detail` 权限保持原样。
- 前端员工任务详情改用员工端接口，未扩大普通员工权限。

页面证据：

```text
G:\code-mcp\playwright-mcp-temp\runtime\bpm-collaborative-approval-l7-todo-ui-20260710.png
G:\code-mcp\playwright-mcp-temp\runtime\bpm-collaborative-approval-l7-detail-ui-20260710.png
G:\code-mcp\playwright-mcp-temp\runtime\bpm-collaborative-approval-l7-advanced-ui-20260710.png
```

### L8：普通流程兼容

- 普通单人流程使用 `modelId=6`、`definitionId=8`、`instanceId=63`、`taskId=82`。
- 待办和已办的 `approvalGroup` 均为 `null`。
- 实例详情与 trace 的审批组列表均为空。
- 通知仍为普通“流程待办提醒”。
- 实例最终通过。

## API 证据

```text
G:\code-mcp\playwright-mcp-temp\runtime\bpm-collaborative-approval-l1-setup-20260710.json
G:\code-mcp\playwright-mcp-temp\runtime\bpm-collaborative-approval-l2-l7-20260710.json
G:\code-mcp\playwright-mcp-temp\runtime\bpm-collaborative-approval-l8-20260710.json
```

这些文件是运行态证据，保留在 Playwright MCP 运行目录，不提交到仓库。

## 自动化门禁

| 门禁 | 结果 |
| --- | --- |
| BPM 后端聚焦测试 | PASS；`74` tests，Failures `0`，Errors `0`，Skipped `0`；完成于 2026-07-11 01:16:36+08:00 |
| Flowable 兼容测试 | PASS；`1` test，Failures `0`，Errors `0`，Skipped `0`；完成于 2026-07-11 01:17:12+08:00 |
| 前端四文件合同测试 | PASS；`4` files，`57` tests；Duration `517ms` |
| `@hunyuan/system` 类型检查 | PASS；`vue-tsc --noEmit --skipLibCheck`，退出码 `0` |
| 设计器 DI 聚焦 TDD | RED：新增 `3` 个 DI 合同按预期失败；GREEN：`13/13` 通过 |

执行命令：

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=SimpleModelValidatorTest,SimpleModelBpmnCompilerTest,BpmTaskAssignmentResolverTest,BpmApprovalGroupServiceTest,BpmTaskServiceTest,BpmTaskProjectionServiceTest,BpmTaskDetailServiceTest,BpmRuntimeDetailServiceTest,BpmInstanceTraceServiceTest' test
```

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin '-Dtest=BpmFlowableCompatibilityTest' test
```

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts --dom
```

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

## 已知边界

- 本轮没有把 `parallelAll` 扩展成通用并行网关能力。
- 并发正确性由固定锁顺序、事务内状态复查、唯一约束、重复动作测试和真实多任务运行态共同覆盖；本轮未引入独立压力测试框架。
- Maven 仍提示本机 `settings.xml` 第 235 行存在非预期文本、`compilerVersion` 已废弃，以及兼容测试中的既有 Spring/MyBatis/FreeMarker 警告；门禁均通过，本轮不处理这些仓库外或既有提示。
- Windows 控制台在部分 Maven 中文日志中仍出现编码显示问题；源码、SQL 和文档均按 UTF-8 读取与写入。
- 未新增依赖。
