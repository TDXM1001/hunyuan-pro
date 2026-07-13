# BPM M2 身份组织与审批策略验收记录

- 日期：2026-07-13
- 分支：`main`
- 验收基线：`11527e203d86d0eab46887833d4d19e129664c82` 加当前未提交 M2 修复
- 状态：`RELEASABLE`；实现、仓库门禁、真实运行矩阵、并发恢复和浏览器验收均已关闭
- 设计：`docs/superpowers/specs/2026-07-11-bpm-module-02-assignment-approval-strategy-design.md`
- 实施计划：`docs/superpowers/plans/2026-07-12-bpm-m2-assignment-approval-strategy-implementation.md`

## 1. 当前能力事实

- 策略目录保存候选、审批和发起可见性策略的不可变版本、生命周期、canonical JSON、摘要和发布冻结事实。
- Graph 发布冻结精确的候选与审批策略版本；审批节点编译为单一 `ApprovalStageControl` 等待点。
- 候选解析只允许已登记来源，运行阶段冻结成员、顺序、组织快照和诊断，不按最新策略或当前组织重算。
- 运行时保存阶段、成员、任务、动作和引擎副作用状态，支持 `SINGLE`、`SEQUENTIAL`、`ALL`、`ANY`、`RATIO` 的固定决策语义。
- 引擎副作用在审批动作事务提交后异步执行，`PENDING`、`CLAIMED`、`FAILED` 可由恢复服务对账，不同步重放不确定的 Flowable 操作。
- Flowable 历史确认流程结束后，Hunyuan 实例幂等收敛为 `FINISHED/APPROVED`；管理员现有“重同步实例投影”入口可修复历史 Graph 实例。
- Graph 草稿必须绑定有效且未删除的流程分类后才能发布，消除了“可发布但不可发起”的契约不一致。
- 动作日志保存 Graph 定义版本和定义来源；旧 `definition_id` 已改为可空，兼容 Graph 与 legacy 两类定义来源。

## 2. 本次自动化证据

| 门禁 | 本次结果 |
| --- | --- |
| `mvn -pl hunyuan-bpm -am test` | 444 个测试，0 失败，0 错误 |
| `BpmFlowableCompatibilityTest` | 6 个测试，0 失败，0 错误，Spring/Flowable 容器启动通过 |
| `pnpm -F @hunyuan/system run typecheck` | 退出码 0 |
| `pnpm exec vitest run apps/hunyuan-system/src` | 30 个测试文件、141 个测试通过 |
| 实例终态与恢复聚焦测试 | after-commit 异步、实例投影、引擎副作用恢复、控制端口和管理员重同步均通过 |
| Graph 发布契约测试 | 空分类在 Flowable 部署前被拒绝，完整测试类 8 个测试通过 |

## 3. 数据库与运行证据

- 本地开发库已执行并核对 `数据库SQL脚本/mysql/sql-update-log/v3.54.0.sql` 的 M2 表、索引和动作日志来源字段。
- `t_bpm_task_action_log.definition_id` 可空，存在 `graph_definition_version_id`、`definition_source` 和 Graph 版本索引。
- 验收策略：候选策略 `m2_acceptance_candidate@1`（版本 ID 2）、审批策略 `m2_acceptance_single@1`（版本 ID 1）、发起可见性策略 `m2_acceptance_start@1`。
- 验收分类 ID 6；Graph 定义版本 ID 3；流程键 `m2_acceptance_single_flow_v3`。
- 当前后端由 PID `30924` 监听 `1024`，`/login/getCaptcha` 返回 HTTP 200；前端通过 `http://127.0.0.1:5788` 验收。

### 3.1 新实例正常主链

| 事实 | 值 |
| --- | --- |
| 实例 | ID 93，`DK20260713NO01021` |
| 任务 | ID 135 |
| 审批阶段 | ID 3，`manager_review`，`SINGLE` |
| Flowable 流程实例 | `9d9e8d9e-7e60-11f1-b28f-a8e2913e212c` |
| 阶段调用 | `9d9fc625-7e60-11f1-b28f-a8e2913e212c` |
| 最终状态 | 阶段 `APPROVED`，成员 `APPROVED`，实例 `runState=3/resultState=1` |
| 任务投影 | 当前任务 0，已办包含实例 93 |
| 动作日志 | ID 121，`M2_APPROVE` |

Flowable 日志确认历史实例写入结束时间、运行 execution 删除，随后执行 Hunyuan 实例条件更新和阶段副作用 `COMPLETED` 更新。

### 3.2 历史不一致恢复

- 修复前实例 91、92 均为阶段 `APPROVED`、Flowable 已结束、Hunyuan 实例仍为 `RUNNING`。
- 调用现有 `/bpm/instance/resyncProjection/{instanceId}` 后，两者均从 `runState=1` 收敛为 `runState=3/resultState=1`，并写入 `finishedAt`。
- 恢复过程不重放 Flowable 审批信号，只读取历史结束事实并执行幂等实例投影。

