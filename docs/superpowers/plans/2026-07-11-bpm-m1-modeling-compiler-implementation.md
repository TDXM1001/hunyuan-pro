# BPM M1 流程建模与编译平台实施计划

> **历史状态：** 本计划对应 2026-07-11 已完成的树形 AST 实现，不再作为当前 M1 执行计划。当前 M1 以 `docs/superpowers/specs/2026-07-11-bpm-module-01-modeling-compiler-design.md` 为准，不执行本文的旧模型兼容、旧 API 外观或树形资产任务。

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development`（推荐）或 `executing-plans` 按任务实施。所有步骤使用 checkbox（`- [ ]`）跟踪；设计范围已经锁定，不在批次间重新发起总体架构审批。

**目标：** 将 Hunyuan BPM 从线性 `userTask` 数组升级为受控 AST 与可组合 BPMN 片段编译平台，完整交付排他/并行/包容分支、结构化路由事实、办理/抄送节点、v2 资产导入导出、设计器与运行详情，并保持旧定义和历史实例兼容。

**架构：** 保留 `SimpleModelValidator`、`SimpleModelBpmnCompiler` 和现有发布 API 作为兼容外观，内部改为 `ProcessAstParser -> ProcessAstValidator -> NodeCompiler -> FragmentComposer -> CompiledDefinitionArtifact`。排他和包容路由在 split gateway 前通过固定的 `${hunyuanRouteDecisionDelegate}` 计算，Flowable 只消费 Hunyuan 写入的布尔分支变量；路由、任务、抄送和分支身份都投影为 Hunyuan 结构化事实，不从 Flowable ID 或 XML 推断。实施按后端契约、运行时、前端体验、样板业务和真实验收连续推进，所有批次共同构成一个 M1 交付块。

**技术栈：** Java 17、Spring Boot、Flowable 7.2.0、MyBatis-Plus、MySQL、JUnit 5、Mockito、AssertJ、Vue 3、TypeScript、Element Plus、Vitest、bpmn-js、Maven、pnpm、持久 Playwright MCP。

## 全局约束

- 所有生产修改只落在 `E:/my-project/hunyuan-pro`；Yudao/RuoYi 只提供机制参考。
- 不新增依赖；BPMN 构造优先复用当前 Flowable 7.2.0 已传递提供的 model/converter API。
- 公共模型只支持 `USER_TASK`、`HANDLE_TASK`、`COPY_TASK`、`EXCLUSIVE_BRANCH`、`PARALLEL_BRANCH`、`INCLUSIVE_BRANCH`；不开放自由连线、回边、任意 EL、脚本或完整 BPMN 编辑。
- v2 最大分支嵌套深度固定为 `3`；节点 key 和 branch key 在整个 AST 内分别全局唯一。
- 节点 key 继续遵守现有 `[A-Za-z_][A-Za-z0-9_]*` 和数据库 `128` 字符上限；branch key 使用同一字符集且最多 `64` 字符；全部生成 BPMN ID 和路由变量名在发布时校验不超过 Flowable 对应存储上限。
- 排他分支按顺序命中第一条规则并必须有默认分支；包容分支进入全部命中分支，无命中时进入默认分支；并行分支进入全部分支。
- 并行和包容分支内的人工节点禁止 `EDITABLE` 字段；M1 不定义并发写合并。
- 普通规则只使用类型化操作符；登记表达式只保存 `expressionKey + version + parameters`，未登记版本阻断发布。
- 路由 delegate 只接收受控 `hunyuanInstanceId` 和 `routeNodeKey`；用户条件、字段值和表达式不得拼接到 Flowable EL。
- 路由幂等键固定为 `(instance_id, engine_process_instance_id, route_node_key)`；退回重提的新 Flowable 实例必须产生新一代事实。
- `HANDLE_TASK` 首期只支持单处理人，动作固定为完成、退回、转办、委派；`COPY_TASK` 非阻塞且抄送事实幂等。
- v1 无 `schemaVersion` 模型按线性模型读取；新发布可规范化为 v2，历史定义快照和历史实例不重写。
- 管理端可看完整路由诊断；员工端只看授权字段 label、命中分支和安全原因摘要，不返回隐藏字段原值。
- SQL 使用下一空闲版本 `数据库SQL脚本/mysql/sql-update-log/v3.46.0.sql`；实施前只做一次占用检查，若已被用户其他工作占用则顺延到当时最小空闲版本并同步本计划引用。
- 自动化先跑聚焦测试，再跑 `hunyuan-bpm` 全量、Flowable 兼容测试、前端 BPM 契约测试和 `@hunyuan/system` 类型检查。
- 真实验收至少覆盖旧线性、排他两条路径和默认路径、独立并行、包容多命中、办理、设计时抄送、重提重路由、取消、回调重试。

---

## 审查确认

总体蓝图和差距基线可作为实施依据，M1 的能力面、依赖关系、非目标和关闭门槛一致。实施前已将以下四项补入模块设计：

1. 路由节点使用 Flowable service task + Spring delegate 作为统一执行钩子，避免任务服务猜测下一个节点。
2. 路由事实用 `engineProcessInstanceId` 区分退回重提代际，避免表单版本未变化时错误复用旧决定。
3. 并行/包容分支禁止可编辑字段，关闭当前审批数据版本模型下的并发写歧义。
4. 办理节点、设计时抄送、登记表达式协议和模型导入导出都属于 M1 关闭门槛，不留在计划外。

## 文件职责映射

| 责任 | 创建或修改的文件 |
| --- | --- |
| AST 与递归遍历 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/ast/` |
| 兼容解析、静态校验、发布报告 | `engine/compiler/ProcessAstParser.java`、`ProcessAstValidator.java`、`SimpleModelValidator.java`、`BpmSimpleModelPublishValidator.java`、`BpmCandidatePrecheckService.java` |
| 编译平台 | `engine/compiler/NodeCompiler.java`、`BpmnFragment.java`、`FragmentComposer.java`、`CompilerContext.java`、`node/*NodeCompiler.java`、`SimpleModelBpmnCompiler.java` |
| 路由执行与表达式登记 | `engine/route/BpmRouteExpression.java`、`BpmRouteExpressionRegistry.java`、`module/runtime/service/BpmRouteConditionEvaluator.java`、`BpmRouteDecisionService.java`、`engine/internal/HunyuanRouteDecisionDelegate.java` |
| 路由事实 | `BpmRouteDecisionEntity.java`、`BpmRouteDecisionDao.java`、`BpmRouteDecisionMapper.xml`、`BpmRouteDecisionVO.java`、`v3.46.0.sql` |
| 实例代际与 Flowable 变量 | `BpmInstanceService.java`、`FlowableProcessInstanceGateway.java` |
| 办理/抄送运行语义 | `BpmTaskService.java`、`AppBpmTaskController.java`、`BpmTaskResultEnum.java`、`BpmInstanceCopyService.java`、对应 form/VO |
| 运行图与 trace | `BpmRuntimeGraphService.java`、`BpmRuntimeGraphVO.java`、`BpmInstanceTraceService.java`、`BpmInstanceTraceVO.java` |
| 前端 AST 与资产桥接 | `components/bpm/adapters/types.ts`、`process-model-asset.ts`、`simple-model-bridge.ts` |
| 前端设计器 | `bpm-process-designer-adapter.vue`、`bpm-process-tree-editor.vue`、`bpm-process-node-editor.vue`、`bpm-branch-editor.vue`、`bpm-route-condition-editor.vue` |
| 前端运行详情 | `api/system/bpm/runtime.ts`、`bpm-runtime-process-graph.vue`、`bpm-route-decision-list.vue`、`bpm-instance-detail-drawer.vue` |
| 样板业务与闭环 | `BpmSampleExpenseDefinitionSeedService.java`、现有 sample expense tests、M1 验收记录和三份基线 |

