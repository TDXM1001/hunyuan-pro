# BPM 模块 M2：身份组织与审批策略设计

- 日期：2026-07-11
- 重审日期：2026-07-12
- 状态：设计完成，待实施
- 优先级：P1
- 总体蓝图：`docs/superpowers/specs/2026-07-11-bpm-enterprise-blueprint.md`
- 前置条件：M1 Graph、定义版本引用与发布事务契约稳定
- 依赖关系：`M1 -> (M2 + M3) -> M4`；M2 与 M3 可并行，M4 消费二者的冻结结果

## 1. 结论与边界

M2 统一回答三个问题：谁能发起或查看流程、谁有资格处理人工任务，以及多人审批在什么条件下形成唯一终态。公共语义是 Hunyuan 的范围策略、候选策略、成员快照、审批阶段和完成策略；不得向设计器、API 或页面暴露 Flowable multi-instance、task key 或引擎变量来表达这些规则。

M2 拥有组织身份接入边界、可版本化的策略目录、候选解析、预检与模拟、成员快照算法、参与者授权决策以及审批完成决策。员工、组织、角色等主数据仍由系统组织域所有；M2 只通过受控网关读取它们。M2 不拥有流程拓扑和编译产物（M1）、业务对象和可用于解析的路由事实（M3）、实例/任务/审批组的持久化与引擎推进（M4），也不拥有外部应用认证（M6）。

旧候选解析、顺序审批和 `parallelAll` 的算法、状态语义与测试仅作为重建证据；新库和公共 API 不为旧作者模型、旧审批组表或旧 VO 保持兼容。历史迁移属于 M8，不是本模块的隐含范围。

## 2. 完整业务线

```text
流程管理员维护身份范围、候选和审批策略版本
-> 在 Graph 的流程级/审批节点选择精确策略版本
-> M1 静态校验、样例模拟并冻结依赖快照
-> 申请人以 M3 已验证的发起与路由事实发起
-> M2 按声明时机解析组织身份并生成候选成员快照
-> M4 持久化审批阶段与成员事实、创建可处理任务
-> M4 对每次任务命令调用 M2 授权和完成决策
-> M4 原子地写成员结果、组终态、任务投影和流程推进
```

候选来源、成员排序、空集合与自审处理、允许动作、完成条件、终止条件和诊断必须由同一冻结策略版本解释。页面提示、编译器分支和任务服务不得各自重算或补写规则。

## 3. 领域模型与版本目录

```text
IdentityReference
  kind / stableId / displaySnapshot / organizationVersion
  EMPLOYEE | ROLE | DEPARTMENT | DEPARTMENT_MANAGER
  START_EMPLOYEE | START_DEPARTMENT_MANAGER | ROUTING_FACT_EMPLOYEE
  POST | USER_GROUP | MANAGEMENT_CHAIN

StartVisibilityPolicyVersion
  policyKey / policyVersion / lifecycleState
  startScope / visibilityScope / allowedOrganizations
  allowedRoles / allowedEmployees / ruleSchemaVersion

CandidatePolicyVersion
  policyKey / policyVersion / lifecycleState
  resolverType / resolverParameters / publishPrecheck / snapshotPhase
  memberOrder / duplicateRule / emptyCandidatePolicy / selfApprovalPolicy

ApprovalPolicyVersion
  policyKey / policyVersion / lifecycleState
  completionMode / ratioPercent / rejectionRule / returnRule
  terminationRule / allowedActions / riskLevel

ResolvedCandidateSnapshot
  definitionVersionId / authoredNodeId / policyVersions
  snapshotPhase / sourceFactsDigest / organizationVersion / resolvedAt
  members[] / effectiveMemberCount / requiredApprovalCount / diagnostics[]
```

三类策略版本均带有 `policyVersionId`、canonical payload、schemaVersion、contentDigest、`riskLevel`、`riskReasons`、创建人、启用人和启用时间；`riskLevel` 不能只放在审批策略中。策略版本只允许从草稿发布为 `ACTIVE`，已启用版本不可原地修改；变更必须生成新版本。`RETIRED` 版本不可再被新定义引用，但已发布定义继续按其定义版本中的冻结内容运行。目录记录完整冻结内容，以便回放与审计。

`publishPrecheck` 只说明发布时能否生成样例和诊断；它不是成员冻结时机。候选策略的 `snapshotPhase` 只能为 `START` 或 `ACTIVATE`：前者在实例创建事务内冻结成员，后者在审批阶段激活事务内冻结成员。策略若需要发起人、M3 路由事实或当前组织关系而发布期无法确定成员，必须标记为 `RUNTIME_REQUIRED`，不得把 `PUBLISH` 写成运行成员快照。

