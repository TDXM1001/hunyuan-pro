# BPM M1 流程建模与编译平台验收记录

- 日期：2026-07-11
- 状态：历史 M1 实现验收已关闭；第 6 节记录的当前 Graph M1 已完成独立验收并关闭
- 实施计划：`docs/superpowers/plans/2026-07-11-bpm-m1-modeling-compiler-implementation.md`

## 1. 当前能力事实

- v1 线性模型保持兼容；v2 使用最大深度 3 的受控递归 AST。
- 编译器支持 `USER_TASK`、`HANDLE_TASK`、`COPY_TASK`、`EXCLUSIVE_BRANCH`、`PARALLEL_BRANCH`、`INCLUSIVE_BRANCH`，并生成固定 split/join 拓扑。
- 排他和包容路由由 `${hunyuanRouteDecisionDelegate}` 计算，决定按实例、引擎代际和路由节点幂等保存。
- 独立并行和包容分支禁止人工节点配置 `EDITABLE` 字段。
- 员工任务通过 `taskKind/availableActions` 区分审批和办理，不由前端猜测。
- 模型资产支持 v1 归一化、v2 JSON 导入导出、未知版本/节点类型/重复 key/超深拒绝。
- 实例 trace 返回 authored 运行图和路由记录；员工 trace 裁剪表单原值与路由诊断原文。
- `v3.47.0.sql` 以幂等方式增加 28 组 BPM 字典，覆盖现有枚举、M1 节点/路由、表单类型、审批模式、命令状态、任务日志动作和样板业务状态。
- `v3.48.0.sql` 允许业务实例先取得 Hunyuan `instance_id`、再启动 Flowable 并回填引擎实例 ID；字段仍保持 `varchar(128)` 和原索引。
- 分支 JUEL 通过 `execution.getVariable(...)` 读取 Hunyuan 布尔变量，未命中分支返回 `null`，不会因顶层标识缺失中断网关计算。

## 2. 本次自动化证据

| 门禁 | 本次结果 |
| --- | --- |
| `hunyuan-bpm` 全量测试 | 261 个测试，0 失败，0 错误 |
| Flowable 兼容门禁 | 3 个测试，0 失败，0 错误 |
| 前端 BPM 四文件契约 | 69 个测试，0 失败 |
| 前端最终页面契约 | 38 个测试，0 失败 |
| `@hunyuan/system` 类型检查 | 退出码 0 |
| 样板与 M1 聚焦门禁 | 17 个测试，0 失败，0 错误 |
| 字典迁移契约 | 6 个测试，0 失败，0 错误 |

`BpmM1BusinessFlowTest` 直接读取生产样板 JSON，验证 AST 校验、BPMN 编译、节点快照、`4999/5001/缺值` 排他选择，以及包容单命中和多命中。该测试属于编译与条件业务契约，不冒充数据库级 Spring/Flowable 端到端流转。

## 3. 本次运行证据

- 后端使用最新 jar 重启，监听 `127.0.0.1:1024`，最终进程 PID `33252`，`/login/getCaptcha` 返回 HTTP 200。
- Chrome 打开递归流程设计器，确认受控流程树、节点类型选择、深度 `0 / 3`、导入/导出和节点属性区正常渲染。
- Chrome 首次进入设计器发现递归组件把 prop 直接用于 `v-model`，修复为显式事件转发后页面运行编译恢复。
- 我的待办继续由后端 `availableActions` 渲染审批动作；现有数据库没有办理类型待办，因此办理按钮隔离以自动化证据为准。
- 员工实例 `DK20260711NO01125` 的详情显示 authored 流程路径和安全字段变更记录。
- 同一员工详情曾暴露隐藏字段原值，修复后 Chrome 确认 `hasRawSnapshot=false`、`hasSecret=false`，流程路径和安全变更记录仍可见。
- `390x844` 视口下详情抽屉最终边界为 `left=0`、`right≈390.4`、页面滚动宽度 `390`，无横向溢出；验收后已恢复默认视口。
- Chrome 最终回到 `http://127.0.0.1:5788/system/bpm/runtime/my-todo-list`。

## 4. 数据库与多路径实流证据

