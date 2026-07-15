# BPM 可视化审批规则与业务对象配置验收记录

- 日期：2026-07-15
- 分支：`main`
- 验收基线：`git rev-parse HEAD = bedae7ba0d52cde40c4f0f3e6d9ef199208e11e5`
- 验收对象：当前本地工作树（包含本次未提交实现与验收文档改动；未包含 `docs/bpm-manual-verification-guide.md`）
- 状态：`RELEASABLE`
- 结论：可视化审批规则、业务对象、Graph 引用、通用申请、审批工作台、管理端实例/任务对账、技术权限边界、v1->v2 升级兼容、响应式与 SQL 重放证据均已闭环

## 1. 本次交付事实

- 审批规则与业务对象从“直接编辑 JSON”收敛为业务可读详情页；技术协议保留只读导出与只读面板，不再要求业务管理员阅读 canonical JSON。
- 管理端审批规则详情、业务对象详情恢复了基于 `admin` 角色的技术只读入口；业务用户直连技术接口仍由服务端拒绝。
- 管理端流程任务详情改为调用 `/bpm/task/detail/{taskId}`，不再误用运行端 `/app/bpm/task/detail/{taskId}`。
- 管理端流程实例详情、流程任务详情、审批阶段面板、流程路径面板移除了原始 JSON、节点 ID、运行时分配快照和 `vundefined` 之类业务不可读展示。
- 旧版 v1 业务对象 `m1_acceptance_contract` 可以升级出 v2 草稿；原 v1 行及其 JSON/digest 保持不变。

## 2. 实库对象与闭环主链

### 2.1 可视化配置对象

| 对象 | 关键标识 | 版本/行 ID | 状态 |
| --- | --- | --- | --- |
| 候选策略 | `visual_accept_huke_20260715` / 胡克审批人 | `v1` / `candidate_policy_version_id=18` | `ACTIVE` |
| 审批策略 | `visual_accept_all_approve_20260715` / 全部通过 | `v1` / `approval_policy_version_id=7` | `ACTIVE` |
| 发起范围策略 | `visual_accept_admin_start_20260715` / 管理员可发起 | `v1` / `start_visibility_policy_version_id=3` | `ACTIVE` |
| 业务对象 | `visual_expense_20260715` / 费用申请 | `v1` / `business_contract_version_id=5` | `ACTIVE` |
| Graph 发布版本 | `visual_expense_flow_20260715` / 费用申请审批 | `graph_definition_version_id=41` | `ACTIVE` |
| Graph 草稿 | 同一流程草稿 | `draft_id=28` | 已存在 |

### 2.2 真实运行闭环

| 对象 | 关键标识 | 结果 |
| --- | --- | --- |
| 流程实例 | `instance_id=127` / `DK20260715NO01009` / `FY-20260715-0001` | `run_state=3`，`result_state=1`，管理员发起后已结束且通过 |
| 流程任务 | `task_id=178` / 胡克审批 | `task_state=2`，`task_result=1`，处理人为胡克，部门为抖音组 |
| 动作轨迹 | `action_log_id=160` | `action_type=M2_APPROVE`，操作者胡克，意见“同意” |

## 3. 自动化门禁

Maven 工作目录为 `hunyuan-backend`，pnpm 工作目录为 `hunyuan-design`。

| 门禁 | 本次结果 |
| --- | --- |
| `pnpm test:unit -- apps/hunyuan-system/src/api/system/bpm apps/hunyuan-system/src/components/bpm apps/hunyuan-system/src/views/system/bpm apps/hunyuan-system/src/router` | 25 个测试文件、60 个测试通过 |
| `pnpm --filter @hunyuan/system typecheck` | 退出码 0 |
| `mvn -pl hunyuan-bpm -am test` | 436 个测试，0 失败，0 错误，0 跳过，`BUILD SUCCESS` |
| `mvn --% -pl hunyuan-admin -am -Dtest=BpmFlowableCompatibilityTest -Dsurefire.failIfNoSpecifiedTests=false test` | 11 个测试通过，真实 MySQL/Flowable 兼容门禁通过 |
| `mvn -pl hunyuan-admin -am -DskipTests package` | reactor `BUILD SUCCESS`，后端可执行包重新打包成功 |

本次前端额外增加/更新的契约门禁覆盖了：

- 管理端任务详情必须使用 admin detail API；
- 实例详情与审批阶段面板不得再暴露原始运行时内部字段；
- 业务对象 Graph 引用不得渲染 `vundefined`。

## 4. SQL 重放与兼容证据

- 增量脚本：`数据库SQL脚本/mysql/sql-update-log/v3.62.0.sql`
- 受控数据库：`hunyuan`
- 临时备份：`C:\Users\admin\AppData\Local\Temp\bpm-v362-backup-20260715-141228\hunyuan-v362-backup.sql`
- 执行方式：同一数据库连续执行两次，两次均成功；脚本中的 `SELECT 1` 输出证明二次执行走到了幂等分支

### 4.1 二次执行后列存在

- 三张策略版本表均存在：
  - `policy_name`
  - `description`
  - `business_summary`
  - `calculated_risk_level`