高风险能力包括宽发起/可见范围、允许自审、命名兜底、自动终态和高风险退回规则。此类版本必须写明 `riskReasons`，由拥有独立高风险启用权限且不是该版本创建人的人员确认启用；确认人、原因、时间和 canonical digest 一并审计。普通策略启用权限不能替代此确认，前端也不能通过默认值绕过它。

目录不得信任调用方声明的低风险级别，必须从 canonical payload 推导 `effectiveRisk`：

| payload 条件 | `effectiveRisk` |
| --- | --- |
| `AUTO_APPROVE`、`AUTO_REJECT`、命名员工/角色兜底、`SelfApprovalPolicy=ALLOW`、`returnRule=RETURN_ANCESTOR` | `HIGH` |
| 发起或可见范围为 `ALL`，或包含 `ROLE_IDS`、`DEPARTMENT_IDS` 的范围规则 | `HIGH` |
| `WHEN_APPROVAL_UNREACHABLE`、主管链解析或受控跨部门范围 | 至少 `MEDIUM` |
| 其余受控、显式且租户内的引用 | `LOW` |

声明 `riskLevel` 低于 `effectiveRisk` 时，校验结果为 `BLOCKING`；声明更高可以保留但必须说明原因。只有高风险独立确认成功，`WARNING` 才能随定义冻结，不能把风险降级为普通 `ACTIVE` 版本。

M2 的对外端口是 `PolicyCatalog`、`IdentityOrganizationGateway`、`CandidateResolutionService`、`ParticipantAuthorizationService` 和 `ApprovalCompletionService`。M1、M3、M4 只依赖这些端口及版本化 DTO，不得直接查询 M2 表或解析策略 JSON。

## 4. Graph 绑定与发布冻结

流程级发起/可见范围放在 Graph `policies`：

```json
{
  "startVisibilityPolicy": {
    "policyKey": "expense-employee-start",
    "policyVersion": 3
  }
}
```

人工节点使用精确版本引用。`APPROVAL` 必须同时拥有候选和审批策略；`HANDLE` 只拥有候选策略，办理语义和动作集合仍由 M4 定义。

```json
{
  "candidatePolicy": {
    "policyKey": "finance-manager",
    "policyVersion": 5
  },
  "approvalPolicy": {
    "policyKey": "two-of-three",
    "policyVersion": 2
  },
  "returnTargetNodeId": "manager-review"
}
```

`returnTargetNodeId` 仅在审批策略选择 `RETURN_ANCESTOR` 时必填，属于 Graph 节点绑定而非可复用策略版本。M1 发布时校验它指向同一已发布 Graph 中合法的 `APPROVAL` 节点，并存在从目标到当前节点的有向结构路径；M4 再以执行代事实确认它确为本次实际路径的祖先。编译时，M1 必须为可退回目标生成仅内部可调用的稳定恢复入口，并把目标 authored node、目标 `compiledElementId`、恢复入口 ID、`returnRule` 与完整策略快照一并冻结。随后 M1 通过 `PolicyCatalog` 校验引用存在、状态为 `ACTIVE`、节点类型匹配且参数 schema 合法，并把策略键、版本、不可变目录 ID、完整 canonical 策略内容、schema 版本、内容摘要、风险级别和节点级退回目标写入 `DefinitionVersionSnapshot.dependencyVersions`。`HANDLE` 只能绑定 `BLOCK` 或类型化兜底的候选策略，若所选策略包含 `AUTO_APPROVE` 或 `AUTO_REJECT`，发布必须拒绝；自动终态只适用于拥有 `ApprovalPolicyVersion` 的 `APPROVAL` 节点。运行时只读取这一完整依赖快照，不能按策略键或目录 ID查询“当前最新版本”。摘要仅用于完整性校验，不能替代可执行策略内容。现有仅冻结 `candidatePolicy` 的发布解析器是过渡实现，M2 落地时必须扩展为流程级范围和审批策略的完整校验与快照。

策略启用、退休与定义发布通过同一个目录线性化协议处理。M1 在发布事务中调用 `PolicyCatalog.freezeForPublication(reference, publicationRequestId)`；该操作以策略版本行锁或等价的目录 revision CAS 取得短期发布租约，返回完整 canonical payload，并在定义快照原子写入后提交。策略退休使用同一把锁或 revision：退休先提交时，后续冻结必须失败；冻结先提交时，该定义携带完整快照继续有效，而退休只阻止后续新引用。发布失败或回滚必须释放租约且不留下可运行的半快照。

