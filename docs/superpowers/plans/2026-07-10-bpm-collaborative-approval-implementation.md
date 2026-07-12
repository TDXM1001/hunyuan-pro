# BPM 并行全员会签实施任务计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development`（推荐）或 `executing-plans` 按任务批次实施。每个批次使用复选框跟踪；设计已经确认，不把本计划当成逐文件二次审批流程。

**目标：** 在 Hunyuan BPM 中完整交付 `approvalMode = parallelAll` 的并行全员会签，包括受限 DSL、BPMN 编译、审批组运行时投影、组级动作语义、结构化详情契约、前端展示、并发保护和真实三人验收。

**设计来源：** `docs/superpowers/specs/2026-07-10-bpm-collaborative-approval-design.md`

**架构：** 保持现有 authored `userTask` 节点数组和 Hunyuan/Flowable 边界。`parallelAll` 只把一个 authored 节点编译为固定的“并行分叉网关 -> N 个单处理人任务 -> 并行汇聚网关”片段；运行期以 Hunyuan `审批组` 作为计数、锁定、终态和展示边界，成员仍是独立的 `t_bpm_task` 投影。所有页面通过结构化审批组 VO 展示会签，不解析 Flowable ID、任务 key 后缀或运行时 JSON 快照。

**技术栈：** Java 17、Spring Boot、Flowable 7.2.0、MyBatis-Plus、MySQL、JUnit 5、Mockito、AssertJ、Vue 3、TypeScript、Element Plus、Vitest、Maven、pnpm、持久 Playwright MCP。

## 全局约束

- 实现留在 `E:/my-project/hunyuan-pro`；Yudao 前端与 RuoYi 后端仅用于理解机制，不迁移其代码、名称、接口、依赖或模块边界。
- 本计划只实现 `parallelAll`。首期必须是至少两名不同、有效的指定员工，候选人类型固定为 `EMPLOYEE`。
- 不实现或签、比例审批、Flowable multi-instance、动态成员、角色/部门自动多人展开、通用网关 DSL、子流程、定时器、脚本或图形运行态高亮。
- 不新增 Maven、pnpm 或前端运行时依赖。
- Flowable 网关、执行 ID、任务 ID 只停留在 BPM 内部；公共 Hunyuan API 只返回审批组、实例、任务和员工语义。
- 已部署的单人和顺序多人定义、历史任务与实例必须保持兼容；普通任务的审批组字段为 `null`，实例审批组列表为空。
- 使用当前分支；不因本模块创建 worktree。保留当前工作树中用户已有的 `AGENTS.md` 和 BPM 基线改动，不回退、不混入实现提交。
- SQL 固定新增 `数据库SQL脚本/mysql/sql-update-log/v3.43.0.sql`；不回填历史数据，不重写已部署定义或 Flowable 表。
- 所有中文文档、测试名称、用户提示和 UI 文案使用 UTF-8。
- 运行时截图、网络日志、浏览器会话、profile 与临时输出只保留在 `G:/code-mcp/playwright-mcp-temp/runtime` 或 `G:/code-mcp/playwright-mcp-temp/cache`，不提交到仓库。
- 已确认设计后，按“**一次架构自审 -> 成组实现 -> 批次验证 -> 总体验收**”推进。只有需求变化、无法兼容的架构冲突或真实安全阻塞才重新讨论范围。

## 锁定的领域契约

### DSL 与编译快照

```json
{
  "nodeKey": "finance_review",
  "name": "财务会签",
  "type": "userTask",
  "approvalMode": "parallelAll",
  "candidateResolverType": "EMPLOYEE",
  "employeeIds": [101, 102, 103],
  "listeners": []
}
```

每个并行展开后的 compiled node 必须保留以下字段，作为投影和详情组装的事实来源：

```text
authoredNodeKey
authoredNodeName
approvalMode = parallelAll
approvalGroupKey
approvalGroupName
parallelIndex
parallelTotal
employeeId
nodeKey
name
candidateResolverType
listeners
startRuleJson
variableMappingJson
```

编译器生成的内部 ID 采用固定命名空间：

```text
gateway_<authoredNodeKey>_split
<authoredNodeKey>_<memberIndex>
gateway_<authoredNodeKey>_join
flow_<monotonicIndex>
```

校验器必须对 authored key、全部展开 task key、两个 gateway key 和全部 sequence flow key 做格式、长度、保留 ID 与全局唯一性校验；不能只检查 authored 节点。

### 审批组状态与动作

```text
groupState: PENDING | APPROVED | REJECTED | RETURNED | CANCELLED
closeReason: ALL_APPROVED | MEMBER_REJECTED | MEMBER_RETURNED |
             INSTANCE_RECALLED | INSTANCE_CANCELLED
lock order: approvalGroup -> task
```

| 动作 | 审批组结果 | Flowable/实例结果 |
| --- | --- | --- |
| 非最后成员通过 | 保持 `PENDING`，重算进度 | 完成当前成员 task；下游不得创建 |
| 最后成员通过 | `APPROVED / ALL_APPROVED` | 完成当前成员 task；由汇聚网关自然放行一次 |
| 任一成员拒绝 | `REJECTED / MEMBER_REJECTED` | 终止流程实例，取消同组其余待办 |
| 任一成员退回 | `RETURNED / MEMBER_RETURNED` | 终止活动流程实例，实例置为 `WAIT_RESUBMIT`，取消同组其余待办 |
| 发起人撤回 | `CANCELLED / INSTANCE_RECALLED` | 保持既有 `WAIT_RESUBMIT` 语义，关闭当前审批组待办 |
| 发起人/管理员取消 | `CANCELLED / INSTANCE_CANCELLED` | 保持既有实例取消语义，关闭当前审批组待办 |
| 转办、委派 | 不变 | 只改变当前成员的处理人 |
| 加签、减签 | 不适用 | 发现 `approval_group_id != null` 时前后端硬拒绝 |

