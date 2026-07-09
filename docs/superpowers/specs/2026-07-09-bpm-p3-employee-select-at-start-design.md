# BPM P3.1b 发起时自选审批人设计

## 结论

本切片采用 **最小启动上下文扩展**，新增候选人解析类型：

- `EMPLOYEE_SELECT_AT_START`：审批人从流程发起表单 `formDataJson` 的指定字段读取。

该策略补齐 P3.1a 预留的下一步，但不扩大成完整候选人平台。本轮只支持单个员工 ID，不做多人审批、用户组、岗位、表达式、表单字段权限或多级主管。

## 当前证据

- P3.1a 已支持 `START_EMPLOYEE` 和 `START_DEPARTMENT_MANAGER`，并打通后端枚举、校验、运行解析和前端设计器选项。
- 当前 `BpmTaskAssignmentResolver` 的旧入口是 `resolve(definitionNodes, startEmployeeSnapshot)`，无法读取启动表单数据。
- 当前启动链路已经携带 `formDataJson`：
  - 员工发起时来自 `BpmInstanceStartForm.formDataJson`。
  - 业务发起时来自 `BpmBusinessStartCommand.formDataJson`。
  - 重提时来自 `BpmInstanceResubmitForm.formDataJson`。
  - Flowable gateway 已把 `formDataJson` 和解析后的 `formData` 放进流程变量。
- 因此本轮不需要新增数据库字段，只需要在 Hunyuan 启动前的候选解析阶段读取同一份 `formDataJson`。

## 范围

### 本轮包含

- 增加 `EMPLOYEE_SELECT_AT_START` 候选人解析类型。
- 新增一个小上下文对象，例如 `BpmTaskAssignmentContext`，携带：
  - `BpmEmployeeSnapshot startEmployeeSnapshot`
  - `String formDataJson`
- 新增 `BpmTaskAssignmentResolver.resolve(definitionNodes, context)`。
- 保留旧方法 `resolve(definitionNodes, startEmployeeSnapshot)`，并委托到新方法，避免影响旧调用方和既有测试。
- 在启动和重提链路中传入 `formDataJson`。
- 从节点配置读取员工字段 key，并从启动表单数据中解析单个员工 ID。
- 前端 BPM 设计器增加候选类型选项和字段 key 输入。
- 后端和前端测试覆盖校验、解析、启动变量和设计器快照。

### 本轮不包含

- 不支持多个审批人。
- 不支持会签或或签。
- 不支持用户组、岗位、表达式、部门成员、多级主管。
- 不新增菜单、权限或 SQL。
- 不新增依赖。
- 不暴露 Flowable 原生对象或原生 ID。
- 不迁移 Yudao/RuoYi 的枚举、接口、页面壳或模块边界。

## 节点配置契约

`EMPLOYEE_SELECT_AT_START` 节点必须配置一个字段 key。推荐字段名：

```json
{
  "candidateResolverType": "EMPLOYEE_SELECT_AT_START",
  "employeeSelectFieldKey": "approverEmployeeId"
}
```

兼容读取字段名：

- `employeeSelectFieldKey`
- `candidateFieldKey`
- `assigneeFieldKey`

优先使用 `employeeSelectFieldKey`，其余字段只用于兼容或测试输入。

## 解析规则

1. 发布后的节点快照进入 `BpmTaskAssignmentResolver`。
2. 解析器识别 `candidateResolverType = EMPLOYEE_SELECT_AT_START`。
3. 从节点配置读取员工字段 key。
4. 解析 `BpmTaskAssignmentContext.formDataJson` 为 JSON 对象。
5. 从表单数据读取字段值。
6. 字段值必须能解析为单个 Long 员工 ID。
7. 写入 Flowable 启动变量：

```text
assignee_<nodeKey> = "<employeeId>"
```

## 错误语义

使用明确的用户错误，继续由 `BpmInstanceService` 捕获 `IllegalArgumentException` 并返回 `ResponseDTO.userErrorParam`。

- 缺少字段 key：`审批节点【<节点名>】未配置发起时自选审批人字段`
- 表单数据为空或非法：`审批节点【<节点名>】未找到发起时自选审批人`
- 字段不存在或值为空：`审批节点【<节点名>】未找到发起时自选审批人`
- 字段值不是单个员工 ID：`审批节点【<节点名>】发起时自选审批人无效`

本轮只校验字段值形态，不额外校验该员工是否存在。员工存在性仍交给 Flowable 后续任务投影和组织快照链路逐步收敛，避免把本切片扩大成完整发起表单校验框架。

## 后端设计

### 枚举

文件：

- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmCandidateResolverTypeEnum.java`

新增：

- `EMPLOYEE_SELECT_AT_START("EMPLOYEE_SELECT_AT_START", "发起时自选审批人")`

### 上下文

新增文件建议：

- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskAssignmentContext.java`

建议实现为 Java record：

```java
public record BpmTaskAssignmentContext(
        BpmEmployeeSnapshot startEmployeeSnapshot,
        String formDataJson
) {
}
```

### 解析器

文件：

- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskAssignmentResolver.java`

调整点：

- 新增 `resolve(List<BpmDefinitionNodeEntity>, BpmTaskAssignmentContext)`。
- 旧 `resolve(List<BpmDefinitionNodeEntity>, BpmEmployeeSnapshot)` 委托为：

```java
return resolve(definitionNodes, new BpmTaskAssignmentContext(startEmployeeSnapshot, "{}"));
```

- `START_EMPLOYEE`、`START_DEPARTMENT_MANAGER`、`DEPARTMENT_MANAGER` 继续从 `context.startEmployeeSnapshot()` 读取。
- `EMPLOYEE_SELECT_AT_START` 从 `context.formDataJson()` 读取。
- 支持数值型和字符串型员工 ID。
- 不支持数组值；遇到数组或逗号字符串时返回无效，避免偷偷退化成“取第一个人”的多人审批假象。

### 启动链路

文件：

- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java`

调整点：

- `startInstanceWithDefinition` 中解析候选人时传入 `startForm.getFormDataJson()`。
- `resubmitMyInstance` 中解析候选人时传入 `resubmitForm.getFormDataJson()`。
- `startBusinessInstance` 已把业务命令转换成 `BpmInstanceStartForm`，无需单独分支。

### 校验器

文件：

- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidator.java`

调整点：

- 枚举加入后，新类型自然进入支持范围。
- 对 `EMPLOYEE_SELECT_AT_START` 增加字段 key 校验，避免发布一个启动时必失败的流程定义。
- 错误文案包含新类型，保持真实范围。

## 前端设计

### 类型

文件：

- `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/types.ts`

调整：

- `candidateResolverType` union 增加 `EMPLOYEE_SELECT_AT_START`。
- `BpmProcessNodeDraft` 增加可选字段：

```ts
employeeSelectFieldKey?: string;
```

### 设计器

文件：

- `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-process-designer-adapter.vue`

调整：

- 候选类型下拉新增 `发起时自选审批人`。
- 当候选类型为 `EMPLOYEE_SELECT_AT_START` 时显示一个字段 key 输入框。
- 字段 key 输入框不做表单字段选择器，本轮只输入文本 key，避免把设计器扩展成完整表单字段联动。
- 默认新节点仍使用 `EMPLOYEE`，不改变已有流程草稿行为。

## 数据流

1. 管理员在流程设计器节点上选择 `发起时自选审批人`。
2. 管理员填写字段 key，例如 `approverEmployeeId`。
3. 前端保存 simple model 节点快照。
4. 发布时 `SimpleModelValidator` 校验候选类型和字段 key。
5. 员工发起或业务发起时提交 `formDataJson`。
6. `BpmInstanceService` 构造 `BpmTaskAssignmentContext`。
7. `BpmTaskAssignmentResolver` 从 `formDataJson` 读取员工 ID。
8. 解析结果写入 `assignee_<nodeKey>`。
9. Flowable 创建真实待办。
10. Hunyuan 任务投影和实例详情继续沿用现有运行链路。

## 测试策略

### 后端 RED 测试

扩展：

- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskAssignmentResolverTest.java`

覆盖：

- `EMPLOYEE_SELECT_AT_START` 从数字字段解析员工 ID。
- `EMPLOYEE_SELECT_AT_START` 从字符串字段解析员工 ID。
- 缺少字段 key 报错。
- 缺少表单字段报错。
- 数组或逗号字符串报无效，不取第一个。

扩展：

- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeStartAssignmentTest.java`

覆盖：

- 启动流程时，`formDataJson` 中选择的员工 ID 进入 `FlowableProcessInstanceGateway.start` 的变量 map。
- 重提流程时同样使用新的 `formDataJson` 重新解析。

扩展：

- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidatorTest.java`

覆盖：

- 新类型配置字段 key 时校验通过。
- 新类型缺少字段 key 时校验失败。

### 前端 RED 测试

扩展：

- `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts`

覆盖：

- `EMPLOYEE_SELECT_AT_START` 可以加载和保存。
- `employeeSelectFieldKey` 保留在快照中。
- 字段 key 输入不会影响现有 `EMPLOYEE`、`ROLE`、`START_EMPLOYEE` 等节点。

## 验证命令

后端聚焦：

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAssignmentResolverTest,BpmRuntimeStartAssignmentTest,SimpleModelValidatorTest' test
```

前端聚焦：

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts
```

前端 BPM 合同：

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

类型检查：

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

如需要活体验收，再准备一个单节点 `EMPLOYEE_SELECT_AT_START` 流程，发起表单传入真实员工 ID，确认待办处理人来自启动表单。运行证据写入 `G:/code-mcp/playwright-mcp-temp/runtime`，不提交仓库。

## 完成定义

- 后端枚举、校验器、解析器和启动链路都支持 `EMPLOYEE_SELECT_AT_START`。
- 前端设计器能选择并保存该策略及字段 key。
- 启动和重提都使用当前提交的 `formDataJson` 解析审批人。
- 数组和逗号字符串不被误当作单人审批。
- 不涉及 SQL 变更。
- 不新增依赖。
- 后端聚焦测试和前端聚焦测试通过。

## 人工审阅重点

- 是否接受字段名固定为 `employeeSelectFieldKey`，同时兼容读取 `candidateFieldKey` 和 `assigneeFieldKey`。
- 是否接受本轮只做文本字段 key 输入，不做表单 schema 字段选择器。
- 是否接受字段值只支持单个员工 ID，不支持数组或多人。
- 是否接受本轮不校验员工存在性，只保证解析出单个员工 ID。