发布预检的结论统一为：

| 级别 | 含义 | 发布行为 |
| --- | --- | --- |
| `READY` | 在发布期可以校验，并且样例上下文已得到确定有效成员 | 可发布 |
| `RUNTIME_REQUIRED` | 合法但依赖发起人、M3 路由事实或激活时组织关系 | 可发布，定义快照必须列出所需事实 |
| `WARNING` | 高风险兜底、自动终态或较宽范围等已授权风险 | 必须确认并留下审计 |
| `BLOCKING` | 版本不存在、引用失效、参数非法、静态候选为空或违反安全规则 | 禁止发布 |

## 5. 身份组织网关与候选来源

组织主数据域是员工有效性、部门隶属、角色、岗位、用户组和主管链的唯一权威来源；`IdentityOrganizationGateway` 是 BPM 读取这些事实的唯一受控入口。它输入当前租户和受控身份引用，输出有效员工、组织版本、解析路径和可审计原因；不得接收任意 SQL、EL、脚本、自由 URL 或由浏览器拼装的表达式。外部用户必须先由 M6 映射为当前租户的 Hunyuan 员工，才能进入人工任务成员集合。

首期解析器目录如下。新增来源必须注册类型化解析器、参数 schema、最小样例、权限需求和预检能力，不能通过字符串表达式绕过目录。

| 解析器 | 输入 | 预检/冻结时机 | 规则 |
| --- | --- | --- | --- |
| `EMPLOYEE` | 明确员工集合 | 发布预检、`START` 或 `ACTIVATE` 冻结 | 员工必须有效且属于当前租户 |
| `ROLE` | 角色与组织范围 | 激活 | 按组织网关返回的有效员工集合 |
| `DEPARTMENT_MANAGER` | 部门引用 | 发布预检、`START` 或 `ACTIVATE` 冻结 | 无有效负责人形成诊断 |
| `START_EMPLOYEE` | 发起人 | `START` | 只取服务端确认的发起人 |
| `START_DEPARTMENT_MANAGER` | 发起人部门 | `START` | 只取发起快照中的组织事实 |
| `ROUTING_FACT_EMPLOYEE` | M3 允许的人员路由事实键 | `START` 或 `ACTIVATE` | 不读取任意表单 JSON |
| `POST` | `positionId` | 激活 | 返回当前租户内该岗位的有效员工，不按角色或其他来源隐式回退 |
| `USER_GROUP` | `userGroupId` | 激活 | 返回当前租户内该用户组的有效成员；不存在、跨租户或无权读取是身份引用错误，不是角色回退 |
| `MANAGEMENT_CHAIN` | `chainType`、`seedIdentityReference`、`maxDepth` | 激活 | 受控解析主管链，按最近主管到最远主管输出，并在循环或超出深度时失败关闭 |

发布预检可以在 `PUBLISH` 期校验不可变引用并生成样例，但不产生运行成员快照；实际成员始终按 `snapshotPhase=START` 或 `ACTIVATE` 冻结。发布期无法确定的成员不伪造样例；策略必须声明所需的 M3 路由事实和组织事实，预检返回 `RUNTIME_REQUIRED`。激活后组织变化不回写已创建阶段的成员快照。

扩展来源的参数 schema 是策略契约的一部分，不能由前端自由拼装。`POST` 只接受正整数 `positionId`；`USER_GROUP` 只接受正整数 `userGroupId`；`MANAGEMENT_CHAIN` 的 `chainType` 必填且只能为 `DEPARTMENT_MANAGER_CHAIN` 或 `EMPLOYEE_REPORTING_CHAIN`，`seedIdentityReference` 必须是冻结的 `EMPLOYEE`、`DEPARTMENT`、`START_EMPLOYEE` 或 `ROUTING_FACT_EMPLOYEE` 引用，`maxDepth` 必须为 1 到 20 的整数。前者按受控部门父链逐层解析负责人，后者只在组织网关登记了员工汇报链能力时可用；任一链型所需网关能力未登记、种子身份无效、跨租户、链路循环或超过最大深度时，策略不得启用或发布为可运行引用，并返回节点级 `BLOCKING` 诊断。链路循环不得返回部分成员后继续执行。

解析结果先按稳定员工 ID 去重，再按策略声明的稳定排序（显式顺序、组织排序或员工 ID）生成成员序号。无确定排序的集合禁止用于 `SEQUENTIAL`。`sourceEmployeeId` 永远保留候选来源，`currentEmployeeId` 仅能由受控转办或委托改变；二者均不可由客户端覆盖。