- `v3.47.0.sql` 已由用户执行；`v3.48.0.sql` 已在获准后执行。`information_schema.COLUMNS` 核验 `engine_process_instance_id` 为 `YES / varchar(128)`，注释 UTF-8 字节为 `E5BC95E6938EE5AE9EE4BE8B4944`。
- 样板自动升级并发布为 definition `21`。升级前 definition `20` 暴露的旧 JUEL 缺失变量错误已通过失败测试、最小编译修复和 Flowable 部署测试闭环；旧验收实例 `85` 已取消。
- `4999`：expense `26`、instance `86`；task `123` 走 `sample_finance_review`，task `124` 走 `sample_archive_review`；routeDecision `3/4` 命中 `small_amount`、`archive_confirm`。
- `5001`：expense `27`、instance `87`；并行 task `125/126` 分别走 `sample_large_finance_review`、`sample_risk_review`，汇合后 task `127` 走归档；routeDecision `5/6` 命中 `large_amount`、`archive_confirm`。
- `10000`：expense `28`、instance `88`；并行 task `128/129`、归档 task `130` 全部完成；routeDecision `7/8` 命中 `large_amount`，随后同时命中 `finance_copy/archive_confirm`；生成 `DESIGN_NODE_COPY` copy `2`。
- 缺失金额：instance `89`；办理 task `131/132` 分别走 `sample_manual_handle`、`sample_missing_amount_archive`；routeDecision `9/10` 均 `defaultBranchUsed=true`，命中 `manual_check`、`missing_amount_archive`。
- 四个实例最终均为 `runState=3/resultState=1` 且无待办。业务回调 `10/11/12` 均成功、重试数为 `0`；expense `26/27/28` 均更新为审批通过状态 `2`，核定金额分别为 `4999.00/5001.00/10000.00`，最终表单版本均为 `1`。
- Chrome “我的抄送”真实显示实例编号 `DK20260711NO01151`、标题 `M1_10000_20260711202251258`、类型 `DESIGN_NODE_COPY`、来源节点“财务抄送”。详情抽屉同时显示大额并行节点、设计时抄送、归档办理、两条 routeDecision 和成功回调记录。

## 5. 关闭结论与边界

M1 的受控 AST、编译器、排他/并行/包容分支、办理、设计时抄送、运行图和路由账本已形成可维护闭环，当前交付块关闭。任意 BPMN、脚本 EL、自由连线、Flowable multi-instance、时间事件和子流程仍是明确非目标；下一主交付块切换为 M4“时间、SLA 与事件驱动”。

## 6. 2026-07-12 正式 Graph 重建执行证据

本节只记录新 `HunyuanProcessDefinitionGraph` 定义中心的本次验证，和上文旧 AST 历史验收严格区分。

### 已完成事实

- 正式作者契约、canonical JSON、语义 hash、稳定 ID、草稿 revision、模板复制、导入导出、Graph 编译、不可变定义版本、元素映射、发布/下线权限及 Graph 设计器均已落入新 Graph 所有权模块。
- 本次补齐 Graph 本地输入边界：scope、node、edge ID 统一限制为字母开头、仅包含字母数字下划线、最长 128 位；非法 ID 在草稿保存和导入前即被拒绝。
- 发布冻结 Graph、依赖版本、BPMN、编译器版本与 authored/compiled 映射；映射写入异常会删除已部署 Flowable 定义，避免部署残留。

### 本次自动化与 Flowable 证据

| 门禁 | 本次结果 |
| --- | --- |
| Graph 聚焦后端测试 | 33 个测试，0 失败，0 错误 |
| `hunyuan-bpm` 全量测试 | 323 个测试，0 失败，0 错误 |
| Graph Flowable 兼容门禁 | 6 个测试，0 失败，0 错误；条件、并行、办理、抄送 Graph 成功部署并可读取 BPMN 元素 |
| 前端 Graph 契约 | 1 个测试通过 |
| `@hunyuan/system` 类型检查 | 退出码 0 |

Flowable 兼容门禁须从 `hunyuan-backend` 使用 `mvn -pl hunyuan-admin -am -Dtest=BpmFlowableCompatibilityTest -Dsurefire.failIfNoSpecifiedTests=false test` 运行；单独选择 `hunyuan-admin` 会从本机 Maven 仓库读取旧 BPM JAR，不能代表当前工作树的 Graph 实现。

