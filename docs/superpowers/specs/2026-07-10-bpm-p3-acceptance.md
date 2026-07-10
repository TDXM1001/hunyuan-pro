# BPM Enterprise P3 总验收

日期：2026-07-10

## 当前状态

P3.1 至 P3.4 功能范围、活体验收、全量门禁与正式 `1024` 后端恢复均已完成。P3 保持 Hunyuan 原生边界，没有引入 Flowable 多实例语义。

## 已完成范围

- P3.1：六类候选策略的基础建模，以及 `EMPLOYEE_SELECT_AT_START` 的真实发起、待办归属、审批和 trace 活体验收。
- P3.2：发起时自选审批人员工存在性校验；发起与重提共用最新表单解析路径；发布时校验员工字段与表单 schema 一致。
- P3.3：逐节点候选策略预检，统一 `READY`、`RUNTIME_REQUIRED`、`BLOCKING` 语义；角色无成员、部门无主管等确定性错误阻断发布。
- P3.3：模拟发起人与模拟表单员工都通过组织身份网关执行员工有效性校验，禁用、删除或不存在的员工以 `EMPLOYEE_NOT_FOUND` 阻断。
- P3.3：`validateForPublish.pass` 与真实发布门禁统一，覆盖模型、发起规则、分类、表单、字段一致性和候选预检。
- P3.3：发布只读取一次模型快照；编译后、部署前按 `model_id + update_time` 条件认领快照，模型发生并发变更时拒绝部署并提示刷新后重新发布。
- P3.4：显式员工列表的顺序多人审批，编译为多个有序单处理人任务，并保留 Hunyuan 单任务运行时边界。
- P3.4：所有编译节点 key 必须符合 `[A-Za-z_][A-Za-z0-9_]*`，避免中划线破坏 `assignee_<nodeKey>` JUEL 变量表达式，并执行全局冲突与 128 字符长度校验；待办和任务详情公共投影补齐 `taskKey`。
- P3.5：设计器发布前同时比较设计器内部脏状态与六个运行规则 JSON 草稿快照，避免运行规则修改后未保存就直接发布。
- 前端运行时表单规则递归处理补齐显式 `FormRule[]` 返回类型，嵌套 `children`、`fields` 统一经过类型收敛。

## 活体证据

P3.1 发起时自选审批人：

- 实例：`instanceId=55`，`instanceNo=DK20260710NO01008`
- 任务：`taskId=58`
- 被选择审批人：`huke / 胡克 / employeeId=2`
- 证据：`G:\code-mcp\playwright-mcp-temp\runtime\hunyuan-p3-candidate-live-acceptance-20260710-095549.json`

P3.4 顺序多人审批：

- 模型：`modelId=4`
- 定义：`definitionId=6`
- 实例：`instanceId=56`，`instanceNo=DK20260710NO01015`
- 第一任务：`taskId=59`，`taskKey=task_finance_1`，管理员审批
- 第二任务：`taskId=60`，`taskKey=task_finance_2`，胡克审批
- 最终状态：`runState=3`，`resultState=1`
- 证据：`G:\code-mcp\playwright-mcp-temp\runtime\hunyuan-p3-sequential-live-acceptance-20260710-120102.json`

## 实现阶段聚焦验证

后端：

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmCandidatePrecheckServiceTest,BpmDefinitionGovernanceServiceTest,SimpleModelValidatorTest,SimpleModelBpmnCompilerTest,BpmTaskAssignmentResolverTest,BpmRuntimeStartAssignmentTest,BpmTaskListProjectionContractTest' test
```

结果：`Tests run: 53, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

前端：

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

结果：`Test Files 3 passed (3)`，`Tests 47 passed (47)`。

## 最终门禁

后端 BPM 全量测试：

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test
```

结果：`Tests run: 171, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

Admin Flowable 兼容性测试：

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin '-Dtest=BpmFlowableCompatibilityTest' test
```

结果：`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

前端四文件契约测试：

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

结果：`Test Files 4 passed (4)`，`Tests 51 passed (51)`。

前端类型检查：

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

结果：通过，无 TypeScript 错误。

差异与编码检查：

```powershell
git diff --check
```

结果：

- `git diff --check` 通过。
- 当前 46 个变更路径均通过严格 UTF-8 解码检查。
- Maven 仍会报告用户级 `settings.xml` 第 235 行格式警告，与 P3 代码和测试结果无关。

## 打包与服务恢复

Reactor 打包：

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin -am -DskipTests package
```

结果：`BUILD SUCCESS`。

- 正式后端使用 `hunyuan-backend/hunyuan-admin/target/hunyuan-admin-dev-3.0.0.jar` 与 `dev` profile 运行。
- `1024` 端口进程：`PID 16772`。
- `http://127.0.0.1:1024/login/getCaptcha` 返回 HTTP `200`。
- 临时 `1025` 后端已关闭。
- 前端 `5788` 保持运行，进程为 `PID 32788`。
- 后端日志位于 `G:\code-mcp\playwright-mcp-temp\runtime\hunyuan-p3-backend-1024-20260710-145154.out.log` 及对应 `.err.log`。

## P4 边界

- 并行会签。
- 或签。
- 比例审批。
- Flowable multi-instance。
- 网关与条件分支。
- 子流程。
- 表达式或脚本审批人。
- 岗位、用户组、动态部门成员。
- 表单字段权限系统。
- 面向公共前端契约的结构化 authored 顺序审批组、组级进度和聚合展示。

## 交付约束

- 未新增依赖。
- 未新增 SQL。
- 未将 Flowable 原生对象、原生 ID 或多实例语义暴露给外部调用方。
- 运行证据只保存在 `G:\code-mcp\playwright-mcp-temp\runtime`，不进入仓库。
- 本轮不执行 stage、commit 或 push。