## 6. 发起与可见范围

`StartVisibilityPolicyVersion` 分别表达发起资格和非任务可见性，避免把“能看流程”误当成“能办任务”。默认拒绝：未命中发起范围的用户不能创建实例，未命中可见范围的用户只能看到其本人提交、参与或被明确授权的最小事实。

范围可由已登记的组织、角色和明确员工集合组成，并使用并集/交集等类型化组合；禁止任意部门树查询或前端传入员工 ID 扩大范围。管理员的运营读取权限属于 M7，不由 M2 的可见范围隐式授予。

范围规则使用版本化 AST，不接受字符串表达式：

```text
ScopeRule
  ALL
  EMPLOYEE_IDS(employeeIds[])
  ROLE_IDS(roleIds[])
  DEPARTMENT_IDS(departmentIds[])
  ANY_OF(scopes[])
  ALL_OF(scopes[])
```

叶子 ID 必须是当前租户中已登记的正整数引用；组合节点至少包含两个子规则，最大嵌套深度为 8，空集合、重复子规则和未知字段一律 `BLOCKING`。`ALL`、跨部门范围和任何通过角色扩大可见面的规则都必须标明风险原因；M2 在启用和冻结时对规则 canonicalize，运行时只解释冻结 AST，不能由页面补充员工或部门。

实例创建时，M4 必须写入 `InstanceVisibilitySnapshot` 与结构化 `InstanceVisibilityGrantFact`：冻结策略版本、canonical digest、组织版本、范围 AST 摘要、由该 AST 在创建时物化的员工集合、发起人和初始显式授权。后续成员激活、抄送或独立授权只能追加新的参与者/授权 grant，不能按当前组织重新展开原范围。`InstanceAccessDecision` 只读取这些冻结 grant、发起人和已冻结参与者事实；组织变化不得追溯扩大既有实例的读取权限。

M4 不得用一个同时承担发起、可见、候选和动作授权的宽泛上下文调用 M2。`StartEvaluationContext` 仅含租户、冻结定义、服务端确认的发起人快照和决定时间，绝不含 `stageInvocationId`；`InstanceAccessContext` 含 `InstanceVisibilitySnapshot`、访问人、冻结参与者集合和 M4 已验证的显式授权事实；`CandidateResolutionContext` 含租户、完整定义快照、节点、`snapshotPhase`、发起人快照、解析时间和 M3 的 `RoutingFactView`，其中 `stageInvocationId` 只在 `ACTIVATE` 时存在；`ParticipantAuthorizationContext` 至少含租户、实例/阶段/任务/成员标识、服务端认证 actor、请求动作、成员与阶段版本和冻结策略；`ApprovalCompletionContext` 至少含租户、`stageInvocationId`、已授权动作、冻结策略、成员/阶段事实及其版本。两个动作上下文都不携带 M3 业务对象。M2 只消费这些版本化 DTO，不直读 M3 表或业务对象。

M2 输出 `StartDecision`、`InstanceAccessDecision`、候选快照、参与者授权和完成决定；M4 用决定控制实例发现性和任务入口。应用侧的列表和详情在返回任何审批对象前都必须执行 `InstanceAccessDecision`，管理员运营读取走 M7 独立授权；通过可见性判定的用户随后仍只能取得 M3 按字段权限裁剪的审批对象。M2 的实例可见性不能授予任何 M3 字段读取权。

## 7. 候选异常与自审策略

候选策略必须声明以下两项，默认值均为失败关闭。`ASSIGN_NAMED_EMPLOYEE` 和 `ASSIGN_NAMED_ROLE` 必须同时携带类型化且受租户约束的 `fallbackIdentityReference`；其解析只允许一次，不能递归触发新的空候选兜底。

```text
EmptyCandidatePolicy
  BLOCK (默认)
  ASSIGN_NAMED_EMPLOYEE | ASSIGN_NAMED_ROLE
  AUTO_APPROVE | AUTO_REJECT

SelfApprovalPolicy
  ALLOW
  SKIP_SELF
  ASSIGN_DEPARTMENT_MANAGER
  BLOCK (默认)
```