## 锁定接口

```java
public record ProcessAst(int schemaVersion, int maxBranchDepth, List<ProcessNode> nodes) {}

public sealed interface ProcessNode permits HumanTaskNode, CopyTaskNode, BranchNode {
    String nodeKey();
    String name();
    ProcessNodeType type();
}

public record BpmnFragment(
        List<String> entryElementIds,
        List<String> exitElementIds,
        List<FlowElement> generatedElements,
        List<SequenceFlow> sequenceFlows,
        List<CompiledNodeSnapshot> compiledNodeSnapshots,
        Set<String> runtimeRequirements
) {}

public record CompiledNodeSnapshot(
        String nodeKey,
        String nodeType,
        String nodeNameSnapshot,
        Integer sortOrder,
        String authoredNodeKey,
        List<String> authoredPath,
        List<String> branchPath,
        String approvalGroupKey,
        String authoredRuleSnapshotJson,
        String compiledNodeSnapshotJson
) {}

public interface NodeCompiler<T extends ProcessNode> {
    ProcessNodeType supports();
    BpmnFragment compile(T node, CompilerContext context);
}

public record BpmRouteDecisionCommand(
        Long instanceId,
        String engineProcessInstanceId,
        String routeNodeKey
) {}

public record BpmRouteDecisionResult(
        Long routeDecisionId,
        List<String> matchedBranchKeys,
        boolean defaultBranchUsed,
        long inputFormDataVersion
) {}
```

Flowable 分支变量统一为：

```text
route_<escapedNodeKey>_<escapedBranchKey> = true | false
```

## Task 1：冻结 v1 黄金基线并建立 AST v2

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/ast/ProcessAst.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/ast/ProcessNode.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/ast/ProcessNodeType.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/ast/HumanTaskNode.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/ast/CopyTaskNode.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/ast/BranchNode.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/ast/ProcessBranch.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/ast/RouteCondition.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/ProcessAstParser.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/ProcessAstWalker.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/ProcessAstParserTest.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelBpmnCompilerTest.java`

**Interfaces:**
- Consumes: v1 `{"nodes":[...]}` 与 v2 `{"schemaVersion":2,"nodes":[...],"settings":{"maxBranchDepth":3}}`。
- Produces: 不含 Fastjson 对象的不可变 `ProcessAst`，以及按 authored 顺序递归返回节点的 `ProcessAstWalker.walk(ProcessAst)`。

- [x] **Step 1：补 v1 编译黄金测试**

在 `SimpleModelBpmnCompilerTest` 固定三类既有语义：单人、顺序多人、`parallelAll`。断言 BPMN 元素/连线集合和 compiled snapshot，而不是依赖 XML 属性顺序：

```java
@Test
void compileV1GoldenModelShouldKeepExistingTopology() {
    CompiledDefinitionArtifact artifact = compiler.compile(
            "golden_v1", "v1 黄金模型",
            "{\"nodes\":["
                    + "{\"nodeKey\":\"manager\",\"type\":\"userTask\",\"name\":\"主管审批\",\"approvalMode\":\"single\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeId\":101},"
                    + "{\"nodeKey\":\"finance\",\"type\":\"userTask\",\"name\":\"财务顺签\",\"approvalMode\":\"sequential\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[102,103]},"
                    + "{\"nodeKey\":\"audit\",\"type\":\"userTask\",\"name\":\"审计会签\",\"approvalMode\":\"parallelAll\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[104,105]}]}",
            "{\"type\":\"ALL\"}", "{}"
    );

    assertThat(artifact.compiledBpmnXml())
            .contains("id=\"manager\"")
            .contains("id=\"finance_1\"")
            .contains("id=\"finance_2\"")
            .contains("id=\"gateway_audit_split\"")
            .contains("id=\"gateway_audit_join\"");
    assertThat(artifact.nodeSnapshots()).extracting(CompiledNodeSnapshot::nodeKey)
            .containsExactly("manager", "finance_1", "finance_2", "audit_1", "audit_2");
}
```

- [x] **Step 2：运行黄金测试并确认当前通过**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=SimpleModelBpmnCompilerTest' test
```

Expected: `BUILD SUCCESS`，黄金测试在重构前通过。

- [x] **Step 3：实现 AST records、枚举、解析和递归 walker**

`ProcessNodeType` 固定为：

```java
public enum ProcessNodeType {
    USER_TASK, HANDLE_TASK, COPY_TASK,
    EXCLUSIVE_BRANCH, PARALLEL_BRANCH, INCLUSIVE_BRANCH
}
```

`ProcessAstParser.parse(String)` 对缺失版本按 v1 处理，将 `userTask` 映射为 `USER_TASK`；v2 只接受大写公共类型。解析错误抛出带稳定 code 的 `ProcessModelParseException`，至少覆盖 `MODEL_JSON_INVALID`、`SCHEMA_VERSION_UNSUPPORTED`、`NODE_TYPE_UNSUPPORTED`。

- [x] **Step 4：补 v1/v2 round-trip 与嵌套顺序测试**