终态请求必须幂等：双击、网络重试，或“通过与拒绝/退回”竞态只能产生一个审批组终态和一次有效的引擎流转。拒绝、退回、撤回、取消的组级停止统一通过 `FlowableProcessInstanceGateway.cancel(...)` 封装，不向公共层泄漏 Flowable 操作。

### 公共结构化 VO

```java
public class BpmApprovalGroupSummaryVO {
    private Long approvalGroupId;
    private String approvalGroupKey;
    private String approvalGroupName;
    private String approvalMode;
    private String groupState;
    private Integer totalMemberCount;
    private Integer processedMemberCount;
    private Integer approvedMemberCount;
    private Integer rejectedMemberCount;
}

public class BpmApprovalGroupDetailVO extends BpmApprovalGroupSummaryVO {
    private String closeReason;
    private LocalDateTime closedAt;
    private List<BpmApprovalGroupMemberVO> members;
}
```

接口承载方式固定如下，不新增仅为展示而存在的独立页面接口：

```text
BpmTaskVO.approvalGroup: BpmApprovalGroupSummaryVO | null
BpmTaskDetailVO.approvalGroup: BpmApprovalGroupDetailVO | null
BpmInstanceDetailVO.approvalGroups: BpmApprovalGroupDetailVO[]
BpmInstanceTraceVO.approvalGroups: BpmApprovalGroupDetailVO[]
```

## 文件职责映射

| 区域 | 文件 | 责任 |
| --- | --- | --- |
| 设计器类型 | `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/types.ts` | 把 `parallelAll` 纳入 `BpmProcessNodeDraft.approvalMode` 联合类型 |
| DSL 编解码与预览 | `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/simple-model-bridge.ts` | 保留/输出 `parallelAll` 与 `employeeIds`，预览固定并行网关片段 |
| 设计器交互 | `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-process-designer-adapter.vue` | 模式、远程员工多选、即时校验与 BPMN 预览 |
| 设计器测试 | `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts` | DSL round-trip、预览 BPMN、非法组员配置 |
| 模型校验 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidator.java` | 发布前校验模式、成员和所有生成 ID |
| 候选人预检 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/BpmCandidatePrecheckService.java` | 发布前逐个验证并行成员可用性 |
| BPMN 编译 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelBpmnCompiler.java` | 将 authored 节点编译成受限片段及完整快照 |
| 运行时变量 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskAssignmentResolver.java` | 为每个展开成员写入 `assignee_<compiledNodeKey>` |
| 数据库 | `数据库SQL脚本/mysql/sql-update-log/v3.43.0.sql` | 审批组表、唯一约束/索引、`t_bpm_task.approval_group_id` |
| 审批组领域 | 新建 `.../runtime/domain/entity/BpmApprovalGroupEntity.java`、`.../runtime/dao/BpmApprovalGroupDao.java`、`.../resources/mapper/bpm/runtime/BpmApprovalGroupMapper.xml` | 审批组持久化、按实例/组键查询、悲观锁查询、详情查询 |
| 审批组服务 | 新建 `.../runtime/service/BpmApprovalGroupService.java` | 稳定归组、计数重算、锁顺序、成员终态、组关闭和结构化详情组装 |
| 任务投影 | `.../runtime/service/BpmTaskProjectionService.java`、`.../runtime/domain/entity/BpmTaskEntity.java` | 创建成员 task 时写入 `approval_group_id`，并保持普通投影不变 |
| 任务/实例动作 | `.../runtime/service/BpmTaskService.java`、`.../runtime/service/BpmInstanceService.java` | 将通过、拒绝、退回、撤回、取消、转办、委派、加减签边界接入审批组 |
| 列表与详情查询 | `.../runtime/dao/BpmTaskDao.java`、`.../resources/mapper/bpm/runtime/BpmTaskMapper.xml` | 任务列表/当前任务连接审批组摘要，成员详情查询保持批量化 |
| 后端 VO | 新建 `.../runtime/domain/vo/BpmApprovalGroupSummaryVO.java`、`BpmApprovalGroupDetailVO.java`、`BpmApprovalGroupMemberVO.java`；修改任务/实例/trace VO | 对外输出 Hunyuan 审批组结构 |
| 前端 API | `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts` | 对齐审批组 TypeScript 类型及委派、加签、减签、撤回 API |
| 运行时页面 | `.../runtime/my-todo-list.vue`、`.../runtime/my-done-list.vue`、`.../runtime/components/bpm-instance-detail-drawer.vue`、`.../task/task-list.vue` | 展示摘要/成员/结束原因；会签成员隐藏加减签，保留可用的成员级转办和委派 |
| 前端契约测试 | `.../views/system/bpm/bpm-modules.test.ts`、`.../runtime/components/bpm-runtime-form-rules.test.ts` | 保护 API 类型、操作边界、列表与详情不解析内部 JSON |
| 关闭记录 | 新建 `docs/superpowers/specs/2026-07-10-bpm-collaborative-approval-acceptance.md`；修改 `docs/superpowers/specs/2026-07-10-bpm-development-baseline.md` | 记录真实验证，更新已完成能力与后续边界 |

---

## Task 0：一次架构自审与实施起点

**目标：** 在第一处生产代码修改前完成一次内部自审，确认设计与当前代码的连接点；通过后连续实施后续批次，不重复发起小点审批。

**输入：**

- `docs/superpowers/specs/2026-07-10-bpm-collaborative-approval-design.md`
- `docs/superpowers/specs/2026-07-10-bpm-development-baseline.md`
- 本计划的“锁定的领域契约”和“文件职责映射”

- [ ] **Step 1：核对编译器片段边界**

确认 `SimpleModelBpmnCompiler` 只从 authored `nodes[]` 顺序生成片段；`parallelAll` 的分叉/汇聚只在该节点内部发生，节点之后仍连接下一 authored 片段。不得引入通用图遍历、条件分支或自由网关配置。

- [ ] **Step 2：核对审批组并发边界**

确认新服务以 `(engine_process_instance_id, approval_group_key)` 唯一定位审批组，所有改变成员状态的路径均遵守：

```text
lock approval group -> lock member task -> check PENDING -> drive Flowable -> persist member/group result
```

