# BPM 实例详情与流转轨迹设计

## 目标

在当前 Hunyuan BPM 运行闭环基础上，统一管理员端和员工端的流程详情语义，让一个流程实例能够清楚回答：

- 现在走到哪一步。
- 谁在什么时候处理过。
- 每次处理的动作、意见、前后处理人是什么。
- 当前表单快照和当前节点摘要是什么。

本阶段不追求完整 BPMN 图高亮，也不新增会签、加签、委派等审批动作。先把“看得懂、查得清、可验收”的详情与轨迹能力打牢。

## 当前依据

当前仓库已经具备以下基础：

- 后端已有实例详情接口：`/bpm/instance/detail/{instanceId}` 与 `/app/bpm/instance/detail/{instanceId}`，共同走 `BpmInstanceService#getDetail`。
- 后端已有任务详情接口：`/bpm/task/detail/{taskId}`，走 `BpmTaskService#getDetail`。
- 后端已有动作日志表与 DAO：`BpmTaskActionLogDao#queryByInstanceId`。
- 前端员工端已有详情抽屉：`bpm-instance-detail-drawer.vue`，能展示基础信息、表单快照和动作轨迹。
- 前端管理员端实例列表、任务列表已经具备详情入口，但详情展示和员工端语义还没有形成统一组件边界。
- 参考仓 Yudao/RuoYi 的价值主要是“流程详情、流转记录、任务动作可观测性”的机制，不是接口、命名或模块边界的直接迁移。

## 设计原则

- 以 Hunyuan 自有运行表和动作日志为事实来源，不把 Flowable 内部对象暴露给前端业务页面。
- 先复用现有 `BpmTaskActionLogVO`，只在现有字段不足时增加 Hunyuan 语义字段。
- 管理员端和员工端共用同一套详情展示语义，权限由入口接口和菜单控制。
- 保持增量改造，不重做现有 BPM runtime，不引入新依赖。
- 所有新增行为必须有 Maven 或 Vitest 契约测试。

## 后端设计

### 详情聚合

`BpmInstanceService#getDetail(Long instanceId)` 继续作为实例详情聚合入口，返回 `BpmInstanceDetailVO`。它负责聚合：

- 实例基础信息：编号、标题、摘要、运行状态、结果状态、发起人、发起部门、发起时间、完成时间。
- 表单快照：`currentFormDataSnapshotJson`。
- 当前节点摘要：`currentNodeSummaryJson`。
- 当前待办任务：从 `t_bpm_task` 查询 `taskState = PENDING` 的任务，按到达时间或任务 ID 排序。
- 动作轨迹：从 `t_bpm_task_action_log` 查询实例维度日志。

### 建议新增字段

在 `BpmInstanceDetailVO` 中增加当前任务列表字段：

```java
@Schema(description = "当前待办任务")
private List<BpmTaskVO> currentTasks;
```

保留已有 `actionLogs` 字段。这样前端能够同时展示“当前在哪”和“过去发生了什么”，无需解析 `currentNodeSummaryJson`。

### 任务详情

`BpmTaskService#getDetail(Long taskId)` 继续返回 `BpmTaskDetailVO`。任务详情只补齐与实例详情一致的动作轨迹语义，不新增独立流程图结构。

如果任务详情需要展示当前实例待办任务，可通过 `instanceId` 复用实例详情接口，不在任务详情里复制整份实例视图。

### 排序语义

动作轨迹必须按 `actionAt` 升序展示；同一时间内按 `actionLogId` 升序稳定排序。

当前待办任务必须按 `assignedAt` 升序展示；同一时间内按 `taskId` 升序稳定排序。

### 状态与动作映射

后端只返回枚举值和动作类型，不返回前端展示文案。前端负责展示文案映射。

本阶段必须覆盖已有动作：

- `APPROVED`
- `REJECTED`
- `RETURNED_TO_INITIATOR`
- `TRANSFERRED`
- `INSTANCE_CANCELLED`
- `RESUBMITTED`

未知动作类型在前端原样展示，避免新动作上线时页面空白。

