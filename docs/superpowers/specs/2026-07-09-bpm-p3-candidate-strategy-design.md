# BPM P3.1a 发起人候选策略扩展设计

## 结论

本切片采用 **真实运行链路上的最小候选策略扩展**。

P3.1a 只新增两类发起人相关策略：

- `START_EMPLOYEE`：审批人解析为流程发起人本人。
- `START_DEPARTMENT_MANAGER`：审批人解析为流程发起人所在部门主管。

这两个策略直接服务当前 Hunyuan BPM 的真实启动路径：流程发布后的节点快照在发起实例时由 `BpmTaskAssignmentResolver` 解析为 `assignee_<nodeKey>` 变量，再交给 Flowable 内核启动。实现后不是前端下拉多两个假选项，而是后端发布校验、运行解析、Flowable 启动变量和前端设计器快照全部一致。

`EMPLOYEE_SELECT_AT_START` 暂缓到 P3.1b。它需要启动表单数据或启动上下文进入候选解析器，而当前解析器签名只有 `resolve(definitionNodes, startEmployeeSnapshot)`。本轮不为了追功能目录而仓促扩大启动契约。

## 当前证据

当前仓库已经具备三类候选人解析：

- `BpmCandidateResolverTypeEnum` 只有 `EMPLOYEE`、`DEPARTMENT_MANAGER`、`ROLE`。
- `BpmTaskAssignmentResolver` 在实例启动前把发布后的 `userTask` 节点解析成 Flowable 变量。
- `SimpleModelValidator` 当前只允许三类候选人解析类型，错误文案仍是 “P0 只支持 EMPLOYEE、DEPARTMENT_MANAGER、ROLE 三类候选人解析类型”。
- 前端 `BpmProcessNodeDraft.candidateResolverType` 类型也只包含这三类。
- 设计器下拉只展示 “指定员工 / 部门负责人 / 角色”。
- 当前解析器已经拥有发起人快照 `BpmEmployeeSnapshot`，其中包括发起人员工 ID 和部门 ID。
- 当前组织网关已经提供 `resolveDepartmentManagerEmployeeId(departmentId)`。

参考后端里存在发起人本人、发起人部门负责人、发起时自选审批人等更丰富策略。本切片只借鉴机制，不迁移参考项目枚举、接口、VO、路由或模块边界。

## 强约束

### 真实运行

本切片必须打通真实链路：

1. 前端设计器能保存新候选类型。
2. 后端发布校验能接受新候选类型。
3. 运行时解析器能把新候选类型解析成真实员工 ID。
4. 实例启动时能把解析结果传给 Flowable 启动变量。

不接受只改前端选项、不接后端解析的半成品；也不接受绕过 Hunyuan 发布链路直接改 Flowable 表。

### UTF-8

所有新增或修改的中文文档、测试名、错误文案、SQL 注释和前端文案必须保持 UTF-8。

实施阶段在 PowerShell 中读取中文文件时优先使用：

```powershell
Get-Content -Encoding UTF8 -Raw <path>
```

如需要批量写中文文件，必须确认写入后无乱码。

### SQL 增量

P3.1a 预计不需要 SQL，因为新增的是代码枚举、校验、解析和前端设计器选项，不新增菜单、权限、表结构或初始化数据。

如果实现阶段发现必须补 SQL，只能新增增量脚本：

- 当前已确认的最新 BPM 相关脚本到 `数据库SQL脚本/mysql/sql-update-log/v3.42.0.sql`。
- 若当时仍无更新脚本，则新增 `数据库SQL脚本/mysql/sql-update-log/v3.43.0.sql`。
- 如果实施前已有更高版本脚本，则追加下一个版本号。
- 不修改 `数据库SQL脚本/mysql/hunyuan.sql`。
- 不回改历史 `v3.xx.0.sql`。
- 菜单、权限、角色绑定、样板数据都必须可追溯到增量 SQL，不能依赖手工数据库状态。

## 范围

### 本轮包含

- 后端候选人解析枚举增加 `START_EMPLOYEE` 和 `START_DEPARTMENT_MANAGER`。
- 后端 SimpleModel 校验接受这两个新类型，并更新错误文案。
- 后端运行解析器支持：
  - 发起人本人。
  - 发起人部门主管。
