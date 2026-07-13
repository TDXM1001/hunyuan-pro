# BPM M5 高级流程运行验收记录

- 日期：2026-07-13
- 分支：`main`
- 状态：`RELEASABLE`；M5 实现、迁移、真实引擎、重启恢复和浏览器契约已验收，模块关闭
- 设计：`docs/superpowers/specs/2026-07-11-bpm-module-05-advanced-runtime-design.md`
- 前置模块：M1-M4 均已关闭

## 1. 当前能力事实

- 正式 Graph 已登记 `DELAY`、`EXTERNAL_TRIGGER` 和 `SUB_PROCESS`，发布时冻结时间模式、连接器版本、超时策略、子定义版本、映射和传播策略。
- 延迟和外部等待超时编译为 Flowable 持久 timer；`FORM_DATETIME` 在节点进入时解析表单日期并写入对应 timer 变量，服务重启后由数据库恢复。
- 外部调用通过持久命令记录保存稳定幂等键、响应、尝试次数和下次重试时间。语义为 at-least-once，接收端必须遵守稳定 `idempotencyKey`，不得宣称物理 exactly-once。
- 回调同时验证 token、应用、相关键、等待版本和独立凭据 HMAC；签名串为 `appKey + "\n" + correlationKey + "\n" + waitVersion + "\n" + payload`，token 不作为 HMAC 密钥。
- 子流程使用冻结 Flowable 定义 ID 启动 callActivity 和独立 Hunyuan 子实例，保存父子关系、输入输出、失败策略和取消传播。技术失败由 dead-letter job 恢复调度器进入冻结策略；父取消会同步关闭活动子 Flowable/Hunyuan 实例。
- 员工 trace 清空时间策略、外部请求/回调快照、子流程输入输出、引擎 ID、失败策略和取消传播；前端员工详情不渲染“失败策略”列，admin 详情保留运营字段。

## 2. 自动化与真实引擎证据

| 门禁 | 本次结果 |
| --- | --- |
| `mvn -pl hunyuan-bpm -am test` | 493 个测试，0 失败，0 错误 |
| `BpmFlowableCompatibilityTest` | 14 个测试，0 失败，0 错误 |
| 前端 `vitest` | 32 个测试文件、147 个测试通过 |
| `pnpm --filter @hunyuan/system typecheck` | 退出码 0 |

Flowable 兼容测试实际证明：Graph 延迟产生真实 timer；表单日期生成真实 timer；外部节点停在 receive task 并产生边界 timer；回调或超时只推进一次；父流程按冻结定义 ID 启动真实 callActivity 子实例，等待子任务并随子流程完成。

新增独立 MySQL 重启测试在临时库中创建活动 timer，关闭第一个 Flowable engine 后重建第二个 engine，并以同一实例 ID 查询到原 timer。测试显式使用 MySQL 驱动、当前 catalog，并关闭生产未启用的 IDM/Event Registry 附属引擎。

## 3. 数据库迁移与恢复

- `数据库SQL脚本/mysql/sql-update-log/v3.57.0.sql` 已连续执行两次，首次和重复执行均成功。
- `t_bpm_command_record` 已具备 `response_payload_json`、`attempt_count`、`next_retry_at`；`t_bpm_time_event`、`t_bpm_external_wait` 已增加 Graph 版本引用；`t_bpm_sub_process_link`、索引和字典项存在。
- 执行前提：关闭写流量并备份 BPM 运行表。只有确认不存在 M5 Graph 实例时才能删除新增索引、列、关系表和字典项；已发生的外部副作用不能通过数据库回滚撤销。
- 当前源码 jar 完成 Spring Boot repackage 后冷启动两次。两次 `/login/getCaptcha` 均返回 `ok=true`；第二次日志显示 `Started AdminApplication`，子流程失败恢复调度器真实查询 `ACT_RU_DEADLETTER_JOB`，无 Bean、schema 或新增列错误。

## 4. 竞态、安全与传播证据

- 回调与 timeout 通过条件更新竞争同一 `WAITING` 事实；输掉竞争的一方不能再次触发 execution。
- 错误 app、correlation key 或 wait version 在 claim 前被拒绝；伪造签名被拒绝；重复 claim 不调用 Flowable；回调 payload 上限为 1 MiB。
- 持久连接器命令成功后复用保存响应，失败后保存尝试次数和退避时间；Flowable delegate 使用稳定命令键 `M5:CONNECTOR:<instanceId>:<nodeKey>:<executionId>`。
- 子流程关系按 `WAITING` 条件认领；完成、拒绝、技术失败、人工处置和取消不能覆盖已形成的终态。`REJECT_PARENT` 取消父 Flowable 实例并写父 Hunyuan 拒绝终态；`CANCEL_CHILD` 同步关闭活动子实例。
- SLA 事件复用 M4 任务版本和统一系统动作边界，人工动作与自动终态只形成一个有效任务结果。

## 5. 浏览器验收

- Graph 设计器可创建延迟、外部调用和子流程节点，属性面板显示时间、连接器、冻结子版本和传播配置；缺少引用或冻结版本时模拟诊断阻断发布。
- “时间事件”和“外部等待”运维页可加载筛选、事实表格与处置入口；实例详情显示时间事件、外部等待和子流程分区。
- 1440x900 与 390x844 下设计器头部、创建区、画布和属性区不重叠，工具栏与画布在各自容器内滚动。
- 本次安全修复后重新打开 runtime“我的申请”页面并确认员工详情入口可用；当前库 `t_bpm_sub_process_link` 无业务记录，因此未伪造子流程页面数据。员工字段裁剪由 `BpmInstanceTraceServiceTest` 和新增前端组件契约测试证明；真实父子推进由第 2 节 Flowable callActivity 测试证明。

## 6. 独立审查缺口关闭

先前独立审查的五项 P1 已关闭：

1. `FORM_DATETIME` 会写入 authored delay 变量并创建真实 Flowable timer。
2. 外部副作用进入 `REQUIRES_NEW` 持久命令边界并暴露稳定幂等键。
3. 回调使用独立连接器凭据，并校验应用、相关键和等待版本。
4. Flowable dead-letter job 调度器把子流程技术失败交给冻结传播策略。
5. 父取消同步关闭独立 Hunyuan 子实例。

P2 同时关闭：员工 trace 与页面不暴露内部子流程策略/引擎 ID；活动 timer 已在真实 MySQL engine 关闭和重建后恢复。

## 7. 最终结论

M5 六项完成定义均有当次自动化、真实 MySQL/Flowable、双冷启动或浏览器契约证据，状态为 `RELEASABLE`，模块关闭。M6 可以基于已经稳定的 timer、等待、持久命令和子流程事实继续配置化业务接入；外部接收端幂等契约与补偿仍分别属于 M6/M7 边界。