## 前端设计

### 统一组件

将员工端已有 `bpm-instance-detail-drawer.vue` 作为统一详情抽屉基础，增强后供以下页面复用：

- 员工端我的申请：`runtime/my-instance-list.vue`
- 员工端我的待办：`runtime/my-todo-list.vue`
- 员工端我的已办：`runtime/my-done-list.vue`
- 管理员端实例列表：`instance/instance-list.vue`

管理员端任务列表可以继续使用任务详情弹层；若需要展示实例全量轨迹，则从任务行的 `instanceId` 打开统一实例详情抽屉。

### 页面内容

统一详情抽屉展示四块内容：

- 基础信息：流程编号、标题、摘要、发起人、发起部门、运行状态、结果状态、发起时间、完成时间。
- 当前待办：当前任务名称、处理人、到达时间；无待办时展示“暂无当前待办”。
- 表单快照：继续用只读 JSON/表单快照展示，不在本阶段做动态编辑。
- 动作轨迹：动作人、动作类型、动作时间、意见、转办前后处理人。

### 展示约束

- 不在详情抽屉中加入解释性大段文案。
- 轨迹列表为空时显示空状态，不视为错误。
- JSON 快照保持可复制、可换行、不会撑破抽屉。
- 抽屉宽度沿用当前页面风格，不新增全屏详情页。

## API 与契约

保持现有接口路径不变：

- 管理员实例详情：`GET /bpm/instance/detail/{instanceId}`
- 员工实例详情：`GET /app/bpm/instance/detail/{instanceId}`
- 管理员任务详情：`GET /bpm/task/detail/{taskId}`

前端 API 类型需要增加 `currentTasks?: BpmTaskRecord[]`，并保持 `actionLogs` 非空数组默认语义。若后端返回 `null`，前端组件按空数组处理。

## 测试方案

### 后端

新增或扩展 `BpmRuntimeDetailServiceTest`：

- 实例详情返回基础信息、表单快照、动作轨迹。
- 实例详情返回当前待办任务，并按 `assignedAt/taskId` 稳定排序。
- 不存在的实例返回 `DATA_NOT_EXIST`。

扩展 `BpmTaskDetailServiceTest`：

- 任务详情动作轨迹按实例维度返回。
- 任务不存在时返回 `DATA_NOT_EXIST`。

运行命令：

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmRuntimeDetailServiceTest,BpmTaskDetailServiceTest test
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test
```

### 前端

扩展 BPM 契约测试：

- `runtime.ts` 类型和接口保留实例详情接口。
- 统一详情抽屉包含当前待办、表单快照、动作轨迹展示区域。
- 管理员实例列表和员工运行端页面都引用同一详情抽屉。

运行命令：

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

### 可选浏览器验收

在前后端服务可用时，使用长连接 Playwright MCP controller 验收：

- 打开员工端我的申请，查看一个实例详情。
- 确认详情抽屉展示基础信息、当前待办或空状态、表单快照、动作轨迹。
- 打开管理员端实例列表，确认同一实例详情语义一致。

浏览器截图和运行日志只作为本地运行证据，不提交仓库。

## 非目标

本阶段不做：

- BPMN 流程图高亮。
- 会签、或签、加签、减签、委派、管理员跳转节点。
- 业务单据详情页深度嵌入。
- 复制 Yudao/RuoYi 的 API 路径、页面壳、权限模型或 Flowable 对象结构。
- 新增第三方依赖。

## 验收标准

- 管理员端和员工端都能打开实例详情，并看到一致的基础信息、当前待办、表单快照、动作轨迹。
- 实例详情能直接回答“当前在哪一步”和“过去发生了什么”。
- 后端详情接口不暴露 Flowable 内部类名或引擎对象。
- Maven BPM 测试通过。
- BPM 前端契约测试和 `@hunyuan/system` typecheck 通过。
- 当前工作区中的 `.superpowers/`、`test-results/`、`pnpm-workspace.yaml` 等非 BPM 噪声不进入本功能提交。