候选处理顺序固定为解析来源、过滤无效与去重、执行自审策略、处理空集合、最后校验完成模式人数。`SKIP_SELF` 后必须重新检查空集合；`ASSIGN_DEPARTMENT_MANAGER` 必须重新经过组织网关和自审检查，不能在主管链循环时回退到发起人。空集合命中 `AUTO_APPROVE` 或 `AUTO_REJECT` 时优先于成员和完成模式：M4 不创建成员任务，而是写入带策略版本、系统动作人和触发原因的自动阶段终态，并只推进一次阶段控制器。`BLOCK` 或兜底解析失败则失败关闭。`AUTO_APPROVE`、`AUTO_REJECT`、指定管理员/角色兜底和允许自审均为高风险能力：策略必须标为高风险、经独立权限发布、显示醒目警告，并在定义快照、解析诊断和运行事实中记录触发原因。普通流程管理员不能通过页面默认值获得这些能力。

身份引用失效、主管链循环、跨租户身份、重复成员、候选歧义、缺少运行时事实和组织网关故障都必须产生结构化诊断。解析失败不得创建无处理人的任务，也不得静默自动通过。`PUBLISH` 阶段的不可恢复错误返回 `BLOCKING` 并拒绝发布；`START` 阶段的失败必须回滚发起事务，不留下实例、阶段或引擎等待点；`ACTIVATE` 阶段的失败必须创建带诊断的 `EXCEPTION_PENDING` 阶段事实，但不创建成员任务，也不对 `ApprovalStageControl` 发出推进或关闭请求。空集合命中自动终态是唯一例外，仍按本节前述规则写入系统终态并只请求一次阶段控制副作用。

## 8. 审批阶段与成员快照

每个 `APPROVAL` 节点在运行时形成一个审批阶段。M2 计算并返回 `ResolvedCandidateSnapshot`；M4 将其持久化为 `ApprovalGroupFact` 和成员事实，负责任务创建、状态投影和引擎推进。这样 M2 可以演进策略算法，M4 仍是唯一的实例和任务数据所有者。

当 `snapshotPhase=START` 时，M4 在实例创建事务内调用 M2 并持久化唯一、不可变的 `PreFrozenCandidateSnapshotFact`，其键为 `instanceId + authoredNodeId + candidatePolicyDigest`，至少记录定义版本、完整候选快照、成员、成员数/阈值、源事实摘要、诊断和创建时间。它不是任务或审批阶段，不能带 `stageInvocationId`。每次等待点激活另写 `PreFrozenCandidateBindingFact(preFrozenSnapshotId, generationId, engineExecutionId, stageInvocationId, approvalStageId)`；同一 `engineExecutionId` 的重试只返回同一 binding，而新的执行代必须创建新的 binding、阶段和成员任务投影，但只能复用预冻结成员，绝不重新解析候选人。`snapshotPhase=ACTIVATE` 才允许在等待点激活时现场解析并直接创建阶段快照。

成员事实至少包含：成员序号、来源员工、当前处理人、成员状态、处理结果、关联任务 ID（未激活可为空）、候选解析摘要、激活/完成/取消时间和变更原因。成员快照还冻结有效成员数、比例所需通过数、策略版本、允许动作、拒绝/退回规则和诊断摘要。内部 JSON 仅用于回放；工作台读取 M4 的结构化成员与审批组投影，不从 JSON 推断状态。

转办只改变当前处理人，不改变来源成员、成员数或比例阈值。委托沿用受控重新分配语义，并保留授权链；原生引擎委派不是本模块前提。加签是独立附加任务或独立审批阶段，默认不进入 authored 阶段分母；减签只能取消未处理的附加任务。若业务需要改变比例分母，必须建立新的明确策略版本和完整验收，不能由高级动作隐式改变。

### 8.1 成员可用性与恢复

成员状态至少包括 `PLANNED`、`ACTIVE`、`APPROVED`、`REJECTED`、`RETURNED`、`TERMINATED`、`INELIGIBLE` 和 `CANCELLED`。M4 在创建任务、激活顺序成员和接受动作时都要通过组织网关复核当前处理人的有效性；此外必须幂等消费组织域的员工有效性变更事件，并对运行中的活动阶段执行受控补偿扫描（包括服务重启后的恢复扫描）。失效、离职、跨租户或无法再处理的成员只能从未终态进入 `INELIGIBLE`，并留下检测事实，不能静默重新解析整个候选集合。

`INELIGIBLE` 永不改变冻结分母或历史成员数。`stillEligibleCount` 只计入尚未终态且仍可由当前处理人完成的 `PLANNED`/`ACTIVE` 成员，不含已拒绝、已退回、已终止或 `INELIGIBLE` 成员。`SINGLE`、`SEQUENTIAL` 和 `ALL` 出现不可用成员时进入非可处理的 `EXCEPTION_PENDING` 阶段，等待受控处置，不推进引擎。`ANY` 和 `RATIO` 则按冻结的 `rejectionRule` 重新计算；仍有可达通过路径时继续，达到“不可能”条件时按 `returnRule` 结束。这样成员失效不会无限挂起，也不会静默通过。