### 本轮最终回归

| 门禁 | 本轮结果 |
| --- | --- |
| `hunyuan-bpm` 全量测试 | 325 个测试，0 失败，0 错误 |
| Graph 发布版本恢复服务与控制器 | 8 个测试，0 失败，0 错误 |
| Graph Flowable 兼容门禁 | 6 个测试，0 失败，0 错误 |
| 前端 Graph 模型、设计器与版本契约 | 9 个测试，0 失败 |
| `@hunyuan/system` 类型检查 | 退出码 0 |
| `hunyuan-admin` 运行 JAR 打包 | `BUILD SUCCESS` |

### 认证浏览器业务链验收

- 在已认证管理员会话中创建并保存草稿 `draftId=2`，`processKey=m1_graph_acceptance_20260712_1443`；页面显示“模拟通过”和“语义已保存”。
- 已发布版本为 `v1`，Flowable 引擎定义为 `5f343e1c-7dc3-11f1-a8e1-a8e2913e212c`。版本详情实际显示编译 BPMN、19 条 authored/compiled 映射和冻结信息。
- 冻结依赖为业务契约 `m1_acceptance_contract@1`，以及 `large_review`、`archive_review` 的候选策略 `m1_acceptance_policy@1`。
- 修复 Vue Proxy 不能直接 `structuredClone` 的草稿快照问题后，创建、保存、模拟、发布和版本查看均可执行。随后补充 `GET /bpm/graph-definition/latest-by-draft/{draftId}`，刷新设计器后仍可恢复“查看版本 v1”，不再依赖本次发布的内存状态。
- 本地开发库中的 `m1_acceptance_policy@1` 和 `m1_acceptance_contract@1` 是本次验收目录夹具，不属于提交的增量 SQL，也不代表生产业务目录数据。

### 关闭边界

M1 已关闭的是流程定义能力面：Graph 草稿、校验、模拟、编译、原子发布、冻结依赖、BPMN 与元素映射的可追溯查看。M1 不发起业务实例，不替代 M2 的实际审批人解析或 M3 的业务对象数据治理；生产发起仍必须在这些真实模块版本目录可用后另行验收。

### 2026-07-12 当前工作树复验

- 后端命令 `mvn -pl hunyuan-bpm test`：325 个测试，0 失败，0 错误；包含 Graph 往返、草稿 revision、模板复制、发布冻结/回滚、依赖目录解析、结构阻断定位、迁移与 API 隔离断言。
- Flowable 命令 `mvn --% -pl hunyuan-admin -am -Dtest=BpmFlowableCompatibilityTest -Dsurefire.failIfNoSpecifiedTests=false test`：6 个测试，0 失败，0 错误。
- 前端命令 `pnpm exec vitest run apps/hunyuan-system/src/components/bpm/graph/graph-process-model.test.ts apps/hunyuan-system/src/components/bpm/graph/graph-process-designer.contract.test.ts apps/hunyuan-system/src/api/system/bpm/graph-definition.contract.test.ts`：9 个测试通过；`pnpm --filter @hunyuan/system typecheck` 退出码 0。
- 本地服务 `127.0.0.1:1024` 与 `127.0.0.1:5788` 均返回 HTTP 200。已认证管理员页面重新读取既有草稿 `draftId=2`，显示“模拟通过”“语义已保存”“查看版本 v1”。版本详情显示当前 Flowable 定义 `5f343e1c-7dc3-11f1-a8e1-a8e2913e212c`、19 条 authored/compiled 映射，以及 `m1_acceptance_contract@1`、`m1_acceptance_policy@1` 冻结依赖；页面控制台未发现 error 级日志。
- 本轮未重新创建、发布、下线或复制验收夹具，避免向本地开发库重复写入定义版本；这些有副作用步骤沿用上文已记录的首次业务链证据。本轮证明的是当前工作树和运行服务仍能读取、校验并追溯该发布结果。
- `BpmApiIsolationTest`、`BpmSchemaSourceTest` 均在本轮全量门禁内通过；新 Graph 前端与后端源码搜索未发现已删除的旧树形设计器适配器、双写桥接或旧路由编辑器引用。`git diff --check` 退出码 0，输出中的 CRLF 提示不属于 whitespace error。