拒绝、退回、撤回、取消不能绕过审批组服务直接批量更新成员任务。

- [ ] **Step 3：核对动作与实例边界**

确认最后通过只完成当前 Flowable task 并依赖并行汇聚自然推进；拒绝/退回使用 `FlowableProcessInstanceGateway.cancel(...)` 停止活动实例，再按既有 Hunyuan 实例状态写入 `FINISHED/REJECTED` 或 `WAIT_RESUBMIT`。不能先完成并行成员 task 再试图回收剩余分支。

- [ ] **Step 4：核对展示数据源**

确认 `runtimeAssignmentSnapshotJson` 和 `currentNodeSummaryJson` 继续作为诊断字段，不承担会签展示职责；任务/实例/trace 页面仅使用新增审批组 VO。

- [ ] **Step 5：记录结论，不请求逐项确认**

在最终验收记录的“架构自审”小节写入通过日期、实际采用的锁查询方法和任何必要的最小偏差。若发现需求变化、无法维持历史兼容或无法保证锁顺序的真实阻塞，停止在该点并提出一条明确的模块级决策；其余自然联动直接继续实施。

**完成条件：**

- 当前实现入口、数据事实源、锁顺序和实例终止方式都能指向本计划中的具体文件。
- 不产生新的设计文档或小任务审批循环。

---

## Task 1：受限 DSL、预检、SQL 与并行 BPMN 编译

**目标：** 让设计器、发布校验、候选人预检和编译器以同一份 `parallelAll` 语义工作，并为运行时投影准备稳定的数据库结构和编译快照。

**文件：**

- 修改：`hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/types.ts`
- 修改：`hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/simple-model-bridge.ts`
- 修改：`hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-process-designer-adapter.vue`
- 修改：`hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts`
- 修改：`hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidator.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/BpmCandidatePrecheckService.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelBpmnCompiler.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskAssignmentResolver.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidatorTest.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/BpmCandidatePrecheckServiceTest.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelBpmnCompilerTest.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskAssignmentResolverTest.java`
- 新建：`数据库SQL脚本/mysql/sql-update-log/v3.43.0.sql`

**产出接口：**

```ts
export interface BpmProcessNodeDraft {
  approvalMode?: 'parallelAll' | 'sequential' | 'single' | 'singleOnly';
  employeeIds?: number[];
}
```

```java
// 只接受 EMPLOYEE + 至少两个不同的正整数；发布预检还要逐个 requireEmployee。
boolean isParallelAllEmployeeApproval(JSONObject nodeObject, String resolverType);

// 所有 compiled task 都拥有独立变量。
Map<String, Object> resolve(List<BpmDefinitionNodeEntity> definitionNodes,
                            BpmTaskAssignmentContext context);
// parallelAll 节点的结果包含 assignee_<authoredNodeKey>_<index>。
```

- [ ] **Step 1：先补前后端失败测试**

在设计器测试中覆盖以下草稿：

```json
{"nodes":[{"nodeKey":"finance_review","name":"财务会签","type":"userTask","approvalMode":"parallelAll","candidateResolverType":"EMPLOYEE","employeeIds":[101,102],"listeners":[]}]}
```

断言 round-trip 不丢失 `parallelAll` 与 `employeeIds`，只读 BPMN 预览包含：

```text
gateway_finance_review_split
finance_review_1
finance_review_2
gateway_finance_review_join
```

在 `SimpleModelValidatorTest`、`BpmCandidatePrecheckServiceTest` 与 `BpmTaskAssignmentResolverTest` 写入失败用例：

```text
parallelAll + 非 EMPLOYEE                       -> 拒绝
employeeIds 少于 2 / 含 0 / 含小数 / 重复         -> 拒绝
任一员工不存在或禁用                              -> 发布预检 BLOCKING
生成 task/gateway/flow ID 与 authored key 冲突    -> 拒绝
两个成员                                         -> 生成两个 assignee_<compiledNodeKey> 变量
```

- [ ] **Step 2：扩展设计器但不扩展节点类型**

将 `approvalMode` 联合类型加入 `parallelAll`。在 `bpm-process-designer-adapter.vue` 中：

```text
选择“并行全员会签”时：
1. candidateResolverType 强制为 EMPLOYEE；
2. 使用现有 queryEmployeePage 的远程多选；
3. 至少选择两名不同且启用的员工；
4. 切换离开 parallelAll 时清理只属于多成员模式的 employeeIds；
5. 选择 sequential 或 parallelAll 时都禁止改为其他候选人类型。
```

`simple-model-bridge.ts` 对 `sequential` 与 `parallelAll` 都序列化 `employeeIds`；预览生成与后端一致的固定 split/member/join 片段，避免设计器看到线性图而部署后出现并行图。

- [ ] **Step 3：让发布校验与预检成为后端权威**

在 `SimpleModelValidator` 中统一复用“多人指定员工”解析规则，区分错误文案：

```text
顺序审批仅支持指定员工
并行全员会签仅支持指定员工
并行全员会签至少配置 2 名员工
并行全员会签存在重复员工
并行全员会员工 ID 无效
```

校验生成 ID 时，除成员 task key 外加入：

```java
String splitGatewayKey = "gateway_" + authoredNodeKey + "_split";
String joinGatewayKey = "gateway_" + authoredNodeKey + "_join";
```

二者必须通过与 task key 相同的格式/长度/保留 ID/全局唯一性检查；`flow_<index>` 继续用单调序号生成，且不得与 authored key 或其他编译器 ID 冲突。

在 `BpmCandidatePrecheckService` 中新增 `parallelAll` 分支：标准化每个 `employeeIds` 元素，逐一调用 `BpmOrgIdentityGateway.requireEmployee`；任一成员不可用时返回 `BLOCKING`，可用时在 `requiredConfig` 和说明中显示“指定 N 名并行会签员工”。

- [ ] **Step 4：把编译器改为受限片段列表**

将当前 `List<UserTaskNode>` 的线性构造演进为顺序片段列表，例如：