```java
@Test
void parseV2ShouldKeepNestedAuthoredOrder() {
    ProcessAst ast = parser.parse(V2_EXCLUSIVE_WITH_NESTED_PARALLEL);

    assertThat(ast.schemaVersion()).isEqualTo(2);
    assertThat(walker.walk(ast)).extracting(ProcessNode::nodeKey)
            .containsExactly("amount_route", "small_review", "large_parallel", "finance", "director", "archive");
}

@Test
void missingSchemaVersionShouldNormalizeAsV1() {
    ProcessAst ast = parser.parse("{\"nodes\":[{\"nodeKey\":\"approve\",\"type\":\"userTask\",\"name\":\"审批\"}]}");
    assertThat(ast.schemaVersion()).isEqualTo(1);
    assertThat(ast.nodes()).singleElement().extracting(ProcessNode::type)
            .isEqualTo(ProcessNodeType.USER_TASK);
}
```

- [x] **Step 5：运行 AST 聚焦测试并提交**

Run: `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=ProcessAstParserTest,SimpleModelBpmnCompilerTest' test`

Expected: `BUILD SUCCESS`。

Commit:

```powershell
git add -- hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/ast hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/ProcessAstParser.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/ProcessAstWalker.java hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler
git commit -m "feat(bpm): establish versioned process ast"
```

## Task 2：递归校验、稳定错误码和登记表达式协议

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/ProcessAstValidator.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/ProcessValidationFinding.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/route/BpmRouteExpression.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/route/BpmRouteExpressionRegistry.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/route/BpmRouteExpressionDescriptor.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidator.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/BpmSimpleModelPublishValidator.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/BpmCandidatePrecheckService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/service/BpmDefinitionService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmModelController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/ProcessAstValidatorTest.java`
- Modify: existing validator/precheck tests in the same test package.

**Interfaces:**
- Consumes: `ProcessAst`、冻结表单 schema、Spring 中登记的 `BpmRouteExpression` beans。
- Produces: `List<ProcessValidationFinding>`，每项含 `level/code/message/nodeKey/branchKey/fieldKey/fixHint`。

- [x] **Step 1：写递归失败测试**

覆盖重复 node key、重复 branch key、深度超过 3、缺默认分支、空分支、字段不存在、操作符/值类型不匹配、并发分支 `EDITABLE`、未知表达式版本：

```java
@Test
void validateShouldRejectEditableFieldInsideInclusiveBranch() {
    List<ProcessValidationFinding> findings = validator.validate(
            parser.parse(INCLUSIVE_WITH_EDITABLE_TASK), FORM_SCHEMA, registry
    );

    assertThat(findings).anySatisfy(finding -> {
        assertThat(finding.code()).isEqualTo("CONCURRENT_BRANCH_EDITABLE_FORBIDDEN");
        assertThat(finding.nodeKey()).isEqualTo("finance_review");
        assertThat(finding.fieldKey()).isEqualTo("approvedAmount");
    });
}

@Test
void validateShouldRejectUnregisteredExpressionVersion() {
    assertThat(validator.validate(parser.parse(MODEL_WITH_EXPRESSION_V2), FORM_SCHEMA, registry))
            .extracting(ProcessValidationFinding::code)
            .contains("ROUTE_EXPRESSION_NOT_REGISTERED");
}
```

- [x] **Step 2：运行测试并确认失败**

Run: `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=ProcessAstValidatorTest' test`

Expected: FAIL because `ProcessAstValidator` and stable finding codes do not exist.

- [x] **Step 3：实现 validator 与兼容外观**

`SimpleModelValidator.validate` 调用 parser/validator 并把第一条 blocking finding 转成当前 `ResponseDTO<String>`；`BpmDefinitionService.validateForPublish` 将全部 finding 加入现有发布报告。`BpmSimpleModelPublishValidator` 和 `BpmCandidatePrecheckService` 改用 `ProcessAstWalker`，确保嵌套人工节点得到字段校验和候选预检。

登记协议固定为：

```java
public interface BpmRouteExpression {
    String expressionKey();
    int version();
    BpmRouteExpressionDescriptor descriptor();
    BpmRouteExpressionResult evaluate(BpmRouteExpressionContext context, Map<String, Object> parameters);
}

public record BpmRouteExpressionResult(boolean matched, String reasonCode, String reasonText) {}
```

注册表按 `(expressionKey, version)` 唯一建索引；重复 bean 在应用启动时失败。
`GET /bpm/model/route-expression/catalog` 只返回 descriptor，不返回 bean/class 名；没有登记项时返回空数组，设计器不显示登记表达式模式。

- [x] **Step 4：补递归候选人与发布报告测试**

在 `BpmCandidatePrecheckServiceTest` 证明嵌套分支内 `ROLE` 和 `EMPLOYEE_SELECT_AT_START` 都返回 nodeKey；在 `BpmDefinitionPublishServiceTest` 证明两个 blocking finding 都进入报告而不是只保留通用 `SIMPLE_MODEL_VALIDATION_FAILED`。

- [x] **Step 5：运行聚焦门禁并提交**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=ProcessAstValidatorTest,SimpleModelValidatorTest,BpmSimpleModelPublishValidatorTest,BpmCandidatePrecheckServiceTest,BpmDefinitionPublishServiceTest' test
```

Expected: `BUILD SUCCESS`。

Commit: `git commit -am "feat(bpm): validate recursive process models"`

## Task 3：重构为片段编译平台并保持 v1 行为

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/NodeCompiler.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/BpmnFragment.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/FragmentComposer.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/CompilerContext.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/StableBpmnIdFactory.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/node/HumanTaskNodeCompiler.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelBpmnCompiler.java`
- Modify: `CompiledDefinitionArtifact.java` and `CompiledNodeSnapshot.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/FragmentComposerTest.java`
- Modify: `SimpleModelBpmnCompilerTest.java`

**Interfaces:**
- Consumes: validated `ProcessAst` and existing start/variable snapshots.
- Produces: Flowable-valid `BpmnModel` serialized to XML plus flattened compiled snapshots carrying `authoredNodeKey`, `authoredPath`, `branchPath`, and generated IDs.

- [x] **Step 1：写片段组合失败测试**

```java
@Test
void composeShouldConnectEveryExitToNextEntry() {
    BpmnFragment left = fragment(List.of("a"), List.of("a1", "a2"));
    BpmnFragment right = fragment(List.of("b1", "b2"), List.of("b"));

    BpmnFragment composed = composer.compose(List.of(left, right), context);

    assertThat(composed.sequenceFlows()).extracting(SequenceFlow::getSourceRef, SequenceFlow::getTargetRef)
            .contains(tuple("a1", "b1"), tuple("a1", "b2"), tuple("a2", "b1"), tuple("a2", "b2"));
}
```

- [x] **Step 2：运行测试并确认失败**

Run: `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=FragmentComposerTest' test`

Expected: FAIL because compiler abstractions do not exist.

- [x] **Step 3：实现结构化 BPMN 产物与稳定 ID**

`StableBpmnIdFactory` 固定命名：

