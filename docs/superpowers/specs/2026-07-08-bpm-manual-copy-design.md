# BPM 手工抄送运行端设计

## 背景

当前 `hunyuan-pro` BPM 已经形成 Hunyuan 原生运行链路：员工可发起流程、审批人可处理待办、发起人可查看申请，实例详情已经统一展示当前待办和动作轨迹。

本轮不继续扩展 Flowable 或设计器能力，而是补齐真实业务里常见的“审批时顺手知会相关人”场景。参考仓 `ruoyi-vue-pro-master` 有抄送分页、手工抄送和自动抄送节点等机制，但本轮只吸收最小机制，不迁移其 API、页面壳、权限模型或 Flowable 对象结构。

## 当前仓库事实

- `t_bpm_instance_copy` 已存在，字段包括 `copy_id`、`instance_id`、`target_employee_id`、`copy_type`、`read_state`、`reason_snapshot`、`sent_at`、`read_at`。
- `BpmInstanceCopyEntity`、`BpmInstanceCopyDao`、`BpmInstanceCopyMapper.xml` 已存在，但 DAO 还没有查询方法。
- `BpmCopyReadStateEnum` 已存在，当前只有 `UNREAD=0` 和 `READ=1`。
- 员工运行端已有 `/app/bpm/my-instance`、`/app/bpm/my-todo`、`/app/bpm/my-done` 和实例详情。
- 前端已有 `bpm-instance-detail-drawer.vue`，管理员端和员工端实例列表已经复用它。
- BPM runtime 菜单当前已有 `可发起流程`、`我的申请`、`我的待办`、`我的已办`，尚无 `我的抄送`。

## 目标

本轮目标是补齐 **审批时手工抄送 P0**：

1. 审批人处理待办时，可选填抄送员工。
2. 系统在审批动作成功后写入 `t_bpm_instance_copy`。
3. 被抄送员工可以在“我的抄送”看到相关流程。
4. 被抄送员工可以打开统一实例详情抽屉查看流程详情。
5. 打开详情后，该抄送记录可以标记为已读。

## 非目标

- 不做流程模型自动抄送节点。
- 不改 simpleModel 设计器、BPMN 编译器或发布流程。
- 不做会签、或签、加签、减签、管理员跳转节点。
- 不新增独立管理员抄送管理页。
- 不把 Flowable 任务、历史任务或评论对象直接暴露给前端。
- 不新增依赖，不引入第二套前端页面体系。

## 方案选择

### 方案 A：审批时手工抄送

审批动作表单携带 `copyEmployeeIds`，后端在动作成功后写入 Hunyuan 抄送投影表。前端在待办审批弹框中提供可选抄送人选择。

优点：
- 范围最小。
- 能快速形成真实业务闭环。
- 不碰设计器和编译器。
- 能复用现有实例详情抽屉和 runtime 页面结构。

缺点：
- 不能自动按流程节点配置抄送。

### 方案 B：流程模型自动抄送

在 simpleModel 中增加抄送节点或节点抄送配置，发布后运行态自动生成抄送记录。

优点：
- 更接近成熟 BPM 平台能力。

缺点：
- 会牵动模型设计器、校验器、编译器、运行态投影。
- 当前阶段风险过高，容易打断已闭环的 runtime 主链路。

### 结论

采用 **方案 A：审批时手工抄送**。

自动抄送节点作为后续独立阶段处理。

## 后端设计

### 表单契约

在现有审批动作表单上增加可选字段：

- `BpmTaskApproveForm.copyEmployeeIds`
- `BpmTaskRejectForm.copyEmployeeIds`
- `BpmTaskReturnForm.copyEmployeeIds`

字段语义：

- 类型：`List<Long>`
- 可为空。
- 为空时行为与当前完全一致。
- 后端过滤空值、重复值和当前操作者自己。
- 如果目标员工不存在，返回用户参数错误，不写入部分抄送记录。

本轮不在 `transfer` 动作上做抄送。转办是任务责任流转，不是审批结论动作，先保持单一语义。

### 抄送写入服务

新增 `BpmInstanceCopyService`，负责抄送投影的写入、分页查询和标记已读。

建议方法：

- `createManualCopies(BpmTaskEntity taskEntity, Collection<Long> targetEmployeeIds, String reasonSnapshot, String copyType)`
- `queryMyCopyPage(BpmInstanceCopyQueryForm queryForm)`
- `markRead(Long copyId)`

写入规则：

- `instance_id` 来自任务所属实例。
- `definition_id` 来自实例。
- `definition_node_id` 先用任务当前 `definitionNodeId`，为空则保持空。
- `engine_process_instance_id` 来自实例。
- `source_node_key` 来自任务 `taskKey`。
- `source_node_name` 来自任务 `taskName`。
- `target_employee_id` 和 `target_name_snapshot` 来自组织员工快照。
- `copy_type` 使用 Hunyuan 枚举：
  - `MANUAL_APPROVE_COPY`
  - `MANUAL_REJECT_COPY`
  - `MANUAL_RETURN_COPY`
- `read_state` 默认为 `UNREAD`。
- `reason_snapshot` 使用审批意见或退回意见。
- `sent_at` 使用当前时间。

### 审批动作接入

`BpmTaskService` 现有主链路仍然先完成审批动作，再写入抄送记录：

1. 校验当前员工是否能处理该任务。
2. 完成 approve / reject / returnToInitiator 原有逻辑。
3. 写入动作日志。
4. 如果有 `copyEmployeeIds`，调用 `BpmInstanceCopyService#createManualCopies(...)`。

抄送写入失败时，本轮应让整个审批动作回滚。原因是“审批成功但抄送失败”会造成用户以为已知会但实际没有记录，业务可信度更差。

