# BPM M1 流程建模与编译平台验收记录

- 日期：2026-07-11
- 状态：已关闭；实现、自动化门禁、数据库迁移、API 多路径实流和 Chrome 验收均通过
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