```text
<compiledTaskNodeKey>
hy_route_<nodeKey>_decide
hy_gateway_<nodeKey>_split
hy_gateway_<nodeKey>_join
hy_flow_<sourceKey>__<targetKey>__<ordinal>
```

人工任务元素继续使用已校验的 compiled task key，保持 v1 公共 taskKey 和 `assignee_<nodeKey>` 兼容；只有 service task、gateway 和 flow 使用 `hy_` 生成命名空间。compiled snapshot 始终保存 authored key/path，运行时不得反向解析生成 ID。`SimpleModelBpmnCompiler` 保持原 public signature，内部注入 parser、compiler registry 和 composer。

- [x] **Step 4：让 v1 黄金测试与现有发布测试全部恢复通过**

允许新 XML 的命名空间/属性顺序变化，但旧模型必须保持任务数量、处理人变量、顺序和 `parallelAll` split/join 行为。同步更新只依赖旧字符串 ID 的测试断言为结构化元素断言。

- [x] **Step 5：运行编译与发布门禁并提交**

Run: `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=FragmentComposerTest,SimpleModelBpmnCompilerTest,BpmDefinitionPublishServiceTest' test`

Expected: `BUILD SUCCESS`。

Commit: `git commit -am "refactor(bpm): compose bpmn from typed fragments"`

## Task 4：建立路由账本、条件计算器和 Flowable delegate

**Files:**
- Create: `数据库SQL脚本/mysql/sql-update-log/v3.46.0.sql`
- Create: `module/runtime/domain/entity/BpmRouteDecisionEntity.java`
- Create: `module/runtime/dao/BpmRouteDecisionDao.java`
- Create: `main/resources/mapper/bpm/runtime/BpmRouteDecisionMapper.xml`
- Create: `module/runtime/domain/vo/BpmRouteDecisionVO.java`
- Create: `module/runtime/service/BpmRouteConditionEvaluator.java`
- Create: `module/runtime/service/BpmRouteDecisionService.java`
- Create: `engine/internal/HunyuanRouteDecisionDelegate.java`
- Create: `test/java/com/hunyuan/sa/bpm/runtime/BpmRouteConditionEvaluatorTest.java`
- Create: `test/java/com/hunyuan/sa/bpm/runtime/BpmRouteDecisionServiceTest.java`
- Modify: `test/java/com/hunyuan/sa/bpm/schema/BpmSchemaSourceTest.java`

**Interfaces:**
- Consumes: `BpmRouteDecisionCommand` and frozen route node snapshot.
- Produces: one immutable route row per engine generation/node and Flowable boolean variables for every branch.

- [x] **Step 1：写 SQL 契约和 evaluator 失败测试**

`v3.46.0.sql` 必须定义：

```sql
CREATE TABLE `t_bpm_route_decision` (
  `route_decision_id` bigint NOT NULL AUTO_INCREMENT,
  `instance_id` bigint NOT NULL,
  `definition_id` bigint NOT NULL,
  `definition_node_id` bigint NOT NULL,
  `engine_process_instance_id` varchar(64) NOT NULL,
  `route_node_key` varchar(128) NOT NULL,
  `input_form_data_version` bigint NOT NULL,
  `matched_branch_keys_json` longtext NOT NULL,
  `default_branch_used` bit(1) NOT NULL DEFAULT b'0',
  `evaluation_status` varchar(16) NOT NULL,
  `reason_snapshot_json` longtext NOT NULL,
  `evaluated_at` datetime NOT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`route_decision_id`),
  UNIQUE KEY `uk_bpm_route_generation_node` (`instance_id`,`engine_process_instance_id`,`route_node_key`),
  KEY `idx_bpm_route_instance` (`instance_id`,`route_decision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM路由决定事实';
```

同一脚本为 `t_bpm_instance_copy` 增加可空 `source_event_key varchar(191)` 和唯一索引 `uk_bpm_copy_source_target(instance_id, engine_process_instance_id, source_event_key, target_employee_id)`。

- [x] **Step 2：实现类型化条件计算**

覆盖 `TEXT/NUMBER/BOOLEAN/SELECT` 与 `EQ/NEQ/GT/GTE/LT/LTE/IN/NOT_IN/EMPTY/NOT_EMPTY/CONTAINS/NOT_CONTAINS`。数字使用 `BigDecimal.compareTo`，集合输入先规范为稳定列表；类型不匹配返回 `ROUTE_VALUE_TYPE_MISMATCH`，不做静默字符串转换。

- [x] **Step 3：实现原子 evaluate-and-record**

```java
@Transactional(rollbackFor = Exception.class)
public BpmRouteDecisionResult evaluateAndRecord(BpmRouteDecisionCommand command) {
    // 按 instanceId 锁定 Hunyuan 实例，读取当前 formDataVersion 与冻结定义节点。
    // 先按幂等键查已有成功事实；不存在时计算、插入并返回。
    // 唯一键竞争时重查同一事实，禁止产生第二组变量。
}
```

失败事实和令牌推进在同一事务回滚，因此业务表不保留半完成 `FAILED` 行；诊断通过异常 code 和命令日志记录。对可恢复的重复调用只返回既有 `SUCCEEDED` 事实。

- [x] **Step 4：实现固定 JavaDelegate**

```java
@Component("hunyuanRouteDecisionDelegate")
public class HunyuanRouteDecisionDelegate implements JavaDelegate {
    @Resource private BpmRouteDecisionService routeDecisionService;
    private Expression routeNodeKey;

    @Override
    public void execute(DelegateExecution execution) {
        Long instanceId = Long.valueOf(String.valueOf(execution.getVariable("hunyuanInstanceId")));
        String nodeKey = String.valueOf(routeNodeKey.getValue(execution));
        BpmRouteDecisionResult result = routeDecisionService.evaluateAndRecord(
                new BpmRouteDecisionCommand(instanceId, execution.getProcessInstanceId(), nodeKey)
        );
        routeDecisionService.writeBranchVariables(execution, nodeKey, result.matchedBranchKeys());
    }
}
```

- [x] **Step 5：运行 schema、evaluator、幂等测试并提交**

Run: `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmSchemaSourceTest,BpmRouteConditionEvaluatorTest,BpmRouteDecisionServiceTest' test`

Expected: `BUILD SUCCESS`。

Commit:

```powershell
git add -- '数据库SQL脚本/mysql/sql-update-log/v3.46.0.sql' hunyuan-backend/hunyuan-bpm/src
git commit -m "feat(bpm): persist deterministic route decisions"
```

## Task 5：调整实例启动/重提事务顺序并传递 Hunyuan 实例 ID