## 4. 浏览器证据

- 认证登录后，审批策略目录实际显示 `m2_acceptance_candidate`、版本 `v1`、状态 `ACTIVE`。
- 可发起流程页面实际显示 Graph 定义 `m2_acceptance_single_flow_v3` 和验收分类。
- 我的已办页面实际显示实例 91、92、93，任务结果均为“通过”。
- 实例 93 详情实际显示：当前待办 0、`manager_review`、单人审批、`1/1 已通过`、候选策略 v2、审批策略 v1、终止原因 `APPROVED`、成员结果 `APPROVED` 和动作 `M2_APPROVE`。
- 登录后重新加载我的已办页面，没有新增浏览器控制台错误。
- 审批策略目录实际显示角色、部门主管、岗位、用户组、主管链、路由事实和发起人来源策略均为 `ACTIVE`。
- 实例 113 详情实际显示 `ANY`、`1/3 已通过`、`已处理 3/3`、员工 44 `APPROVED`、其余两名成员 `TERMINATED`，动作轨迹只有一条 `M2_APPROVE`。
- 已登录状态重新加载实例列表后无新增控制台错误；仅存在前端基础设施的 LocalStorage 空前缀警告。

## 5. 本次活体验收中修复的问题

- 非法历史策略正文中的未登记字段曾拖垮整个策略目录；现改为目录隔离，绑定、启用、冻结等写路径继续严格失败关闭。
- 非 `RATIO` 审批策略曾错误要求 `ratioPercent`；现仅 `RATIO` 强制提供，其他模式规范化为 100。
- MyBatis-Plus `@Version` 与阶段 `updateById` 冲突；现使用显式 revision CAS 更新阶段终态。
- Graph 动作日志曾缺少定义来源字段；现补充 Graph 版本和来源，并通过增量 SQL 兼容旧定义 ID。
- Flowable 结束事实在 `AFTER_COMMIT` 回调资源清理前对新事务不可见；现将引擎副作用提交到既有异步执行器，回调退出后再触发和投影。
- Graph 空分类曾允许发布但运行时拒绝；现发布前阻断。
- Flowable 连续 receiveTask 会复用 execution ID，原实现曾把第二审批节点误判为第一节点重放；现按实例、authored 节点、generation 和引擎绑定生成确定性阶段调用 ID，实例 115 已验证双节点连续推进。
- 同一 authored 节点、同一 execution 的重复激活始终复用原阶段绑定；即使阶段及引擎副作用已经完成，也不会重建候选、成员和任务事实，聚焦回归测试已覆盖。

## 6. 尚未关闭的验收项

### 6.1 2026-07-13 继续实施证据

- 候选解析已补齐 `USER_GROUP`、部门主管链、员工汇报链，并对循环、重复主管、超深度和失效员工失败关闭。
- 自审 `ALLOW`、`SKIP_SELF`、`ASSIGN_DEPARTMENT_MANAGER`、`BLOCK` 与空候选命名兜底、`AUTO_APPROVE`、`AUTO_REJECT` 已进入统一解析顺序；自动终态写零成员阶段事实且不创建成员任务。
- 审批命令新增按 `tenantId + instanceId + requestId` 持久化回执；同指纹完成回执可重放，异指纹冲突，唯一键竞争者不再重复推进阶段。
- `v3.54.0.sql` 已声明用户组、用户组成员、员工汇报关系和审批命令回执四张表；`BpmSchemaSourceTest` 覆盖关键主键、唯一键和指纹字段。
- 当次聚焦回归：候选解析、策略校验、阶段创建、自动终态激活、命令幂等和 schema 共 50 个测试，0 失败、0 错误。

### 6.2 治理缺口关闭证据

- 高风险策略由服务端从 canonical payload 推导；普通启用入口失败关闭，独立 `bpm:policy-catalog:activate-high-risk` 权限要求确认人与创建人分离，并保存确认原因、时间和 digest。
- M2 authored 成员通过独立 `bpm:task:m2-member-transfer` 治理命令受控转办；只更新 `currentEmployeeId` 和任务投影，保留 `sourceEmployeeId`、冻结成员数和比例阈值，并记录原处理人、新处理人、处置人和原因。
- 周期补偿扫描通过组织网关复核开放成员；失效成员幂等转为 `INELIGIBLE`。`SINGLE/SEQUENTIAL/ALL` 进入 `EXCEPTION_PENDING`，`ANY/RATIO` 复用冻结完成决策判断仍可达或拒绝终态；受控转办可恢复成员、待办与异常阶段。
- 当次 `mvn -pl hunyuan-bpm -am test` 共 441 个测试，0 失败、0 错误；`git diff --check` 无空白错误。
- 本地 `hunyuan` 数据库已补执行高风险审计字段、组织关系、命令回执、字典及权限迁移，事务返回 `M2_MIGRATION_OK`。
- 新后端包构建成功并由 PID `65144` 监听 `1024`；启动日志包含 `Started AdminApplication`，验证码接口 HTTP 200，管理员登录成功。