恢复只能由 M4/M7 的受权命令将该成员受控转办给一个明确且有效的员工，保留来源成员、原/新处理人、处置人和原因；它不改变成员数、分母或策略版本。无法安全恢复时保留异常事实，由运营处置终止或重启实例，不能以当前组织重新解析覆盖原快照。

### 8.2 M1 编译与 M4 阶段控制契约

每个 Graph `APPROVAL` 节点由 M1 编译为一个具有稳定 `compiledElementId` 的 M4 控制阶段等待点，并在 authored/compiled 映射中关联 `authoredNodeId`、策略依赖快照和节点级退回目标。该等待点不是直接分配给人处理的公开 Flowable 任务；M1 可选择受控的 `receiveTask` 或内部 `userTask` 表达，但必须实现同一 `ApprovalStageControl` 端口，且不得把 multi-instance 作为 Graph 或 API 语义。

M4 在等待点激活时创建唯一 `stageInvocationId`、审批组事实和 M4 成员任务投影。每个成员的审批动作只更新 M4 事实并调用 M2 决策，绝不直接完成独立 Flowable 人工任务。决策为通过或自动通过时，M4 只对该 `stageInvocationId` 成功信号/完成一次；拒绝、退回、取消或异常则关闭同一控制阶段，并按 M4 的实例命令和冻结 Graph 创建退回执行代或终态。`ANY`、`RATIO` 的剩余成员终止只取消 M4 成员任务投影，不取消额外引擎 token。这样并发成员、提前终止和重放命令始终对应一个引擎等待点与一次可观察推进。

## 9. 完成与终止策略

```text
CompletionMode
  SINGLE | SEQUENTIAL | ALL | ANY | RATIO

ApprovalPolicyVersion
  completionMode
  ratioPercent (仅 RATIO，1..100)
  rejectionRule: IMMEDIATE | WHEN_APPROVAL_UNREACHABLE
  returnRule: RETURN_INITIATOR | RETURN_ANCESTOR | END_REJECTED
  terminationRule: CANCEL_REMAINING_MEMBERS
  allowedActions
```

`SINGLE` 有且仅有一个有效成员；静态预检或运行解析得到其他人数均为错误。`SEQUENTIAL` 按冻结序号一次激活一个成员，前一成员通过后才激活下一成员。`ALL` 要求全部有效成员通过。`ANY` 在第一位有效成员通过后成功并终止其余活动成员。`RATIO` 以冻结分母计算 `requiredApprovalCount = ceil(effectiveMemberCount * ratioPercent / 100)`，仅用整数比较；达到所需通过数后立即成功并终止其余活动成员。成员转办、组织变化、附加加签和页面计数均不改变分母或阈值。

拒绝语义必须随策略版本冻结。`SINGLE`、`SEQUENTIAL` 和 `ALL` 只允许 `IMMEDIATE`；`ANY` 与 `RATIO` 可选择 `IMMEDIATE`，或在全部通过不可能时才按 `returnRule` 结束。`RATIO` 的“不可能”条件为 `approvedCount + stillEligibleCount < requiredApprovalCount`。成员执行显式退回时立即按 `returnRule` 结束当前阶段。

| 触发 | M2 终态决定 | M4 引擎指令 |
| --- | --- | --- |
| 达到通过条件或自动通过 | `APPROVED` | 对当前 `stageInvocationId` 仅完成一次 |
| 拒绝、显式退回、自动拒绝或不可达 | 由冻结 `returnRule` 决定 `RETURNED` 或 `REJECTED` | 见下表的退回/终止分流 |
| `RETURN_INITIATOR` | `RETURNED` | 关闭当前控制阶段，实例进入待重提；不得重写既有成员或历史 |
| `END_REJECTED` | `REJECTED` | 关闭当前控制阶段并形成拒绝终态 |
| `RETURN_ANCESTOR` | `RETURNED` | 建立新的执行代并从冻结恢复入口激活目标审批节点；旧执行代、来源阶段和关联关系必须保留 |
| 取消 | `CANCELLED` | 按独立取消命令关闭当前控制阶段 |