### 查询接口

新增员工端接口：

- `POST /app/bpm/my-copy`
  - 查询当前登录员工收到的抄送。
  - 支持 `instanceNo`、`title`、`readState`、分页。

- `POST /app/bpm/copy/read/{copyId}`
  - 将当前登录员工自己的抄送标记为已读。
  - 不是自己的抄送返回无权限或数据不存在。

本轮不新增管理员 `/bpm/copy/*` 接口。

### 返回对象

新增 `BpmInstanceCopyVO`：

- `copyId`
- `instanceId`
- `instanceNo`
- `title`
- `copyType`
- `readState`
- `sourceNodeName`
- `targetNameSnapshot`
- `reasonSnapshot`
- `sentAt`
- `readAt`
- `startEmployeeNameSnapshot`
- `runState`
- `resultState`

该 VO 只暴露 Hunyuan 投影字段，不暴露 Flowable 历史任务或评论对象。

## 前端设计

### API

在 `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts` 中新增：

- `BpmInstanceCopyRecord`
- `BpmInstanceCopyPageQueryParams`
- `queryMyBpmCopyPage(params)`
- `markBpmCopyRead(copyId)`

在审批动作表单类型上新增：

- `copyEmployeeIds?: number[]`

### 我的抄送页面

新增页面：

- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/my-copy-list.vue`

页面遵循 `docs/frontend-list-table-page-standard.md`：

- `Page` 外壳。
- 搜索卡 + 表格卡。
- 搜索条件：实例编号、流程标题、已读状态。
- 表格列：实例编号、流程标题、抄送类型、来源节点、抄送原因、已读状态、发送时间、操作。
- 操作：详情。
- 点击详情时先调用 `markBpmCopyRead(copyId)`，再打开现有 `BpmInstanceDetailDrawer`。

页面不添加独立说明文案，不重复菜单标题。

### 待办审批弹框

在 `my-todo-list.vue` 的通过、拒绝、退回弹框中增加可选抄送员工字段。

P0 选择员工方式：

- 优先复用现有组织/角色页面中的员工查询 API。
- 使用多选员工选择控件或轻量选择弹层。
- 不做组织树、常用联系人、部门级批量抄送。

如果当前已有员工选择抽屉可复用，优先复用；如果没有，就做一个 BPM 页面内的最小员工选择弹层，保持与现有页面风格一致。

### 菜单

新增 SQL 增量，为 BPM runtime 增加菜单：

- 菜单名：`我的抄送`
- 路径：`/system/bpm/runtime/my-copy-list`
- 组件：`/system/bpm/runtime/my-copy-list.vue`
- 父级：`流程引擎`
- 排序：放在 `我的已办` 后。

`bpm_runtime_user` 角色应获得该菜单权限，保证 huke 等运行端用户登录后可见。

## 权限与边界

- `my-copy` 查询只返回当前登录员工收到的抄送。
- `markRead` 只能操作当前登录员工自己的抄送。
- 实例详情继续通过现有 `/app/bpm/instance/detail/{instanceId}` 读取。
- 本轮不扩大“被抄送人”的审批权限，被抄送人只能查看详情，不能处理任务。
- 后续若需要限制被抄送人查看表单字段，应单独设计表单字段权限，不在本轮加入。

## 测试与验证

后端测试：

- `BpmInstanceCopyServiceTest`
  - 审批动作写入多名抄送人。
  - 重复员工只写一次。
  - 目标员工不存在时返回错误。
  - 查询我的抄送只返回当前员工记录。
  - 标记已读只能更新当前员工自己的记录。

- `BpmTaskServiceTest`
  - approve 带 `copyEmployeeIds` 后写入抄送。
  - reject 带 `copyEmployeeIds` 后写入抄送。
  - returnToInitiator 带 `copyEmployeeIds` 后写入抄送。
  - 不带抄送时保持原行为。

- schema/API 测试
  - `t_bpm_instance_copy` 字段继续受 schema 测试保护。
  - 新运行端接口不泄漏 Flowable 包名或 Yudao/RuoYi 路径。

前端测试：

- `bpm-api.test.ts`
  - 覆盖 `/app/bpm/my-copy`、`/app/bpm/copy/read/`。
  - 审批表单类型包含 `copyEmployeeIds`。

- `bpm-modules.test.ts`
  - `my-copy-list.vue` 存在。
  - 页面复用 `BpmInstanceDetailDrawer`。
  - 页面包含 `markBpmCopyRead`。
  - 待办页审批动作传递 `copyEmployeeIds`。

推荐验证命令：

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmInstanceCopyServiceTest,BpmTaskServiceTest test
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

浏览器验收在本地服务可用时执行：

1. 审批人登录并进入“我的待办”。
2. 通过、拒绝或退回任务时选择一个抄送人。
3. 被抄送人登录后看到“我的抄送”菜单。
4. 被抄送人在列表中看到该流程。
5. 点击详情后打开统一实例详情抽屉。
6. 列表记录变为已读。

浏览器验收应复用长连接 Playwright MCP controller，不把 runtime 输出写入当前仓库。

## 后续阶段

完成 P0 后，再考虑：

1. simpleModel 自动抄送节点。
2. 抄送消息通知与消息中心未读计数联动。
3. 表单字段级可见性。
4. 管理员抄送审计页。
5. 常用抄送人或组织树批量选择。

## 成功标准

- 审批时手工抄送形成后端、前端、菜单、权限、测试闭环。
- 被抄送员工能独立看到抄送列表并查看实例详情。
- 不改变现有发起、待办、已办、我的申请主链路。
- 不引入设计器、编译器、Flowable 暴露面或外部依赖。
- 所有新增能力都有 Maven 或 Vitest 契约保护。