- 业务对象版本表存在：
  - `object_name`
  - `description`
  - `business_summary`

### 4.2 菜单与权限

- 菜单 `342`、`351` 已更新为“审批规则”“业务对象”。
- 新增按钮权限 `388-395` 均只保留一条：
  - `bpm:policy-catalog:save`
  - `bpm:policy-catalog:simulate`
  - `bpm:policy-catalog:technical`
  - `bpm:policy-catalog:delete`
  - `bpm:business-contract:save`
  - `bpm:business-contract:technical`
  - `bpm:business-contract:upgrade`
  - `bpm:business-contract:delete`
- `role_id=1` 对以上 8 个权限菜单授权各存在一条，无重复授权。

### 4.3 历史 v1 行不变

`m1_acceptance_contract` 的 v1 行在脚本执行前后保持一致：

- `business_contract_version_id=1`
- `contract_version=1`
- `schema_version=1`
- `contract_json={"businessType":"M1_LOCAL_ACCEPTANCE","fields":[]}`
- `contract_digest=NULL`
- `catalog_revision=0`

## 5. 真实浏览器与权限证据

### 5.1 业务闭环

本机运行服务：

- 后端：`http://127.0.0.1:1024`
- 前端：`http://127.0.0.1:5788`

真实页面已完成以下核对：

1. 管理员登录后可打开审批规则详情与业务对象详情。
2. 管理员侧审批规则详情显示：
   - 规则名称、编码、版本、状态、业务摘要、风险、Graph 引用
   - 技术协议入口存在，但页面默认不直接露出原始 JSON
3. 管理员侧业务对象详情显示：
   - 业务对象名称、编码、版本、状态、业务说明、业务摘要、Graph 引用
   - Graph 引用文案已修复为 `费用申请审批 · 已发布 v41`，不再出现 `vundefined`
4. 管理员侧流程实例详情显示：
   - `DK20260715NO01009`
   - 标题“差旅交通费用申请”
   - 发起人“管理员”
   - 审批阶段与流程路径使用业务可读中文，不再显示节点 ID、表单 JSON、可靠性追踪原始块
5. 管理员侧流程任务详情显示：
   - 任务名称“胡克审批”
   - 当前处理人“胡克”
   - 任务状态“已完成”
   - 任务结果“通过”
   - 动作轨迹为“审批通过 / 同意”
   - 不再显示任务标识和运行时分配快照

### 5.2 技术权限边界

- 以 `huke / 123456` 登录后，携带前端真实 access token 直连后端技术接口：
  - `/bpm/policy-catalog/technical-detail/CANDIDATE/visual_accept_huke_20260715/1`
  - `/bpm/business-contract/technical-detail/visual_expense_20260715/1`
- 两个接口均返回：
  - `code=30005`
  - `msg=对不起，您没有权限访问此内容哦~`

这证明技术协议权限仍在服务端关闭，前端不会因为隐藏失败而绕过。

## 6. 响应式与控制台/网络

核对视口：

- 桌面：`1440x900`
- 移动端：`390x844`

核对页面：

- 审批规则详情
- 业务对象详情
- 流程实例
- 流程任务

结果：

- 4 个页面均无页面级横向溢出；
- 移动端返回按钮可达；
- 审批规则详情与业务对象详情在移动端默认不直接显示技术 JSON；
- 本轮验收页面未出现新的 error 级浏览器控制台日志；
- 本轮验收页面未出现新的 401/403/404/500 网络错误。

## 7. v1 -> v2 升级兼容

`m1_acceptance_contract` 已完成真实升级验证：

- 旧版详情页点击“升级为可视化草稿”后成功跳转到：
  - `/system/bpm/business-contract/editor?contractKey=m1_acceptance_contract&contractVersion=2`
- 新生成 v2 草稿：
  - `business_contract_version_id=6`
  - `contract_version=2`
  - `schema_version=2`
  - `lifecycle_state=DRAFT`
  - `contract_digest=49a6f48e956d4a38d8718fe9e849ea7708cd0711027d5a6e0d3953c2fd7de537`
- 原 v1 行保持不变，见第 4.3 节。

## 8. 最终结论

本次计划要求的 11 类关键证据已关闭：

1. 后端 BPM 自动化门禁通过；
2. Flowable/MySQL 兼容门禁通过；
3. 后端打包成功；
4. BPM 前端聚焦 Vitest 通过；
5. `@hunyuan/system` typecheck 通过；
6. `v3.62.0.sql` 连续执行两次成功；
7. 业务管理员无需接触 JSON 即可完成规则、业务对象、Graph、发起与审批闭环；
8. 技术详情服务端权限与只读边界有效；
9. v1 运行兼容与 v1->v2 升级通过；
10. 桌面/移动端和浏览器控制台门禁通过；
11. 管理端实例/任务对账页已收敛为业务可读展示。

因此本次可视化审批规则与业务对象配置状态为 `RELEASABLE`。

当前唯一剩余边界不是功能缺口，而是发布动作尚未执行：代码与文档仍位于本地工作树，尚未做最终提交/基线落盘。