`RETURN_ANCESTOR` 必须使用节点绑定中冻结的 `returnTargetNodeId`，且目标是当前已执行路径上的祖先审批节点。M4 的 `ExecutionGenerationFact` 至少记录 `generationId`、`parentGenerationId`、来源阶段、旧/新引擎实例 ID、目标节点、恢复入口、关联 ID、状态和时间。若运行时祖先校验不成立，M4 必须拒绝该命令并保留原阶段、成员和引擎等待点不变，只记录可审计的路径冲突诊断。M4 以新执行代实现，不能自由移动 Flowable token 或覆盖旧历史。

完成决策是纯领域计算：输入冻结审批策略、成员事实和本次已授权动作，输出新成员状态、组状态、是否激活下一成员、是否终止其余成员、终止原因和不可变 `EngineDirective`。M4 在同一事务内以 `instance -> approval group -> member -> task` 的固定锁顺序调用该决策、写入事实并按指令通过 `ApprovalStageControl` 推进、关闭或创建执行代。业务终态只能从活动状态单向进入通过、拒绝、退回或取消；`EXCEPTION_PENDING` 是非可处理的运营状态，只能由上一节定义的显式恢复命令离开，不能由普通审批动作绕过。

每个任务命令必须带客户端生成的、长度受限的 opaque `requestId`。服务端注入 actor 后，M4 必须先原子查找或占用 `CommandReceipt(tenantId, instanceId, requestId)`，再做任务/数据版本校验、M2 授权和完成决策。receipt 的 canonical 指纹至少包含任务、动作、服务端 actor、任务版本、审批对象版本、工作数据版本、规范化评论和附件引用；相同作用域且相同指纹直接返回既有结果，即使任务版本在首次执行后已过期，也不再授权、写事实或推进引擎；相同 `requestId` 配合不同指纹返回冲突。只有新 receipt 才进入后续校验和推进，结果或明确拒绝结果必须回写 receipt。幂等记录的保留期不得短于对应实例的审计保留期。

## 10. 参与者授权与命令边界

M2 的授权决策只回答当前服务端身份是否为冻结成员的当前处理人、是否有效、该动作是否被策略允许以及动作后是否仍满足策略。M4 负责认证身份注入、任务版本/实例版本检查、幂等认领、写动作证据、执行引擎和写投影。客户端不得提交实际处理人、成员状态、组计数、策略版本或完成结果。

非候选人、已失效员工、未映射外部用户、跨租户身份和未获授权的管理员一律不能处理任务。管理员的转办、取消和恢复必须经 M4/M7 的独立治理命令审计，不能通过伪造审批人身份绕过 M2。每次授权与完成决策都记录策略版本、成员 ID、命中规则、输入事实版本和决策原因，但不记录外部凭据或不必要的敏感业务字段。

## 11. 管理与运行体验

- 策略目录提供草稿、校验、样例模拟、风险标识、启用、停用和版本比较；启用后只允许复制为新版本。
- Graph 设计器使用类型匹配的选择器选择精确策略版本，显示版本摘要、解析时机、完成条件、拒绝/退回语义和风险，日常路径不得手输员工 ID 或解析表达式。
- 发布预检展示每个节点的 `READY`、`RUNTIME_REQUIRED`、`WARNING`、`BLOCKING` 结论、所需事实、候选样例、空集合和自审处理；`BLOCKING` 阻止发布。
- 任务和实例详情展示冻结候选来源、原始与当前处理人、计划/已激活/已处理成员、比例阈值、阶段进度、终止原因和策略版本；页面不展示或依赖 Flowable 内部标识。
- 参与者只能看到 M3 裁剪后的审批对象和其授权范围内的成员信息；策略模拟和组织范围配置只向有权限的流程管理员开放。

## 12. 安全、数据与兼容边界

- 所有组织查询强制当前租户、有效状态和受控范围；缓存必须带租户与组织版本，不能跨租户复用成员结果。
- 路由事实只从 M3 的冻结、脱敏、允许候选使用的字段读取；业务对象实时查询和任意表单 JSON 均不是候选输入。
- 策略、定义和运行快照分别按版本冻结，组织变化只影响尚未解析的新阶段；恢复时优先使用已有成员事实，不重新按“当前组织”解析。
- 不接受任意 EL、SpEL、脚本、动态 SQL 或自由字符串表达式；新解析器的代码、schema、最小测试和权限模型都须登记。
- M2 新模型不回填无法证明的旧计划成员，不增加旧 API 字段来伪造新语义，也不把旧 `parallelAll` / `sequential` 字符串作为新公共枚举。

## 13. 交付批次