**Files:**
- Modify: `engine/internal/FlowableProcessInstanceGateway.java`
- Modify: `module/runtime/service/BpmInstanceService.java`
- Modify: `test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeStartAssignmentTest.java`
- Modify: `test/java/com/hunyuan/sa/bpm/runtime/BpmInstanceServiceTest.java`
- Create: `test/java/com/hunyuan/sa/bpm/runtime/HunyuanRouteDecisionDelegateTest.java`

**Interfaces:**
- Consumes: definition, start/resubmit form data, assignment variables.
- Produces: Flowable start variables including `hunyuanInstanceId`; root route delegate can read a transaction-visible Hunyuan instance.

- [x] **Step 1：写启动顺序失败测试**

Mockito `InOrder` 断言初次发起先 insert instance，再 `gateway.start(..., instanceId, ...)`，最后更新 engine ID；重提先锁定并更新 form snapshot/version，再启动新 engine generation。

- [x] **Step 2：修改 gateway signature**

```java
public String start(
        String engineProcessDefinitionId,
        Long hunyuanInstanceId,
        Long employeeId,
        String formDataJson,
        Map<String, Object> runtimeAssignmentVariables
) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("hunyuanInstanceId", hunyuanInstanceId);
    variables.put("startEmployeeId", employeeId);
    // 保留 formData/formDataJson 和 assignment variables。
}
```

- [x] **Step 3：重排初次发起和重提**

初次发起：校验与解析处理人 -> insert Hunyuan instance（engine ID 为空）-> insert initial change -> start Flowable -> update engine ID -> sync tasks。

重提：锁实例与版本 -> 计算新数据版本 -> 更新 Hunyuan snapshot/state -> 写变更账本 -> start Flowable with same instance ID -> update engine ID -> 写重提日志 -> sync tasks。任一步失败由现有事务回滚。

- [x] **Step 4：验证根路由和同版本重提代际**

delegate test 证明两次不同 `engineProcessInstanceId`、相同 `instanceId + routeNodeKey + formDataVersion` 会写两条事实；同一 engine ID 重试只保留一条。

- [x] **Step 5：运行运行时聚焦门禁并提交**

Run: `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmRuntimeStartAssignmentTest,BpmInstanceServiceTest,HunyuanRouteDecisionDelegateTest' test`

Expected: `BUILD SUCCESS`。

Commit: `git commit -am "refactor(bpm): establish instance before engine routing"`

## Task 6：交付排他分支编译与运行

**Files:**
- Create: `engine/compiler/node/ExclusiveBranchNodeCompiler.java`
- Modify: `FragmentComposer.java`、`StableBpmnIdFactory.java`、`CompiledNodeSnapshot.java`
- Modify: `SimpleModelBpmnCompilerTest.java`
- Create: `test/java/com/hunyuan/sa/bpm/runtime/BpmExclusiveRouteFlowableTest.java`

**Interfaces:**
- Consumes: `BranchNode(EXCLUSIVE_BRANCH)` with ordered branches and one default.
- Produces: decide service task -> exclusive split -> branch fragments -> exclusive join；条件 flow 只比较 Hunyuan boolean 变量。

- [x] **Step 1：写三路径编译失败测试**

断言 XML 包含 delegate、split/join、两个条件 flow 和无条件 default flow，并确认 `compareValue`、字段值不出现在 `${...}` 表达式中。

- [x] **Step 2：实现 `ExclusiveBranchNodeCompiler`**

每个非默认分支生成 `${route_<node>_<branch> == true}`；split gateway 的 `defaultFlow` 指向默认分支。空分支合法时用直达 join 的 sequence flow 表达，不创建虚假 user task。

- [x] **Step 3：补真实 Flowable 内核测试**

用 H2/测试 ProcessEngine 部署编译 XML，分别写入 small/large/default 变量，断言每次只产生目标分支的活动任务，分支完成后只进入一次 join 后任务。

- [x] **Step 4：运行编译与 Flowable 排他门禁并提交**

Run: `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=SimpleModelBpmnCompilerTest,BpmExclusiveRouteFlowableTest' test`

Expected: `BUILD SUCCESS`。

Commit: `git commit -am "feat(bpm): compile exclusive approval branches"`

## Task 7：交付独立并行分支与结构化分支身份

**Files:**
- Create: `engine/compiler/node/ParallelBranchNodeCompiler.java`
- Modify: `CompiledNodeSnapshot.java`
- Modify: `module/runtime/service/BpmTaskProjectionService.java`
- Modify: `module/runtime/domain/vo/BpmTaskVO.java`
- Modify: task mapper/VO projection tests.
- Create: `test/java/com/hunyuan/sa/bpm/runtime/BpmParallelBranchFlowableTest.java`

**Interfaces:**
- Consumes: `PARALLEL_BRANCH` with two or more branches.
- Produces: parallel split/join, every nested compiled task snapshot with stable `branchPath`, and task VO with `authoredNodeKey/branchPath`.

- [x] **Step 1：写独立并行与旧 `parallelAll` 区分测试**

```java
    assertThat(artifact.nodeSnapshots())
        .filteredOn(snapshot -> snapshot.authoredNodeKey().equals("finance_review"))
        .singleElement()
        .satisfies(snapshot -> assertThat(snapshot.branchPath()).containsExactly("parallel_review.finance"));
assertThat(artifact.nodeSnapshots()).extracting(CompiledNodeSnapshot::approvalGroupKey)
        .doesNotContain("parallel_review");
```

- [x] **Step 2：实现并行编译和固定汇合**

所有分支从 split 同时进入；每个分支的全部 exits 连接 join；join 只有一条父级出口。并行分支本身不创建审批组，分支内部既有 `parallelAll` 仍按审批组处理。

- [x] **Step 3：投影 authored branch 身份**

`BpmTaskProjectionService` 从 definition node compiled snapshot 读取 `authoredNodeKey`、`authoredPath`、`branchPath`，写入 task VO/详情的结构化字段；不新增 task 表列，因为历史任务已有 `definitionNodeId` 可稳定关联冻结快照。

- [x] **Step 4：Flowable 测试全部进入、全部完成后一次汇合**

测试两个分支同时产生活动任务，先完成一个时 join 后任务不存在，完成第二个后 join 后任务恰好一条；取消流程后没有残留活动任务。

- [x] **Step 5：运行聚焦门禁并提交**

Run: `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=SimpleModelBpmnCompilerTest,BpmParallelBranchFlowableTest,BpmTaskProjectionServiceTest,BpmTaskListProjectionContractTest' test`

Expected: `BUILD SUCCESS`。

Commit: `git commit -am "feat(bpm): execute independent parallel branches"`

## Task 8：交付包容分支与动态汇合