- 前端 BPM 设计器类型定义增加新候选类型。
- 前端 BPM 设计器节点属性下拉增加两个选项。
- 前端 simple model 桥接保持新类型可保存、加载、生成快照。
- 后端和前端测试覆盖新策略。

### 本轮不包含

- 不实现 `EMPLOYEE_SELECT_AT_START`。
- 不引入岗位、用户组、表单字段审批人、表达式审批人。
- 不实现多级主管、部门成员、角色多人会签。
- 不新增 BPM 页面、菜单或权限。
- 不新增依赖。
- 不暴露 Flowable 原生对象。
- 不迁移 Yudao/RuoYi 的枚举名、接口或前端页面壳。

## 策略语义

### START_EMPLOYEE

含义：审批节点由流程发起人本人处理。

解析规则：

- 从 `BpmEmployeeSnapshot.employeeId()` 读取员工 ID。
- 写入 Flowable 变量：`assignee_<nodeKey> = "<employeeId>"`。
- 如果缺少发起人快照或员工 ID，返回明确错误。

建议错误文案：

- `审批节点【<节点名>】未找到发起人`

### START_DEPARTMENT_MANAGER

含义：审批节点由流程发起人所在部门主管处理。

解析规则：

- 从 `BpmEmployeeSnapshot.departmentId()` 读取发起人部门 ID。
- 调用 `BpmOrgIdentityGateway.resolveDepartmentManagerEmployeeId(departmentId)`。
- 写入 Flowable 变量：`assignee_<nodeKey> = "<managerEmployeeId>"`。
- 如果缺少发起人、缺少发起人部门或没有部门主管，返回明确错误。

建议错误文案：

- `审批节点【<节点名>】未找到发起人部门`
- `审批节点【<节点名>】未找到发起人部门主管`

### 和现有 DEPARTMENT_MANAGER 的区别

现有 `DEPARTMENT_MANAGER` 的语义偏“节点配置部门主管”，并且当前实现会在节点没有 `departmentId` 时回退到发起人部门。

新增 `START_DEPARTMENT_MANAGER` 后，语义应更清楚：

- `DEPARTMENT_MANAGER`：优先按节点配置的部门找主管；没有配置时保留当前兼容行为。
- `START_DEPARTMENT_MANAGER`：明确按发起人所在部门找主管。

本轮不破坏 `DEPARTMENT_MANAGER` 的兼容逻辑，避免影响已发布流程。

## 后端设计

### 枚举

文件：

- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmCandidateResolverTypeEnum.java`

新增：

- `START_EMPLOYEE("START_EMPLOYEE", "发起人本人")`
- `START_DEPARTMENT_MANAGER("START_DEPARTMENT_MANAGER", "发起人部门主管")`

### 校验器

文件：

- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidator.java`

调整点：

- `isSupportedResolverType` 已经基于枚举遍历，新枚举加入后校验自然通过。
- 错误文案更新为包含新类型的真实范围，避免用户看到过时的 “三类候选人”。

### 解析器

文件：

- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskAssignmentResolver.java`

调整点：

- 在 `resolveNodeAssignee` 中增加 `START_EMPLOYEE` 分支。
- 增加 `START_DEPARTMENT_MANAGER` 分支。
- 保持 `resolve` 方法签名不变，避免把 P3.1a 扩大成启动上下文重构。
- 继续返回字符串形式的 Flowable assignee 变量，保持现有变量契约。

## 前端设计

### 类型

文件：

- `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/types.ts`

调整 `candidateResolverType` union：

- 增加 `START_EMPLOYEE`
- 增加 `START_DEPARTMENT_MANAGER`

### 设计器

文件：

- `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-process-designer-adapter.vue`

下拉新增：

- `发起人本人` -> `START_EMPLOYEE`
- `发起人部门主管` -> `START_DEPARTMENT_MANAGER`

不新增额外输入项。两个策略都只依赖发起人快照，不需要节点参数。

## 数据流

1. 管理员在 Hunyuan BPM 设计器中选择候选人解析类型。
2. 前端 simple model 保存节点 `candidateResolverType`。
3. 发布时 `SimpleModelValidator` 校验新类型合法。
4. 发布服务固化节点快照。
5. 员工发起流程时，`BpmInstanceService` 读取定义节点快照。
6. `BpmTaskAssignmentResolver` 根据发起人快照解析审批人。
7. 解析结果写入 `assignee_<nodeKey>` 变量。
8. Flowable 按变量创建真实待办。
9. Hunyuan 任务投影和实例详情继续沿用现有运行链路。

## 测试策略

### 后端 RED 测试

优先扩展：

- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskAssignmentResolverTest.java`