1. 建立身份组织网关、三类策略版本目录、生命周期、权限和目录端口；将 M1 发布解析器改为完整冻结流程级/节点级引用。
2. 实现候选解析、模拟、`READY/RUNTIME_REQUIRED/WARNING/BLOCKING` 预检、空集合与自审失败闭环，并接入 M3 路由事实白名单。
3. 实现成员快照、参与者授权和完成决策，先以 `SINGLE`、`SEQUENTIAL`、`ALL` 证明与现有算法资产等价但不复用旧公共模型。
4. 实现 `ANY`、`RATIO`、并发终态、未达阈值和剩余成员终止；由 M4 完成统一任务命令与结构化运行投影。
5. 完成策略目录和 Graph 设计器、运行详情、内置通用申请的真实端到端验收；高级动作仅按本设计边界接入。

## 14. 验收矩阵

| 场景 | 必须证明 |
| --- | --- |
| 策略版本与发布 | 草稿策略不可被运行引用；`ACTIVE` 精确版本连同完整 canonical payload 被 M1 原子冻结；停用后旧定义仍按快照运行、新引用被阻断；发布与退休并发时只允许一个线性化结果，失败不留半快照；目录从 canonical payload 推导 `effectiveRisk`，高风险版本必须有独立启用人和风险原因 |
| 发起与可见范围 | 范围内员工可发起，范围外、跨租户与未映射身份被拒绝；实例可见范围在创建时物化为冻结 grant，参与者最小可见性不被组织变化扩大；应用侧列表和详情都先经过冻结 `InstanceAccessDecision`，管理员读取不复用该判定 |
| 跨模块输入与可见性 | M4 分别以 `StartEvaluationContext`、`InstanceAccessContext`、`CandidateResolutionContext`、授权/完成上下文调用 M2；M2 不直读 M3 表，且实例访问决定不能绕过 M3 字段裁剪 |
| 候选解析 | 指定员工、角色、部门负责人、发起人相关、M3 路由人员、岗位、用户组和两种已登记主管链的结果可解释、去重且排序稳定；主管链深度边界与循环必须失败关闭；`START` 预冻结是不可变来源，每个执行代创建独立绑定和审批阶段，`ACTIVATE` 才现场解析 |
| 预检和模拟 | 静态有效、运行时所需、高风险警告和阻断错误分别返回正确等级与节点定位；未登记的组织网关能力不能形成可运行策略引用 |
| 空集合与自审 | 默认阻断；自审跳过后重新校验空集合；具备类型化兜底目标；自动通过/拒绝先于完成模式写入系统终态且只推进一次；高风险兜底需权限、警告和运行审计；候选失败在发起时回滚、在激活时进入无任务的 `EXCEPTION_PENDING` |
| 成员冻结与授权 | 组织变化不重写已创建成员；转办保留来源与阈值；非候选人、失效员工和伪造身份不能处理任务；冻结成员失效进入可处置异常或按可达性规则结束，不永久挂起 |
| 五种完成模式 | `SINGLE`、顺序、全员、或签和比例模式各覆盖通过、拒绝/退回、阈值边界和组终态投影 |
| 阶段控制与引擎 | 每个审批节点只有一个 M4 控制阶段等待点；多人动作、自动终态、剩余成员取消和退回都与一次且可追溯的引擎推进或关闭对应；祖先退回保留旧执行代并建立可对账的新执行代，绝不自由移动既有 token |
| 并发与幂等 | 双击、两个成员竞争通过、通过与拒绝竞争、重放同一 receipt 指纹的 `requestId` 即使任务版本已变化也只返回既有结果，不形成第二个成员/组/实例终态或阶段控制推进；同作用域但不同指纹的 `requestId` 必须冲突 |
| 高级动作边界 | 转办、委托、加签、减签、撤回/取消和祖先退回不静默改变 authored 成员分母、阈值或历史；祖先退回目标不在实际执行路径时拒绝命令且不产生引擎副作用 |
| 真实产品线 | 内置通用申请从策略选择、定义发布、发起、多人审批到 M4 终态可在浏览器和服务端证据中闭环 |

## 15. 完成定义与后续边界

M2 只有在策略目录与 Graph 冻结、组织解析和诊断、发起/可见范围、成员冻结、服务端授权、`SINGLE/SEQUENTIAL/ALL/ANY/RATIO` 的完成决策、并发幂等、管理和运行投影以及验收矩阵全部闭环后才可关闭。单独增加一种候选来源、实现或签，或复用旧审批组页面都不构成模块完成。

M2 关闭只表示身份组织与审批策略能力面完成。M1-M4 共同通过内置通用申请主路径、关键异常路径、权限边界和真实运行证据后，才能宣称首个可用审批产品基线关闭。