**Files:**
- Create: `engine/compiler/node/InclusiveBranchNodeCompiler.java`
- Modify: `BpmRouteDecisionService.java`
- Modify: `SimpleModelBpmnCompilerTest.java`
- Create: `test/java/com/hunyuan/sa/bpm/runtime/BpmInclusiveBranchFlowableTest.java`
- Modify: route service/evaluator tests.

**Interfaces:**
- Consumes: `INCLUSIVE_BRANCH` and evaluator result containing one or more branch keys.
- Produces: inclusive split/join that waits only for actually entered branches.

- [x] **Step 1：写单命中、多命中、默认命中失败测试**

Flowable test 分别选择 `{finance}`、`{finance,legal}`、`{default}`，断言活动任务集合和 join 后任务数量。

- [x] **Step 2：实现 inclusive gateway 条件和默认 flow**

每个非默认分支读取自己的 boolean 变量；默认 flow 无用户表达式。`BpmRouteDecisionService` 保存稳定排序后的 `matchedBranchKeys`，但按 authored 分支顺序写变量与 trace。

- [x] **Step 3：验证幂等、并发和数据版本**

并发两次 delegate 调用时唯一键只允许一个决定；第二次读取同一 matched set。修改表单版本后若仍在同一 engine generation/route node 则返回既有决定并记录诊断日志，因为无回边模型不允许同代重新路由。

- [x] **Step 4：运行包容聚焦门禁并提交**

Run: `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmInclusiveBranchFlowableTest,BpmRouteDecisionServiceTest,SimpleModelBpmnCompilerTest' test`

Expected: `BUILD SUCCESS`。

Commit: `git commit -am "feat(bpm): execute inclusive approval branches"`

## Task 9：交付办理节点和设计时抄送节点

**Files:**
- Create: `engine/compiler/node/CopyTaskNodeCompiler.java`
- Create: `engine/internal/HunyuanCopyTaskDelegate.java`
- Create: `module/runtime/service/BpmCopyRecipientResolver.java`
- Modify: `module/runtime/service/BpmInstanceCopyService.java`
- Create: `module/runtime/domain/form/BpmTaskCompleteForm.java`
- Modify: `module/runtime/service/BpmTaskService.java`
- Modify: `module/runtime/service/BpmTaskAssignmentResolver.java`
- Modify: `controller/app/AppBpmTaskController.java`
- Modify: `common/enumeration/BpmTaskResultEnum.java`
- Modify: `module/runtime/domain/vo/BpmTaskVO.java` and `BpmTaskDetailVO.java`
- Create/modify corresponding service, controller, projection and compiler tests.

**Interfaces:**
- Consumes: `HANDLE_TASK` single assignee; `COPY_TASK` frozen recipient resolver.
- Produces: `taskKind=APPROVAL|HANDLE`、`availableActions`、`HANDLED(7)` result、幂等 design copy facts.

- [x] **Step 1：写办理动作隔离测试**

断言 handle task 调用 approve/reject 返回用户错误；调用 `/app/bpm/task/complete` 完成 Flowable task、写 `HANDLED` 和 `HANDLE_COMPLETED` action log；return/transfer/delegate 保持可用，加签/减签不可用。

- [x] **Step 2：实现结构化可用动作**

```java
public enum BpmTaskKind { APPROVAL, HANDLE }

// VO 不让前端从 nodeType 或 taskName 猜动作。
private BpmTaskKind taskKind;
private List<String> availableActions;
```

`BpmTaskService` 在所有动作入口先调用统一 `BpmTaskActionPolicy.requireAllowed(task, action)`。

- [x] **Step 3：实现非阻塞 copy delegate**

`HunyuanCopyTaskDelegate` 读取 `hunyuanInstanceId`、`copyNodeKey` 和 engine PID，按冻结节点快照解析接收人，调用 `createDesignCopies`。`sourceEventKey` 固定为 `COPY:<engineProcessInstanceId>:<copyNodeKey>`；唯一键竞争视为幂等成功。通知发送沿现有记录/重试边界进行，抄送事实写入成功后令牌立即继续。

- [x] **Step 4：限制 M1 接收人语义**

`EMPLOYEE` 支持一个或多个固定员工；`ROLE` 解析当前可用成员；`DEPARTMENT_MANAGER`、`START_EMPLOYEE`、`START_DEPARTMENT_MANAGER`、`EMPLOYEE_SELECT_AT_START` 复用现有组织网关。空集合默认失败关闭并阻断 delegate，发布期能确定为空时直接阻断发布。

- [x] **Step 5：运行办理/抄送门禁并提交**

Run: `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=SimpleModelBpmnCompilerTest,BpmTaskServiceTest,BpmTaskAdvancedActionServiceTest,BpmInstanceCopyServiceTest,BpmTaskProjectionServiceTest' test`

Expected: `BUILD SUCCESS`。

Commit: `git commit -am "feat(bpm): add handle and design copy nodes"`

## Task 10：升级前端模型资产桥接与导入导出

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/types.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/process-model-asset.ts`
- Modify: `simple-model-bridge.ts`
- Modify: `bpm-designer-adapters.test.ts`
- Modify: `views/system/bpm/model/model-editor.vue`
- Modify: `api/system/bpm/model.ts`

**Interfaces:**
- Consumes: backend v1/v2 JSON and current form schema.
- Produces: recursive discriminated TypeScript union, canonical v2 JSON, `.hunyuan-process.json` import/export.

- [x] **Step 1：写 v1/v2 round-trip 与导入失败测试**

```ts
it('round-trips nested v2 branches without dropping authored identity', () => {
  const asset = parseProcessModelAsset(v2Fixture);
  expect(parseProcessModelAsset(stringifyProcessModelAsset(asset))).toEqual(asset);
});

it.each([3, 99])('rejects unsupported schemaVersion %s before replacing draft', (version) => {
  expect(() => parseProcessModelAsset(JSON.stringify({ schemaVersion: version, nodes: [] })))
    .toThrow('不支持的流程模型版本');
});
```

- [x] **Step 2：实现递归 union 和兼容解析**

```ts
export type BpmProcessNodeDraft =
  | BpmHumanTaskNodeDraft
  | BpmCopyTaskNodeDraft
  | BpmBranchNodeDraft;