```java
private sealed interface BpmnFragment permits UserTaskFragment, ParallelAllFragment {}

private record UserTaskFragment(String taskKey, String taskName) implements BpmnFragment {}

private record ParallelAllFragment(
        String splitGatewayKey,
        List<UserTaskNode> memberTasks,
        String joinGatewayKey
) implements BpmnFragment {}
```

实现要求：

1. 单人节点输出一个 `UserTaskFragment`。
2. `sequential` 输出多个顺序 `UserTaskFragment`。
3. `parallelAll` 输出一个 `ParallelAllFragment`，成员 task 使用 `<authoredKey>_<index>`。
4. 生成 BPMN 时，前一片段连接 split；split 连接每个成员 task；每个成员 task 连接 join；join 再连接下一片段或 `endEvent`。
5. 每个成员 task 使用现有 `flowable:assignee="${assignee_<compiledNodeKey>}"`。
6. 每个 compiled snapshot 填入 `approvalGroupKey`、`approvalGroupName`、`parallelIndex`、`parallelTotal` 和冻结的 `employeeId`。

编译器测试必须断言两个或三个成员的 BPMN 同时包含一个 split、N 条成员分支和一个 join，并断言普通单人、顺序多人输出未回归。

- [ ] **Step 5：创建 v3.43.0 SQL**

按当前 SQL 脚本风格创建以下结构，字段名和索引名保持稳定：

```sql
create table t_bpm_approval_group (
    approval_group_id bigint not null auto_increment,
    instance_id bigint not null,
    definition_id bigint not null,
    engine_process_instance_id varchar(128) not null,
    approval_group_key varchar(128) not null,
    approval_group_name varchar(255) not null,
    approval_mode varchar(32) not null,
    group_state varchar(32) not null,
    close_reason varchar(64) null,
    total_member_count int not null,
    processed_member_count int not null default 0,
    approved_member_count int not null default 0,
    rejected_member_count int not null default 0,
    closed_at datetime null,
    create_time datetime not null default current_timestamp,
    update_time datetime not null default current_timestamp on update current_timestamp,
    primary key (approval_group_id),
    unique key uk_bpm_approval_group_engine_key (engine_process_instance_id, approval_group_key),
    key idx_bpm_approval_group_instance_state (instance_id, group_state)
) engine=innodb default charset=utf8mb4 comment='BPM 并行审批组';

alter table t_bpm_task
    add column approval_group_id bigint null comment '并行审批组ID' after definition_node_id,
    add key idx_bpm_task_approval_group_state (approval_group_id, task_state);
```

根据相邻 SQL 文件的审计字段和表注释惯例补齐必要列定义；不得增加历史回填或数据修复语句。

- [ ] **Step 6：运行批次门禁**

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=SimpleModelValidatorTest,SimpleModelBpmnCompilerTest,BpmCandidatePrecheckServiceTest,BpmTaskAssignmentResolverTest' test
```

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

**预期：** 所有聚焦测试通过；不增加依赖；SQL 只新增 `v3.43.0.sql`。

- [ ] **Step 7：提交本批次**

```text
feat(bpm): 支持并行全员会签模型与编译
```

提交包含 DSL、编译、校验、SQL 和对应测试；不包含浏览器产物、无关基础模块或验收截图。

**完成条件：**

- 非法会签模型无法保存/发布，且后端是最终权威。
- 编译产物为固定 split/member/join 拓扑，快照足以稳定归组。
- 普通和顺序审批相关测试继续通过。

---

## Task 2：审批组投影、组级收敛与并发幂等

**目标：** 将同一次 Flowable 并行分叉的成员任务稳定归入同一个 Hunyuan 审批组，并以审批组作为所有成员终态与取消动作的并发边界。

**文件：**

- 新建：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmApprovalGroupStateEnum.java`
- 新建：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmApprovalGroupCloseReasonEnum.java`
- 新建：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/entity/BpmApprovalGroupEntity.java`
- 新建：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/dao/BpmApprovalGroupDao.java`
- 新建：`hunyuan-backend/hunyuan-bpm/src/main/resources/mapper/bpm/runtime/BpmApprovalGroupMapper.xml`
- 新建：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmApprovalGroupService.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/entity/BpmTaskEntity.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/dao/BpmTaskDao.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/resources/mapper/bpm/runtime/BpmTaskMapper.xml`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskProjectionService.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskProjectionServiceTest.java`
- 新建：`hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmApprovalGroupServiceTest.java`
- 新建：`hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskServiceTest.java`

**产出接口：**

```java
public interface BpmApprovalGroupService {
    Long assignApprovalGroup(
            BpmInstanceEntity instance,
            BpmDefinitionNodeEntity definitionNode,
            BpmTaskEntity memberTask
    );

    BpmApprovalGroupActionResult handleMemberAction(
            Long taskId,
            BpmApprovalMemberAction action,
            BpmEmployeeSnapshot actor,
            String commentText
    );

    void closePendingGroupsForInstance(
            Long instanceId,
            BpmApprovalGroupCloseReasonEnum closeReason,
            BpmTaskResultEnum memberCancelledResult,
            LocalDateTime actionAt
    );
}
```

`handleMemberAction` 的返回值至少表达：

```text
ordinaryTask / memberTask
shouldCompleteCurrentFlowableTask
shouldCancelEngineProcess
finishInstanceResultState
waitResubmit
groupId
groupState
```

`BpmTaskService` 保留既有公共方法签名；它依据 `task.approvalGroupId` 决定委托给审批组服务或沿用原有单任务路径。

- [ ] **Step 1：先写审批组服务和投影失败测试**

覆盖下列可独立拒绝的事实：

```text
同一 process instance + approvalGroupKey 的两个成员只创建一个审批组
不同 process instance 即使 approvalGroupKey 相同也创建不同审批组
并行成员投影写入同一个 approval_group_id
普通/顺序/加签任务的 approval_group_id 仍为 null
非最后成员通过不会关闭组，也不会结束实例
最后成员通过只将组置为 APPROVED 一次
成员拒绝关闭组、取消其余 PENDING 成员并终止实例
成员退回关闭组、取消其余 PENDING 成员并使实例进入 WAIT_RESUBMIT
双击通过和通过/拒绝竞态的第二个请求不会再次驱动 Flowable
撤回、发起人取消、管理员取消均关闭正在 PENDING 的审批组
转办、委派只改变当前成员处理人
会签成员加签、减签均返回明确错误
```

测试使用 Mockito 验证网关调用次数：最后通过恰好一次 `complete`；拒绝/退回恰好一次 `FlowableProcessInstanceGateway.cancel`；被取消成员不再调用 `complete`。

- [ ] **Step 2：实现审批组实体、DAO 与锁查询**

`BpmApprovalGroupEntity` 对应 `t_bpm_approval_group` 全部领域字段。`BpmApprovalGroupDao` 至少提供：

```java
BpmApprovalGroupEntity selectByEngineProcessInstanceIdAndGroupKey(
        String engineProcessInstanceId, String approvalGroupKey);

