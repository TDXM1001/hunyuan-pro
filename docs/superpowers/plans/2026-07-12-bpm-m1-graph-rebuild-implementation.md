# BPM M1 Graph 重建实施计划

**目标：** 以 `HunyuanProcessDefinitionGraph` 替换旧树形 AST，建立流程定义唯一作者事实源，并逐批完成草稿、校验、编译、发布和设计器闭环。

**架构：** 新 Graph 在 `engine/graph` 中定义纯作者契约与规范化规则；`module/model` 负责草稿 revision 和资产生命周期；编译层只消费已冻结的 Graph 快照，生成 BPMN 与 authored/compiled 映射。旧 `SimpleModel` 代码只能作为算法与回归证据来源，不能成为新 Graph 的读取、写入或发布兼容路径。

**技术栈：** Java 17、Spring Boot、Fastjson 2、Flowable 7.2.0、MyBatis-Plus、JUnit 5、AssertJ、Vue 3、TypeScript、Vitest、pnpm、Maven。

## 全局约束

- 正式作者模型固定为 `HunyuanProcessDefinitionGraph`；不引入旧树形 JSON 的导入、双读、双写或兼容发布。
- 节点、边、作用域 ID 系统生成且不可复用；显示名称和布局不参与语义 hash。
- Graph 草稿可以不完整；wire schema、未知节点类型、非法 ID 与越权配置在保存时阻断，发布再执行全局结构与跨模块引用校验。
- M1 只实现开始、结束、审批、办理、抄送、条件、并行、包容节点；时间、外部等待与子流程留给 M5。
- 不新增依赖，不修改当前未提交的 `engine/ast`、旧 `SimpleModel`、M4/M5 运行时代码或已有历史计划。
- 每一批先写失败测试并记录 RED，再写最小实现；发布失败不得留下定义版本、部署记录或映射残留。

## 文件职责

| 责任 | 路径 |
| --- | --- |
| Graph 契约、ID、canonical JSON、语义 hash、局部校验 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/graph/` |
| Graph 单元测试 | `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/graph/` |
| 草稿 revision、分类、模板、导入导出 API | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/model/` |
| 发布快照、Graph 编译与映射 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/` 与 `engine/compiler/graph/` |
| 新定义表和版本事实 | `数据库SQL脚本/mysql/sql-update-log/` 的下一空闲版本 |
| Graph 设计器和 API 适配 | `hunyuan-design/apps/hunyuan-system/src/components/bpm/graph/` 与 `apps/hunyuan-system/src/views/system/bpm/model/` |

## 批次 1：Graph 契约与语义稳定性

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/graph/HunyuanProcessDefinitionGraph.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/graph/GraphScope.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/graph/GraphNode.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/graph/GraphEdge.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/graph/GraphNodeType.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/graph/GraphCanonicalizer.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/graph/GraphValidationException.java`
- Test: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/graph/HunyuanProcessDefinitionGraphTest.java`

**Produces:** `GraphCanonicalizer.canonicalize(HunyuanProcessDefinitionGraph)` and `semanticHash(HunyuanProcessDefinitionGraph)`; both accept an immutable Graph and reject duplicate IDs, missing root scope, cross-scope edges and unsupported M1 node types.

1. Write `HunyuanProcessDefinitionGraphTest` to prove: layout-only edits preserve canonical semantic hash; duplicate node IDs and cross-scope edges throw `GraphValidationException`; `DELAY` is rejected.
2. Run `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=HunyuanProcessDefinitionGraphTest' test`; expected RED because Graph types do not yet exist.
3. Implement immutable records/enums, sorted canonical JSON excluding layout, SHA-256 hash, and local validation.
4. Re-run the focused test and then `ProcessAstParserTest,SimpleModelBpmnCompilerTest` as a non-invasive legacy regression check.

## 批次 2：草稿、分类、模板与 revision

**Files:** `module/model` entity/DAO/service/controller/form/VO tests; next available BPM SQL migration.

1. Add `t_bpm_process_draft` and `t_bpm_process_template` with immutable `draft_id`, monotonic `revision`, `graph_json`, `layout_json`, `semantic_hash`, author/audit fields and optimistic revision update condition.
2. Write service tests proving save rejects stale revision, layout-only save updates layout without changing semantic hash, and template copy assigns entirely new stable IDs.
3. Add Graph save/read/import/export endpoints; imports accept only Graph schema and allocate fresh IDs when cloning.
4. Run targeted DAO/service/controller tests and schema source test.

## 批次 3：发布预检、冻结与编译

**Files:** `module/definition`, new `engine/compiler/graph`, definition snapshot/mapping tables and tests.

1. Define versioned resolver ports for M2 candidate policy and M3 business/form contracts; provide test-only resolvers only in tests.
2. Write publish tests for global reachability, gateway/default-path pairing, dependency version freeze, compiler failure rollback, and complete authored-to-compiled mapping.
3. Compile immutable Graph snapshot into BPMN, parse/deploy with Flowable, and atomically persist `DefinitionVersionSnapshot` plus mappings only after all checks succeed.
4. Verify Graph compiler tests, definition publish tests, Flowable compatibility test and `hunyuan-bpm` module test suite.

## 批次 4：设计器、模拟、Diff 与权限验收

**Files:** Graph frontend components/API tests, model editor, admin routes and backend simulation/diff endpoints.

1. Replace tree adapter input with Graph API contract; render business node palette, connections, scope-aware property forms and automatic layout without BPMN/XML exposure.
2. Write Vitest contract tests for Graph round-trip, validation marker placement, simulation diagnostics and layout-only semantic stability.
3. Add semantic Diff and sample simulation endpoints; enforce draft/edit, publish and deactivate permissions.
4. Run frontend contract tests, `@hunyuan/system` typecheck, backend focused tests, and browser proof for create -> save -> simulate -> publish -> version inspection.

## 验收与文档

M1 closes only when one definition containing approval, condition, parallel and copy nodes is saved, validated, simulated, compiled/deployed and frozen with a complete mapping; failed publish leaves no partial record; layout-only edits do not create a semantic version. Record fresh evidence in `docs/superpowers/specs/2026-07-11-bpm-m1-modeling-compiler-acceptance.md` and update the BPM development baseline after the final verification.