新增行为：

- `START_EMPLOYEE` 解析为发起人员工 ID。
- `START_EMPLOYEE` 缺少发起人时报错。
- `START_DEPARTMENT_MANAGER` 解析为发起人部门主管。
- `START_DEPARTMENT_MANAGER` 缺少发起人部门时报错。
- `START_DEPARTMENT_MANAGER` 部门无主管时报错。

扩展：

- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeStartAssignmentTest.java`

新增或调整行为：

- 实例启动时，新策略解析出的变量会真实传给 `FlowableProcessInstanceGateway.start`。

如仓库已有校验器测试，则增加新类型合法性；没有现成测试时，优先在实现计划中补一个聚焦的 `SimpleModelValidatorTest`。

### 前端 RED 测试

优先扩展：

- `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts`

新增行为：

- 新候选类型可以作为节点快照加载和保存。
- 生成的 simple model 节点保留 `START_EMPLOYEE`。
- 生成的 simple model 节点保留 `START_DEPARTMENT_MANAGER`。

## 建议验证命令

后端聚焦测试：

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAssignmentResolverTest,BpmRuntimeStartAssignmentTest' test
```

如果新增校验器测试：

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAssignmentResolverTest,BpmRuntimeStartAssignmentTest,SimpleModelValidatorTest' test
```

前端契约测试：

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts
```

前端类型检查：

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

如本地服务可用，并且需要活体验收，再用真实发布模型发起一个 `START_EMPLOYEE` 或 `START_DEPARTMENT_MANAGER` 单节点流程，确认待办处理人来自真实员工数据。浏览器或网络证据写入 `G:/code-mcp/playwright-mcp-temp/runtime`，不提交到仓库。

## P3.1b 预留

`EMPLOYEE_SELECT_AT_START` 不进入 P3.1a，但需要保留清晰演进方向。

P3.1b 推荐先设计一个小的运行解析上下文：

```java
public record BpmTaskAssignmentContext(
        BpmEmployeeSnapshot startEmployeeSnapshot,
        String formDataJson
) {
}
```

然后把解析器从：

```java
resolve(definitionNodes, startEmployeeSnapshot)
```

演进为兼容方式：

```java
resolve(definitionNodes, context)
```

旧方法可以短期保留并委托给新上下文。`EMPLOYEE_SELECT_AT_START` 再从节点配置的表单字段 key 中读取员工 ID。这样能避免 P3.1a 期间把启动表单数据路径做成临时约定。

## 完成定义

P3.1a 完成时必须满足：

- 前端设计器能选择并保存两个新策略。
- 发布校验接受两个新策略，错误文案不再停留在旧的三类范围。
- 后端解析器能在真实实例启动路径中解析两个新策略。
- 缺少发起人、缺少发起人部门、缺少部门主管时有明确错误。
- 新增后端测试先经历 RED，再通过 GREEN。
- 新增前端契约测试通过。
- 不新增依赖。
- 不需要 SQL 时，最终说明“不涉及 SQL 变更”。
- 如果出现 SQL 需求，必须新增版本化增量脚本，且说明原因和验证方式。
- 所有中文文案和文档保持 UTF-8。

## 人工审阅重点

- 是否同意 P3.1a 只做 `START_EMPLOYEE` 和 `START_DEPARTMENT_MANAGER`。
- 是否同意 `EMPLOYEE_SELECT_AT_START` 放入 P3.1b，通过启动上下文设计后再做。
- 是否接受本切片默认不新增 SQL；只有新增菜单、权限、结构或初始化数据时才追加增量脚本。
- 是否需要在实现完成后做一次真实浏览器/API 活体验收，而不仅是源级测试。