BpmApprovalGroupEntity selectByIdForUpdate(Long approvalGroupId);

List<BpmApprovalGroupEntity> selectPendingByInstanceIdForUpdate(Long instanceId);
```

`BpmApprovalGroupMapper.xml` 使用显式 `for update` 锁定审批组行。新增任务 DAO 方法以同一事务在审批组锁之后锁定成员 task，例如：

```java
BpmTaskEntity selectByIdForUpdate(Long taskId);
List<BpmTaskEntity> selectPendingByApprovalGroupIdForUpdate(Long approvalGroupId);
```

任何需要同时访问二者的代码必须先调用 `selectByIdForUpdate(approvalGroupId)`，再调用任务锁查询；不得先按 task 锁行后再查询审批组。

- [ ] **Step 3：在投影入口稳定归组**

在 `BpmTaskProjectionService.insertTaskIfMissing(...)` 中先按当前定义节点的 `compiledNodeSnapshotJson` 判断是否满足：

```text
approvalMode == parallelAll
approvalGroupKey 非空
parallelIndex / parallelTotal 合法
```

满足时调用 `bpmApprovalGroupService.assignApprovalGroup(...)`，将返回的 ID 写入 `BpmTaskEntity.approvalGroupId` 后插入任务。实现必须处理两个 Flowable task 创建事件并发到达：

1. 先查询唯一组；
2. 不存在时尝试插入；
3. 唯一键冲突后重新查询；
4. 绝不创建第二个组；
5. `totalMemberCount` 只取编译快照的 `parallelTotal`，不以本次活动任务数量猜测。

通知创建仍以成员任务为粒度；需要增加可读会签上下文时由审批组快照生成，不在通知中写 Flowable 内部 ID。

- [ ] **Step 4：将成员动作收敛到审批组服务**

重构 `BpmTaskService.approve`、`reject`、`returnToInitiator` 的内部实现：

```java
if (taskEntity.getApprovalGroupId() != null) {
    return bpmApprovalGroupService.handleMemberAction(
            taskEntity.getTaskId(), action, actorSnapshot, commentText);
}
return existingSingleTaskPath(...);
```

审批组处理顺序固定：

```text
1. lock approval group
2. lock current task
3. 检查 group/task 都是 PENDING，并检查当前员工权限
4. 计算成员动作对组的结果
5. 调用 Flowable complete 或 process cancel
6. 写入当前任务、其余待办成员、审批组计数/终态、动作日志、实例状态
7. 同步活动任务投影或清空实例活动摘要
```

通过时根据锁内的实际成员状态重算计数，不以“请求次数”加一。最后成员通过后仅完成当前 Flowable task，由网关汇聚创建下游任务；随后调用 `syncActiveTasksForInstance`，只有无活动任务时才按既有语义结束实例。

拒绝与退回时先在组锁内确定唯一终态，再取消 Flowable 实例并关闭同组的 `PENDING` 投影任务。拒绝写入既有 `REJECTED` 结果；退回写入既有 `RETURNED` 结果和 `WAIT_RESUBMIT`，两者都不允许留下可继续审批的活动引擎任务。

- [ ] **Step 5：将实例级撤回与取消接入审批组关闭**

在 `BpmTaskService.recall(...)`、`BpmInstanceService.cancelMyInstance(...)` 和 `BpmInstanceService.adminCancel(...)` 中，在关闭普通待办前调用：

```java
bpmApprovalGroupService.closePendingGroupsForInstance(
        instanceId,
        closeReason,
        BpmTaskResultEnum.RECALLED /* 或 INSTANCE_CANCELLED */,
        now
);
```

该方法在实例内按审批组逐个取得组锁再锁成员任务，更新组状态/结束原因/结束时间和成员取消状态。普通不属于审批组的待办继续走现有关闭路径，避免改变历史行为。

- [ ] **Step 6：保留可用成员级动作，封住加减签**

`transfer`、`adminTransfer`、`delegate`、`adminDelegate` 对成员 task 只调用现有处理人更新路径，不增减组成员、不更新进度。`addSign`、`reduceSign` 在读取 task 后优先检查：

```java
if (taskEntity.getApprovalGroupId() != null) {
    return ResponseDTO.userErrorParam("并行全员会签成员不支持加签或减签");
}
```

不要通过 `runtimeAssignmentSnapshotJson` 识别会签成员。

- [ ] **Step 7：运行批次门禁**

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskProjectionServiceTest,BpmApprovalGroupServiceTest,BpmTaskServiceTest' test
```

**预期：** 审批组创建、成员并发、全员通过、拒绝、退回、撤回、取消和高级动作边界均通过聚焦测试；测试能断言 Flowable 调用未重复。

- [ ] **Step 8：提交本批次**

```text
feat(bpm): 增加并行审批组运行时投影
```

**完成条件：**

- 同一并行节点的成员稳定关联一个审批组。
- 组计数来自持久化成员任务状态，所有状态改变遵守 `approvalGroup -> task`。
- 终止、退回、撤回、取消不留下同组可处理待办或活动引擎任务。

---

## Task 3：结构化审批组契约、查询与详情投影

