# BPM 流程定义 current 版本唯一性验收记录

## 结论

BPM 流程定义发布的不变量修复通过验收。

本轮修复 P2 收官活体验收留下的非阻塞问题：同一个 `definitionKey` 在发布新版本后，旧版本可能仍保持 `lifecycleState = 1`。修复后，发布服务会在新定义插入并拿到 `definitionId` 后，将同一 `definitionKey` 下除新定义外仍为 current 的记录一次性降级为 historical。

## 修复范围

- `BpmDefinitionService#publish` 保持新版本插入、节点快照、模型回写的既有流程。
- 新增发布事务内的表级不变量收敛：同 key、`lifecycle_state = CURRENT`、且 `definition_id != 新定义ID` 的记录统一更新为 `HISTORICAL`。
- `BpmDefinitionPublishServiceTest` 改为断言发布后会执行 bulk historicalize，并确认该操作发生在新定义插入之后。

## 验收结果

| 验收项 | 命令 | 结果 |
| --- | --- | --- |
| RED 验证 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmDefinitionPublishServiceTest' test` | 初次失败符合预期：未调用 `bpmDefinitionDao.update(entity, wrapper)` |
| 聚焦回归 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmDefinitionPublishServiceTest' test` | PASS；2 tests，0 failures，0 errors |
| BPM 模块门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test` | PASS；109 tests，0 failures，0 errors |
| Flowable 边界门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin '-Dtest=BpmFlowableCompatibilityTest' test` | PASS；1 test，0 failures，0 errors |
| 当前代码打包 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin -am -DskipTests package` | PASS；`hunyuan-admin-dev-3.0.0.jar` 基于当前工作区重新生成 |
| 运行态 API 证明 | 重启当前 jar 后，登录管理员、发布 `sample_expense_apply` 模型、查询定义列表和可发起列表 | PASS；发布前本地库有 `definitionId = 2/3` 两个 current；发布 `definitionId = 4` 后仅 `definitionId = 4` 为 current，`definitionId = 2/3` 均为 historical；可发起列表只返回 `definitionId = 4` |

## 运行态证据

- 后端进程：PID `3460`
- 当前 jar 启动日志：`G:\code-mcp\playwright-mcp-temp\runtime\hunyuan-admin-p2-definition-invariant-20260709-225131.out.log`
- 运行态证明文件：`G:\code-mcp\playwright-mcp-temp\runtime\hunyuan-p2-definition-version-invariant-20260709-225319.json`
- 证明路径：
  1. `POST /login` 以管理员登录，`employeeId = 1`。
  2. `POST /bpm/model/query` 查询 `modelKey = sample_expense_apply`，得到 `modelId = 2`。
  3. `POST /bpm/definition/query` 记录发布前定义状态：`definitionId = 3` 和 `definitionId = 2` 均为 `lifecycleState = 1`。
  4. `POST /bpm/definition/publish` 发布模型，得到 `newDefinitionId = 4`。
  5. 再次查询定义列表，确认 `definitionId = 4` 为唯一 current，`definitionId = 2/3` 为 historical。
  6. `GET /app/bpm/startable` 确认可发起列表只返回 `definitionId = 4`。

## 边界说明

- 本轮只修流程定义版本 current 唯一性，不新增样板页面、不扩展业务模块、不改变 P2.3 回调执行器或 P2.4 样板费用申请契约。
- 修复使用 MyBatis-Plus 既有 `update(entity, wrapper)` 能力，没有新增依赖。
- 该修复能覆盖正常发布路径，也能在下一次发布同 key 定义时收敛历史遗留的多个 current 记录。
- Maven 仍报告本机 `F:\maven\apache-maven-3.9.11\conf\settings.xml` 第 235 行附近存在非预期文本；相关门禁均在该既有 warning 下通过。
- `BpmFlowableCompatibilityTest` 编译阶段仍有 `MockBean` 过时 warning；该 warning 属于既有测试技术债，本轮未处理。