export interface BpmBranchNodeDraft {
  branchType: 'EXCLUSIVE' | 'INCLUSIVE' | 'PARALLEL';
  branches: BpmProcessBranchDraft[];
  id: string;
  name: string;
  nodeKey: string;
  type: 'branch';
}
```

v1 解析后可在内存规范化，但保存前必须显式生成 `schemaVersion: 2`；未知节点、未知版本、重复 key、损坏引用抛出错误，不过滤后继续。

- [x] **Step 3：加入导入/导出命令**

模型编辑器工具栏使用 `Upload`/`Download` lucide/Element Plus 图标按钮和 tooltip。导出文件名为 `<modelKey>-v2.hunyuan-process.json`；导入先 parse + 前端完整校验，再展示摘要确认，用户确认后才替换当前草稿并标记 dirty。`model.ts` 同时读取 `/bpm/model/route-expression/catalog`，供登记表达式选择器使用。

- [x] **Step 4：运行前端聚焦测试和类型检查并提交**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts --dom
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: Vitest 通过；typecheck exit code `0`。

Commit: `git commit -am "feat(bpm): support versioned model assets"`

## Task 11：实现递归设计器、分支规则和只读预览

**Files:**
- Create: `components/bpm/adapters/bpm-process-tree-editor.vue`
- Create: `components/bpm/adapters/bpm-process-node-editor.vue`
- Create: `components/bpm/adapters/bpm-branch-editor.vue`
- Create: `components/bpm/adapters/bpm-route-condition-editor.vue`
- Create: `components/bpm/adapters/process-preview-graph.ts`
- Modify: `bpm-process-designer-adapter.vue`
- Modify: `bpm-designer-adapters.test.ts`
- Modify: `views/system/bpm/bpm-modules.test.ts`

**Interfaces:**
- Consumes: recursive `BpmProcessNodeDraft[]`、form schema field options、registered expression catalog（若后端没有可用条目则 UI 不展示该模式）。
- Produces: 节点增删/排序/嵌套、类型化条件编辑、固定 split/join 预览、与后端一致的校验消息。

- [x] **Step 1：拆出现有人工节点编辑器并保持 UI 回归**

先把当前 700+ 行 adapter 的节点属性部分迁入 `bpm-process-node-editor.vue`，保持单人/顺序/`parallelAll`、字段权限、监听器和候选规则行为不变；原 adapter 只协调 snapshot、dirty、validate 和预览。

- [x] **Step 2：实现递归树编辑器**

每个节点行提供图标命令：在前/后新增、移动、复制、删除；分支节点使用明确 split、branch、join 带状布局，最大深度 3。节点/分支 key 自动生成后仍允许编辑，失效 key 直接就地提示。

- [x] **Step 3：实现条件编辑器**

字段下拉只来自绑定表单 schema；选择字段后按类型过滤操作符，值控件使用数字输入、开关、选择器或文本输入。无效历史字段保持可见错误。排他/包容必须维护一个默认分支；并行不显示条件控件。

- [x] **Step 4：实现 HANDLE/COPY 属性**

办理节点复用单处理人候选配置，不显示 approvalMode 和拒绝策略。抄送节点展示接收人来源和通知渠道，不显示任务字段权限。

- [x] **Step 5：实现固定拓扑预览**

`process-preview-graph.ts` 从 authored AST 生成只读 BPMN-DI：三种 gateway 使用不同图标/标签，分支名贴近连线，join 后回到父级。预览只表达 authored 结构，不伪造运行状态。

- [x] **Step 6：运行前端契约、类型检查并做桌面/移动截图检查**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: tests pass；typecheck exit code `0`。使用 Playwright 在 `1440x900` 和 `390x844` 检查工具栏、三层嵌套、长分支名、属性区无重叠和横向溢出。

Commit: `git commit -am "feat(bpm): build recursive process designer"`

## Task 12：交付结构化运行图、路由 trace 和员工安全视图

**Files:**
- Create: `module/runtime/domain/vo/BpmRuntimeGraphVO.java`
- Create: `module/runtime/service/BpmRuntimeGraphService.java`
- Modify: `module/runtime/domain/vo/BpmInstanceTraceVO.java`
- Modify: `module/runtime/service/BpmInstanceTraceService.java`
- Create: `test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeGraphServiceTest.java`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`
- Create: `views/system/bpm/runtime/components/bpm-runtime-process-graph.vue`
- Create: `views/system/bpm/runtime/components/bpm-route-decision-list.vue`
- Modify: `bpm-instance-detail-drawer.vue`
- Modify: runtime contract tests.

**Interfaces:**
- Consumes: definition snapshot, compiled node snapshots, task/action/copy/route facts, caller view mode.
- Produces: authored graph with node state and route decisions; employee view never contains hidden field values.

- [x] **Step 1：写管理端/员工端裁剪失败测试**

```java
assertThat(adminGraph.getRouteDecisions().getFirst().getReasonSnapshotJson())
        .contains("approvedAmount").contains("6000");
assertThat(employeeGraph.getRouteDecisions().getFirst().getReasonSnapshotJson())
        .contains("金额条件已满足").doesNotContain("6000").doesNotContain("internalCode");
```

- [x] **Step 2：实现运行图装配**

节点状态固定为 `NOT_ENTERED/ACTIVE/COMPLETED/CANCELLED/SKIPPED`；分支状态来自 route decision + 真实 task/copy facts。未命中分支标为 `SKIPPED`，不能从 absence 猜测。旧 v1 实例返回线性 authored graph 和空 route decisions。

- [x] **Step 3：扩展 trace 契约**

`BpmInstanceTraceVO` 增加 `processGraph` 和 `routeDecisions`。管理端 trace 调用完整视图；App 详情/trace 通过服务端安全视图。route VO 只暴露 Hunyuan instance/definition/node/branch/form version/time，不含 Flowable execution/gateway ID。

- [x] **Step 4：实现前端运行图和路由列表**

详情抽屉新增“流程路径”tab，展示 authored graph、当前节点、命中/跳过分支；“路由记录”使用紧凑表格展示节点、命中分支、输入版本、时间和安全原因。没有 route facts 的旧实例不显示空面板。

- [x] **Step 5：运行后端/前端聚焦门禁并提交**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmRuntimeGraphServiceTest,BpmInstanceTraceServiceTest,BpmRuntimeDetailServiceTest' test
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: all pass。

Commit: `git commit -am "feat(bpm): explain runtime branch decisions"`

## Task 13：用样板费用关闭多路径业务线

**Files:**
- Modify: `module/sampleexpense/service/BpmSampleExpenseDefinitionSeedService.java`
- Modify: `test/java/com/hunyuan/sa/bpm/sampleexpense/BpmSampleExpenseDefinitionSeedServiceTest.java`
- Modify: `test/java/com/hunyuan/sa/bpm/sampleexpense/BpmSampleExpenseServiceTest.java`
- Create: `test/java/com/hunyuan/sa/bpm/runtime/BpmM1BusinessFlowTest.java`