**目标：** 将审批组摘要和详情作为 Hunyuan API 的一等字段返回给任务列表、任务详情、实例详情和 trace，并保证普通任务兼容。

**文件：**

- 新建：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmApprovalGroupSummaryVO.java`
- 新建：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmApprovalGroupDetailVO.java`
- 新建：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmApprovalGroupMemberVO.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmTaskVO.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmTaskDetailVO.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmInstanceDetailVO.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmInstanceTraceVO.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/dao/BpmApprovalGroupDao.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/resources/mapper/bpm/runtime/BpmApprovalGroupMapper.xml`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmApprovalGroupService.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceTraceService.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskDetailServiceTest.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeDetailServiceTest.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmInstanceTraceServiceTest.java`

**产出接口：**

```java
public class BpmApprovalGroupMemberVO {
    private Long taskId;
    private Integer memberIndex;
    private Integer memberTotal;
    private Long assigneeEmployeeId;
    private String assigneeNameSnapshot;
    private String assigneeDepartmentNameSnapshot;
    private String taskName;
    private Integer taskState;
    private Integer taskResult;
    private LocalDateTime assignedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private BpmTaskActionLogVO lastAction;
}
```

- [ ] **Step 1：补详情契约失败测试**

添加以下断言：

```text
会签任务列表得到非空 approvalGroup 摘要，包含“已处理/总人数”的整数数据
会签任务详情得到成员列表、终态/结束原因和最后动作
实例详情与 trace 得到同一结构化审批组列表
普通任务 approvalGroup 为 null，实例 approvalGroups 为空列表
任何 VO 不需要前端解析 taskKey、taskName、runtimeAssignmentSnapshotJson 或 currentNodeSummaryJson
```

- [ ] **Step 2：实现摘要与详情批量组装**

在 `BpmApprovalGroupService` 提供面向查询的明确方法：

```java
Map<Long, BpmApprovalGroupSummaryVO> mapSummariesById(Collection<Long> approvalGroupIds);

BpmApprovalGroupDetailVO getDetailById(Long approvalGroupId);

List<BpmApprovalGroupDetailVO> listDetailsByInstanceId(Long instanceId);
```

详情组装规则：

1. 审批组基本字段来自 `t_bpm_approval_group`。
2. 成员来自 `t_bpm_task.approval_group_id`，按 compiled snapshot 的 `parallelIndex` 排序；若快照异常，则按 `assigned_at, task_id` 稳定排序并在服务日志中记录诊断，不向页面猜测序号。
3. 成员动作从既有 `BpmTaskActionLogDao` 取该成员最后一条可展示记录。
4. 对同一个列表/实例详情的多个组使用批量查询，避免按 task 逐条加载。

- [ ] **Step 3：扩展任务、实例与 trace 服务**

`BpmTaskService.queryAdminPage(...)`、`queryMyTodoPage(...)`、`queryMyDonePage(...)` 在返回列表前批量填充：

```java
taskVO.setApprovalGroup(groupSummaryMap.get(taskEntityApprovalGroupId));
```

`BpmTaskService.getDetail(...)` 在任务有 `approvalGroupId` 时填充详情。`BpmInstanceService.getDetail(...)` 设置：

```java
detail.setApprovalGroups(bpmApprovalGroupService.listDetailsByInstanceId(instanceId));
```

`BpmInstanceTraceService.getTrace(...)` 同时把同一审批组详情暴露到 trace 顶层，保证员工/管理端详情与可靠性追踪共享同一数据语义。

- [ ] **Step 4：调整 Mapper 与查询索引使用**

任务分页和当前待办查询通过左连接或批量查询填充摘要；不改变既有分页、权限过滤和排序。会签成员详情查询必须使用：

```text
idx_bpm_task_approval_group_state
idx_bpm_approval_group_instance_state
```

普通任务不得因为新增 left join 变为内连接，历史记录仍可正常返回。

- [ ] **Step 5：运行批次门禁**

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskDetailServiceTest,BpmRuntimeDetailServiceTest,BpmInstanceTraceServiceTest,BpmApprovalGroupServiceTest' test
```

**预期：** 结构化审批组字段在任务、实例与 trace 中一致，非会签历史数据保持空字段兼容。

- [ ] **Step 6：提交本批次**

```text
feat(bpm): 输出审批组结构化运行时详情
```

**完成条件：**

- 不需要新增展示专用接口。
- 后端能够完整描述当前组进度、成员、结果与结束原因。
- 任务/实例/trace 所见审批组语义一致。

---

## Task 4：前端展示、高级动作边界、轨迹与通知文案

**目标：** 让设计器和运行时页面可理解、可操作地展示审批组，同时把会签成员的高级动作限制落实到前端与后端。

**文件：**

- 修改：`hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`
- 修改：`hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/my-todo-list.vue`
- 修改：`hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/my-done-list.vue`
- 修改：`hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue`
- 修改：`hunyuan-design/apps/hunyuan-system/src/views/system/bpm/task/task-list.vue`
- 修改：`hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`
- 修改：`hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskProjectionService.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskServiceTest.java`

**产出接口：**

```ts
export interface BpmApprovalGroupSummaryRecord {
  approvalGroupId: number;
  approvalGroupKey: string;
  approvalGroupName: string;
  approvalMode: 'parallelAll';
  groupState: 'PENDING' | 'APPROVED' | 'REJECTED' | 'RETURNED' | 'CANCELLED';
  totalMemberCount: number;
  processedMemberCount: number;
  approvedMemberCount: number;
  rejectedMemberCount: number;
}