### 6.3 五模式、自动终态和跨节点证据

| 能力 | 实例 | 实际结果 |
| --- | --- | --- |
| `SINGLE` | 93 | 1 名成员通过，实例 `FINISHED/APPROVED` |
| `SEQUENTIAL` | 94 | 3 名成员按冻结顺序通过，实例 `FINISHED/APPROVED` |
| `ALL` | 95 | 3 名成员全部通过，实例 `FINISHED/APPROVED` |
| `ANY` | 96 | 1 名成员通过、2 名终止，实例 `FINISHED/APPROVED` |
| `RATIO` | 97 | 3 人、67% 阈值向上取整为 3，全部通过后结束 |
| `AUTO_APPROVE` | 98 | 0 成员、0 任务，阶段及实例自动通过，副作用 `COMPLETED` |
| `AUTO_REJECT` | 99 | 0 成员、0 任务，实例自动拒绝，副作用 `COMPLETED` |
| 双审批节点 | 115 | 任务 169、170 由员工 44、47 连续完成；两个独立阶段调用均 `COMPLETED` |
| `RETURN_INITIATOR` | 116 | 任务 171 写入 `M2_RETURN`，阶段 `RETURNED`，实例进入 `WAIT_RESUBMIT` |

### 6.4 组织来源与治理证据

| 来源或治理 | 实例 | 实际成员或结果 |
| --- | --- | --- |
| `ROUTING_FACT_EMPLOYEE` | 102 | 冻结业务契约声明的 `approverId=44`，员工 44 完成 |
| `ROLE` | 103 | 角色 59 解析员工 2 |
| `DEPARTMENT_MANAGER` | 104 | 部门 8 解析员工 47 |
| `POST` | 105 | 岗位 5 解析多成员，员工 47 触发 `ANY` 终态 |
| `USER_GROUP` | 106 | 用户组 1 解析员工 44 |
| 部门主管链 | 109 | 完整展开并 `SKIP_SELF`，员工 47 完成；深度 1 和 `BLOCK` 自审均另有失败关闭证据 |
| 员工汇报链 | 110 | 员工 47 的上级员工 44 完成 |
| `START_EMPLOYEE` | 111 | 发起人 2 自审；高风险策略由创建人 1 之外的员工 44 独立确认 |
| `START_DEPARTMENT_MANAGER` | 112 | 发起人 2 的部门主管员工 47 完成 |
| M2 受控转办 | 100 | 来源员工 2 保留，当前处理人转为 68，冻结分母不变并完成 |
| `INELIGIBLE` 恢复 | 101 | 员工 2 失效后进入 `EXCEPTION_PENDING`，转办 68 后恢复并完成，员工状态已还原 |

### 6.5 幂等、并发与重启恢复证据

- 实例 96、任务 140、请求 `m2-any-live-001`：同指纹历史回放返回成功；不同 comment 形成异指纹时返回 `requestId 已被不同审批命令占用`，动作数前后均为 1。
- 实例 113：员工 2 与 44 同时提交 `ANY` 终态，只有任务 166 成功；任务 165 返回“审批任务已处理”，当前任务 0，动作日志仅一条。
- 服务停止后将实例 109、110、112 的已确认阶段分别注入 `PENDING`、`CLAIMED`、`FAILED`，再由 PID 50136 重启恢复。`CLAIMED/FAILED` 首轮只读 Flowable 历史收敛；`PENDING` 唯一领取失败后下一轮只读历史收敛，最终三者均为 `APPROVED/COMPLETED`，动作数仍各为 1、当前任务仍为 0。
- 最终运行包由 PID 30924 启动并通过验证码健康检查；缺陷复现实例 114 已通过管理员治理入口取消，不留开放待办。

### 6.6 发布判定

- 增量 SQL、字典、权限、策略审计、成员治理、真实组织数据、五种完成模式、自动终态、退回、跨节点、幂等、并发、重启恢复和浏览器详情均已有当次证据。
- 本次矩阵未发现仍会阻止 M2 发布的实现级缺口；`NOT_RELEASABLE` 关闭，发布状态更新为 `RELEASABLE`。

## 7. 当前结论

M2 的策略目录、Graph 绑定、候选解析、阶段与成员事实、审批决策、任务授权、一次性 Flowable 推进、实例终态投影、治理恢复、管理页面和详情展示均已落地。第 6 节的真实运行矩阵、最终自动化门禁和浏览器验收已关闭，M2 状态为 `RELEASABLE`，模块可以关闭。