**Interfaces:**
- Consumes: existing sample expense form fields `requestedAmount/approvedAmount`, employee `1` test seed.
- Produces: one v2 definition proving exclusive/default, independent parallel, inclusive, handle and copy without introducing a second sample domain.

- [x] **Step 1：固定样板 v2 路径**

样板定义使用以下规则：

```text
requestedAmount <= 5000 -> 小额财务核定
requestedAmount > 5000  -> 财务核定 + 风险复核（独立并行）
默认分支               -> 人工核验办理
三条路径汇合后进入包容通知：金额 >= 10000 命中财务抄送，所有路径命中归档确认
```

测试环境继续使用员工 `1`，避免额外组织种子依赖；并行分支的人工节点均为只读，实际可编辑 `approvedAmount` 的财务核定放在并行 split 之前或排他单分支内。

- [x] **Step 2：写端到端 Spring/Flowable 测试**

测试创建/发布定义，启动 `4999`、`5001`、缺失金额默认路径三个实例，逐一完成任务，断言 route rows、branchPath、copy facts、handle result、最终 callback snapshot/version。

- [x] **Step 3：覆盖退回重提、拒绝、取消与回调重试**

退回后以新金额重提，断言新 engine generation 重新路由且旧 route row 不变；并行分支任一拒绝取消其他活动任务；取消包容分支实例不残留任务；回调第一次失败、重试成功仍使用同一最终表单版本和路由事实。

- [x] **Step 4：运行样板与 M1 业务门禁并提交**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmSampleExpenseDefinitionSeedServiceTest,BpmSampleExpenseServiceTest,BpmSampleExpenseCallbackHandlerTest,BpmM1BusinessFlowTest' test
```

Expected: `BUILD SUCCESS`。

Commit: `git commit -am "feat(bpm): prove m1 with multi-path expense flow"`

## Task 14：全量门禁、真实验收和基线回写

**Files:**
- Create: `docs/superpowers/specs/2026-07-11-bpm-m1-modeling-compiler-acceptance.md`
- Modify: `docs/superpowers/specs/2026-07-10-bpm-development-baseline.md`
- Modify: `docs/superpowers/specs/2026-07-11-bpm-enterprise-gap-baseline.md`
- Modify: `docs/superpowers/specs/2026-07-11-bpm-enterprise-blueprint.md`
- Modify: this plan only to check completed items; do not rewrite planned evidence as actual evidence.

**Interfaces:**
- Consumes: completed Tasks 1-13 and a running local frontend/backend.
- Produces: current-run automated evidence, API/Chrome/database acceptance, and honest baseline status.

- [x] **Step 1：运行后端全量与 Flowable 兼容门禁**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin '-Dtest=BpmFlowableCompatibilityTest' test
```

Expected: both `BUILD SUCCESS`；记录本次实际测试数和警告，不复用历史数字。

- [x] **Step 2：运行前端 BPM 契约与类型门禁**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: all test files pass；typecheck exit code `0`。

- [x] **Step 3：执行 API/Chrome/数据库真实验收矩阵**

依次证明：

1. v1 旧模型仍可保存、发布、发起和完成。
2. v2 导入导出后 JSON 完整往返，未知版本在覆盖草稿前失败。
3. `4999/5001/缺值` 进入小额/大额/默认路径，未命中分支无任务。
4. 独立并行同时出现两个任务，全部完成后只汇合一次。
5. 包容分支单命中/多命中只等待实际进入分支。
6. 办理任务只显示完成/退回/转办/委派；审批按钮不可用。
7. 设计时抄送生成一条幂等 copy fact，通知失败不阻塞令牌。
8. 退回重提按新数据重新路由，route facts 按 engine generation 保留。
9. 员工详情看不到隐藏字段原值；管理端能查看完整诊断。
10. 拒绝、取消、回调失败重试和最终业务回写保持稳定。

- [x] **Step 4：写验收记录并回写三份基线**

验收记录区分：当前能力事实、本次执行命令/测试数、真实 definition/instance/task/routeDecision/copy IDs、已知边界和后续 M4/M2 工作。差距基线把 M1 对应项更新为实际状态；开发基线将下一优先级移到蓝图中的 M4；总体蓝图只更新模块状态和验收链接，不改架构方向。

- [x] **Step 5：执行完成前静态检查并提交文档**

Run:

```powershell
rg -n "TBD|TODO|应该可以|待补" docs/superpowers/specs/2026-07-11-bpm-m1-modeling-compiler-acceptance.md docs/superpowers/specs/2026-07-10-bpm-development-baseline.md docs/superpowers/specs/2026-07-11-bpm-enterprise-gap-baseline.md docs/superpowers/specs/2026-07-11-bpm-enterprise-blueprint.md
git diff --check
```

Expected: `rg` 无命中；`git diff --check` 无 whitespace error。

Commit:

```powershell
git add -- docs/superpowers/specs/2026-07-11-bpm-m1-modeling-compiler-acceptance.md docs/superpowers/specs/2026-07-10-bpm-development-baseline.md docs/superpowers/specs/2026-07-11-bpm-enterprise-gap-baseline.md docs/superpowers/specs/2026-07-11-bpm-enterprise-blueprint.md
git commit -m "docs(bpm): record m1 modeling platform acceptance"
```

## M1 完成门槛

- AST v2、递归校验、编译平台、三种分支、路由 delegate/账本、登记表达式协议全部通过自动化与真实运行验证。
- `HANDLE_TASK` 与 `COPY_TASK` 具备独立公共语义，不复用审批文案或从 Flowable 对象推断。
- 设计器、导入导出、只读预览、运行图和 route trace 形成前后端闭环。
- v1 线性、顺序多人、`parallelAll`、字段权限、数据版本、退回重提、可靠回调无回归。
- 样板费用至少证明排他、并行、包容、默认、办理、抄送和可靠回写。
- 当前运行的全量门禁、Flowable 兼容、Chrome/API/数据库证据全部写入验收记录。
- 三份基线已回写，旧的条件路由计划继续保持“已被总体蓝图取代”状态。

## 建议提交序列

```text
feat(bpm): establish versioned process ast
feat(bpm): validate recursive process models
refactor(bpm): compose bpmn from typed fragments
feat(bpm): persist deterministic route decisions
refactor(bpm): establish instance before engine routing
feat(bpm): compile exclusive approval branches
feat(bpm): execute independent parallel branches
feat(bpm): execute inclusive approval branches
feat(bpm): add handle and design copy nodes
feat(bpm): support versioned model assets
feat(bpm): build recursive process designer
feat(bpm): explain runtime branch decisions
feat(bpm): prove m1 with multi-path expense flow
docs(bpm): record m1 modeling platform acceptance
```