export interface BpmApprovalGroupDetailRecord extends BpmApprovalGroupSummaryRecord {
  closeReason?: null | string;
  closedAt?: null | string;
  members: BpmApprovalGroupMemberRecord[];
}
```

`BpmTaskRecord` 与 `BpmTaskDetailRecord` 分别增加 `approvalGroup?: BpmApprovalGroupSummaryRecord | null` 和 `approvalGroup?: BpmApprovalGroupDetailRecord | null`；`BpmInstanceDetailRecord`、`BpmInstanceTraceRecord` 增加 `approvalGroups`。

- [ ] **Step 1：先写前端契约与动作边界测试**

在 `bpm-modules.test.ts` 和运行时规则测试中断言：

```text
运行时 API 定义审批组 summary/detail/member 类型
待办、已办、管理任务和实例详情读取 approvalGroup/approvalGroups
页面不通过 taskKey 后缀、任务名、runtimeAssignmentSnapshotJson 或 currentNodeSummaryJson 推导会签
会签成员的加签、减签入口不可见或禁用
会签成员仍可走转办、委派、撤回的既有权限/可用性判断
```

- [ ] **Step 2：扩展运行时 TypeScript API**

在 `runtime.ts` 添加审批组类型并对齐现有后端字段。将已存在的后端动作补齐为前端表单和函数：

```ts
export interface BpmTaskDelegateForm {
  reason?: null | string;
  targetEmployeeId: number;
  taskId: number;
}

export interface BpmTaskAddSignForm {
  reason?: null | string;
  targetEmployeeId: number;
  taskId: number;
}

export interface BpmTaskReduceSignForm {
  reason?: null | string;
  taskId: number;
}

export interface BpmTaskRecallForm {
  reason?: null | string;
  taskId: number;
}
```

函数路径严格复用现有 `AppBpmTaskController`：

```text
/app/bpm/task/delegate
/app/bpm/task/addSign
/app/bpm/task/reduceSign
/app/bpm/task/recall
```

不得为审批组新增前端私有接口。

- [ ] **Step 3：实现列表摘要与详情成员展示**

在待办、已办和管理任务列表的任务名称附近显示：

```text
<审批组名称>，<processedMemberCount>/<totalMemberCount> 已处理
```

普通任务不显示空占位。任务详情、实例抽屉和管理 trace 中增加“审批组”区块：

```text
组名、状态、进度、结束原因、结束时间
成员姓名、部门、状态、结果、意见、处理时间/取消时间
```

当前进行中的组优先显示进度；已关闭组显示终态和结束原因。成员被拒绝、退回、撤回或取消后，其余成员必须显示“已取消”而非继续显示为可处理状态。

- [ ] **Step 4：把高级动作收敛到一个紧凑操作面板**

改造 `my-todo-list.vue` 的操作区：

1. 用现有远程员工选择替换“输入员工 ID”的转办弹窗。
2. 增加委派、加签、减签、撤回操作及确认/错误反馈，复用现有 Element Plus 对话框和员工远程选择。
3. `row.approvalGroup` 非空时，隐藏加签、减签；转办、委派仍仅作用于该成员任务。
4. 发起人撤回按后端返回结果刷新待办、实例详情和组进度。
5. 所有入口都以服务端返回为权威；前端隐藏不是安全边界。

按钮组保持现有列表页面密度，不增加重复页面标题、说明卡片或永久分栏。

- [ ] **Step 5：补充轨迹与通知可读文案**

后端动作日志仍保留稳定的 `actionType`，前端映射至少覆盖：

```text
PARALLEL_MEMBER_APPROVED        -> 会签成员通过
PARALLEL_MEMBER_REJECTED        -> 会签成员拒绝，审批组已终止
PARALLEL_MEMBER_RETURNED        -> 会签成员退回发起人，其他成员待办已取消
APPROVAL_GROUP_ALL_APPROVED     -> 审批组全员通过
APPROVAL_GROUP_CANCELLED        -> 审批组已关闭
```

成员任务创建通知标题/正文加入审批组名称和成员进度语义，但不泄漏引擎 ID。普通任务通知文本保持兼容。

- [ ] **Step 6：运行批次门禁**

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskServiceTest,BpmTaskProjectionServiceTest' test
```

**预期：** 前端类型、契约与操作边界通过；后端仍拒绝会签成员加签/减签。

- [ ] **Step 7：提交本批次**

```text
feat(bpm): 完成审批组运行时展示与动作边界
```

**完成条件：**

- 页面只使用结构化审批组数据解释会签。
- 任务成员的可用操作与后端语义一致。
- 普通任务的页面、动作和诊断 JSON 保持原行为。

---

## Task 5：兼容门禁、并发验证、三人活体验收与文档关闭

**目标：** 以测试、API/浏览器真实流程和可追溯文档证明该模块闭环，并更新 BPM 发展基线。

**文件：**

- 新建：`docs/superpowers/specs/2026-07-10-bpm-collaborative-approval-acceptance.md`
- 修改：`docs/superpowers/specs/2026-07-10-bpm-development-baseline.md`
- 仅运行时证据：`G:/code-mcp/playwright-mcp-temp/runtime/`

- [ ] **Step 1：运行后端模块与兼容门禁**

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=SimpleModelValidatorTest,SimpleModelBpmnCompilerTest,BpmTaskAssignmentResolverTest,BpmApprovalGroupServiceTest,BpmTaskServiceTest,BpmTaskProjectionServiceTest,BpmTaskDetailServiceTest,BpmRuntimeDetailServiceTest,BpmInstanceTraceServiceTest' test
```

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin '-Dtest=BpmFlowableCompatibilityTest' test
```

**预期：** 受限并行 BPMN 能被当前 Flowable 兼容层部署/运行；单人、顺序多人和历史详情契约不回归。

- [ ] **Step 2：运行前端契约与类型门禁**

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

**预期：** API 类型与页面契约一致，未引入额外依赖。

- [ ] **Step 3：执行并发与幂等专门验证**

使用可控单元/集成测试或同一测试事务内的并发调用覆盖：

| 场景 | 必须断言 |
| --- | --- |
| 两名成员同时通过 | 组计数正确；只有最后成员导致下游任务出现；没有第二个下游 task |
| 一个通过、一个拒绝并发 | 先获得组锁的终态胜出；另一请求只得到已处理/组已关闭反馈；无重复 Flowable 调用 |
| 一个通过、一个退回并发 | 无遗留活动 task；实例仅一次进入 `WAIT_RESUBMIT` |
| 同一成员双击通过 | 当前 task 只完成一次；动作日志、成员计数和组终态不重复 |
| 处理时撤回/取消 | 所有组成员最终不可处理；没有死锁或锁顺序倒置 |

