# BPM M4 时间、SLA 与事件驱动验收记录

- 日期：2026-07-11
- 状态：历史时间事件实现证据；仓库门禁、数据库核对、真实启动、管理 API、Flowable 计时器和 Chrome 页面已通过，外部回调、自动终态竞态及重启恢复未关闭；不代表当前 M5 已完成
- 实施计划：`docs/superpowers/plans/2026-07-11-bpm-m4-time-event-runtime-implementation.md`

## 1. 当前能力事实

- 流程模型升级到 schema v3，并继续兼容 v1/v2；新增任务 SLA、`DELAY` 和 `EXTERNAL_TRIGGER`。
- 发布校验拒绝非法时间、任意 URL、凭据、EL、未登记连接器及不符合风险约束的自动终态。
- 编译器生成 SLA boundary timer、延迟 catch event、登记连接器 delegate、receive task 和外部等待超时路径。
- `t_bpm_time_event`、`t_bpm_external_wait`、`t_bpm_connector_definition` 保存时间、等待和连接器事实。
- 时间事件和外部等待使用稳定业务键、状态锁和终态幂等；实例取消会清理待处理事实。
- 连接器只保存端点和凭据引用，出站策略拒绝回环、链路本地和元数据地址；重试只允许幂等操作。
- 外部回调不依赖员工会话，但继续强制 token 摘要、HMAC、相关键和单次恢复。
- 管理端提供时间事件、外部等待和连接器目录查询及处置页面，实例 trace 返回脱敏 M4 事实。
- dev/pre/prod 启用 Flowable async executor，测试环境保持关闭。

## 2. 本次自动化证据

| 门禁 | 本次结果 |
| --- | --- |
| `mvn -pl hunyuan-bpm test` | 285 个测试，0 失败，0 错误 |
| `BpmFlowableCompatibilityTest` | 5 个测试，0 失败，0 错误 |
| v3 Flowable 部署 | SLA、延迟和外部等待编译产物成功部署 |
| v3 真实计时任务 | 启动 `PT1H` 延迟实例后 `createTimerJobQuery` 返回 1 |
| 设计器适配器 Vitest | 19 个测试通过 |
| BPM 页面模块 Vitest | 39 个测试通过 |
| `@hunyuan/system` 类型检查 | 退出码 0 |
| `git diff --check` | 无空白错误，仅 Windows 行尾提示 |

本次还补充了两个运行态发现的回归门禁：时间事件协调器改为触发时延迟解析，避免 Spring 启动依赖环；外部回调方法使用项目原生 `@NoNeedLogin` 匿名契约，避免被员工登录拦截器提前拒绝。

## 3. 数据库证据

- 用户已手动执行 `数据库SQL脚本/mysql/sql-update-log/v3.49.0.sql`；本次在开发库核对迁移结果。
- 脚本三张表均使用 `CREATE TABLE IF NOT EXISTS`，字典、菜单和角色授权使用可重复执行写法。
- 数据库实际存在 3 张 M4 表、8 个新字典类型、38 个 M4 字典项、8 个菜单和 8 条管理员角色授权。
- 三张表排序规则均为 `utf8mb4_general_ci`；中文字典和菜单十六进制为标准 UTF-8 字节。
- 重复执行后的对象数量保持 `3 / 8 / 38 / 8 / 8`，未产生重复数据。

## 4. 本次运行与 Chrome 证据

- 当前分支 JAR 重新打包并启动，后端 PID `12880` 监听 `1024`。
- 启动日志记录 `ProcessEngine hunyuan-bpm created`、Flowable async job executor 创建和 `Started AdminApplication`，`/login/getCaptcha` 返回 HTTP 200。
- 管理 API 实际登录后查询 `/bpm/time-event/query`、`/bpm/external-wait/query`、`/bpm/connector/query`，三者均返回成功，当前数据总数均为 0。
- 无员工会话的伪造回调进入业务安全边界并以 `回调签名校验失败` 拒绝，不再返回登录失效。
- Chrome 使用 `admin / 123456` 登录 `hunyuan-system`，验证时间事件、外部等待和连接器目录三条真实路由。
- 时间事件页面显示实例、事件类型、状态筛选及事件类型、计划/触发时间、触发次数和处置列。
- 外部等待页面显示相关键、连接器、操作、实例、节点、状态、超时时间和处置列。
- 连接器目录显示编码、版本、名称、端点引用、超时、状态及新增连接器操作。
- 样板费用设计器的新增节点下拉实际显示“延迟节点”和“外部触发节点”，审批节点属性实际显示“任务 SLA”开关。
- 浏览器验收没有保存或发布模型；一次误触产生的未保存 `branch_4` 已通过整页重载丢弃，最终后端模型状态仍为“与已发布版本一致”。

## 5. 尚未关闭的验收项

- 尚未启动受控 HTTPS 模拟连接器跑通 NO_WAIT 实际请求。
- 尚未通过真实 HTTP 回调跑通 WAIT_CALLBACK 成功、重复回调和伪造回调组合流；当前成功/重复语义由服务测试覆盖，活体只验证了伪造签名拒绝。
- 尚未在开发库跑通 SLA 提醒、自动通过/拒绝/转管理员与人工动作竞态；当前由命令协调和时间事件测试覆盖。
- 尚未完成外部等待超时、实例取消后的真实 Flowable 恢复，以及后端重启后的等待恢复。
- 当前库没有 M4 运行事实，因此 Chrome 实例详情只完成组件契约测试，未展示真实时间事件或外部等待记录。

## 6. 当前结论

M4 的模型、编译、持久化、安全边界、运行服务、管理 API 和前端能力已经落地，数据库和真实引擎可以启动并创建 Flowable timer job。由于第 5 节的跨系统活体矩阵尚未全部执行，本记录不把 M4 标记为关闭；下一次继续时应从受控模拟连接器和真实回调恢复开始，不重新设计已确认范围。