测试必须使用真实 DAO 锁查询或能够证明锁调用顺序的集成测试；只靠页面禁用按钮不构成并发证据。

- [ ] **Step 4：准备真实三人会签定义和服务**

确认本地服务可用：

```text
frontend: http://127.0.0.1:5788
backend:  http://127.0.0.1:1024
browser controller: http://localhost:8934
MCP endpoint: http://localhost:8933/mcp
```

若持久控制器未运行，按 `AGENTS.md` 启动：

```powershell
G:\code-mcp\playwright-mcp-temp\local-scripts\start-http.ps1
node G:\code-mcp\playwright-mcp-temp\runtime\persistent-mcp-controller.cjs
```

复用一个可见浏览器会话；不写调用 `client.close()` 的一次性 MCP 脚本。准备三名不同、有效且可登录的员工，以及一个只包含三人 `parallelAll` 节点的测试定义。

- [ ] **Step 5：完成活体验收矩阵**

| 编号 | 流程 | 验收事实 |
| --- | --- | --- |
| L1 | 设计与发布 | 设计器只能用指定员工配置至少两人；发布后 BPMN/API 显示 split、三成员、join 的语义 |
| L2 | 发起与待办 | 一次发起产生三条独立待办、一个审批组；三名员工都能看到自己的任务和相同组进度 |
| L3 | 全员通过 | 前两人通过时下游未创建；第三人通过后下游只创建一次或实例正常结束 |
| L4 | 任一拒绝 | 一人拒绝后，其余成员待办被取消、实例结束、成员详情保留取消状态与组结束原因 |
| L5 | 退回发起人 | 一人退回后，其余待办取消、实例为 `WAIT_RESUBMIT`、没有遗留活动引擎任务；重新提交产生新的审批组 |
| L6 | 撤回与取消 | 会签进行中撤回、发起人取消、管理员取消均关闭审批组及成员待办，状态与既有实例语义一致 |
| L7 | 高级动作 | 转办/委派只改当前成员；会签成员前端看不到加减签，直接调用 API 也被拒绝 |
| L8 | 展示与兼容 | 待办、已办、任务详情、实例抽屉、trace 显示结构化组；普通单人/顺序流程无审批组时仍正常 |

记录每项的定义 ID、实例 ID、审批组 ID、成员任务 ID、账号角色、接口/页面路径、结论与运行时证据文件名。不要把账号密码、token、浏览器 profile 或运行时文件写入仓库。

- [ ] **Step 6：完成验收记录和 BPM 基线更新**

在 `2026-07-10-bpm-collaborative-approval-acceptance.md` 中记录：

```text
1. 实际交付范围与明确未交付范围
2. 一次架构自审结论及实际锁查询方式
3. SQL 文件与兼容策略
4. 每个后端/前端命令的实际结果
5. 并发/幂等验证证据
6. L1-L8 活体验收事实和证据名
7. 未新增依赖的明确声明
8. 任何遗留风险及其不影响本模块完成的理由
```

同步更新 BPM 基线：

```text
已完成能力：新增“并行全员会签（parallelAll）”
平台边界：继续明确不支持或签、比例审批、multi-instance、动态成员与通用网关
当前优先级：依据真实验收后重排，不把 parallelAll 误写成通用并行能力
验证基线：补充本模块实际执行的命令与活体验收结论
```

- [ ] **Step 7：收尾检查与提交**

```powershell
git diff --check
rg -n 'TODO|TBD|待定|待补充|implement later|fill in details' docs/superpowers/specs/2026-07-10-bpm-collaborative-approval-acceptance.md docs/superpowers/specs/2026-07-10-bpm-development-baseline.md
git status --short
```

验收记录不得包含未填写模板文字。确认没有把 `G:/code-mcp/playwright-mcp-temp/runtime`、截图、profile、日志或无关用户改动加入暂存区。

```text
docs(bpm): 记录并行全员会签验收
```

**完成条件：**

- 后端、Flowable 兼容、前端契约和类型检查都已按实际结果记录。
- 三人会签的通过、拒绝、退回、撤回/取消和高级动作边界均有活体或等价 API 证据。
- 验收记录与 BPM 基线已更新，且仍明确首期能力边界。

---

## 执行顺序与提交边界

```text
Task 0  一次架构自审（不形成二次审批）
Task 1  DSL、预检、SQL、受限并行 BPMN 编译
Task 2  审批组投影、收敛、终止与并发保护
Task 3  结构化 VO、Mapper、任务/实例/trace 契约
Task 4  前端展示、高级动作边界、轨迹与通知
Task 5  兼容、并发、三人活体、验收记录和基线关闭
```

建议提交保持以下意图边界：

```text
feat(bpm): 支持并行全员会签模型与编译
feat(bpm): 增加并行审批组运行时投影
feat(bpm): 输出审批组结构化运行时详情
feat(bpm): 完成审批组运行时展示与动作边界
docs(bpm): 记录并行全员会签验收
```

除非实现中出现本计划“全局约束”定义的真实阻塞，否则五个任务按顺序连续推进；每一批只在批次门禁处汇报进度和风险，不对 DSL、编译器、投影、API、页面之间已经确定的自然联动反复请求确认。

## 最终完成定义

本模块仅在以下条件同时满足时关闭：

1. `parallelAll` 只接受至少两名有效、不同的指定员工，并可从设计器发布。
2. 运行引擎得到固定 split/member/join BPMN，成员拥有独立 Hunyuan 任务投影和同一审批组。
3. 全员通过、任一拒绝、任一退回、撤回、取消、转办、委派、加减签边界和并发幂等均符合锁定契约。
4. 任务、实例详情和 trace 使用结构化审批组契约，前端不从内部 JSON 或命名规则推导会签。
5. 历史、单人和顺序多人流程保持兼容。
6. 相关测试、Flowable 兼容验证、前端类型检查和三人活体验收已有真实记录。
7. 验收记录与 BPM 开发基线已按真实结果更新，且未提交运行时证据产物。
