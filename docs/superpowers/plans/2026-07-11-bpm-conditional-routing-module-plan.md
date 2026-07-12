# BPM 受限条件审批路由模块实施计划

> **状态：已被总体蓝图取代，不应独立执行。** 本计划只覆盖排他条件路由，范围不足以关闭企业级流程编排能力。其有效内容已并入 `docs/superpowers/specs/2026-07-11-bpm-module-01-modeling-compiler-design.md`；后续应先基于 M1 模块设计编写新的整体实施计划。

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development`（推荐）或 `executing-plans` 按任务批次实施。设计范围已经在本计划中锁定；实施时按批次验证，不把自然联动重新拆成多轮审批。

**目标：** 在 Hunyuan BPM 中交付一个可配置、可发布、可运行、可追踪、可回写的“受限条件审批路由”模块，使流程能够依据已发布表单数据确定性地选择后续审批路径。

**架构：** 保留现有 authored `nodes[]` 顺序模型和 Hunyuan/Flowable 边界，仅新增一种受限的 `exclusiveRoute` 片段。设计器配置有限字段、有限运算符和有序分支；发布时将条件冻结到定义快照；发起或节点完成时由后端使用当前实例快照一次性计算路由，编译器生成固定的排他网关分支结构，运行时投影、trace 和业务回调只暴露 Hunyuan 的路由语义。它不是通用 BPMN 图编辑器，不支持脚本表达式、任意网关嵌套或运行中改路由。

**技术栈：** Java 17、Spring Boot、Flowable 7.2.0、MyBatis-Plus、MySQL、JUnit 5、Mockito、AssertJ、Vue 3、TypeScript、Element Plus、Vitest、Maven、pnpm、持久 Playwright MCP。

## 全局约束

- 实现留在 `E:/my-project/hunyuan-pro`；Yudao/RuoYi 仅用于机制参考。
- 本模块依赖“审批数据治理模块”完成：节点权限快照、表单数据版本、原子变更、字段审计和最终回写必须先通过总体验收；未通过前只做本计划，不进入生产实现。
- 首期只支持 `exclusiveRoute`：按已绑定表单 schema 的字段值匹配有序分支，命中第一条即选择该分支，否则走明确的默认分支；不支持脚本、正则、跨字段表达式、外部服务调用或人工运行时改路由。
- 条件字段只能来自发布时冻结的表单 schema；字段被删除、类型不匹配、条件重复或没有默认分支时阻断发布。
- 不实现通用并行/包容网关、或签、比例审批、定时器、子流程、动态修改已运行实例路由。
- 路由计算在服务端完成，前端校验只用于体验；员工接口不得接收或决定最终分支。
- 不新增运行时依赖；新增 SQL 版本实施前必须确认版本号未被占用。
- 历史线性、顺序审批和 `parallelAll` 定义保持兼容；旧定义无 route 配置时行为不变。
- 浏览器截图、网络日志、会话和 profile 只存放在运行证据目录，不提交仓库。

## 锁定领域契约

### DSL

新增 authored 节点类型，仅允许出现在现有节点数组中：

```json
{
  "nodeKey": "amount_route",
  "name": "按金额选择审批路径",
  "type": "exclusiveRoute",
  "fieldKey": "amount",
  "valueType": "NUMBER",
  "branches": [
    {
      "branchKey": "small",
      "name": "小额审批",
      "operator": "LTE",
      "compareValue": "5000",
      "nodes": [
        {
          "nodeKey": "manager_review",
          "name": "经理审批",
          "type": "userTask",
          "approvalMode": "single",
          "candidateResolverType": "DEPARTMENT_MANAGER"
        }
      ]
    },
    {
      "branchKey": "large",
      "name": "大额审批",
      "operator": "GT",
      "compareValue": "5000",
      "nodes": [
        {
          "nodeKey": "finance_review",
          "name": "财务审批",
          "type": "userTask",
          "approvalMode": "single",
          "candidateResolverType": "ROLE",
          "roleIds": [201]
        },
        {
          "nodeKey": "director_review",
          "name": "总监审批",
          "type": "userTask",
          "approvalMode": "single",
          "candidateResolverType": "ROLE",
          "roleIds": [202]
        }
      ]
    },
    {
      "branchKey": "manual_review",
      "name": "人工兜底审批",
      "isDefault": true,
      "nodes": [
        {
          "nodeKey": "manual_review",
          "name": "人工复核",
          "type": "userTask",
          "approvalMode": "single",
          "candidateResolverType": "ROLE",
          "roleIds": [203]
        }
      ]
    }
  ],
  "defaultBranchKey": "manual_review"
}
```

首期允许的字段类型和运算符：

| 字段类型 | 运算符 |
| --- | --- |
| `TEXT` | `EQ`、`NEQ`、`IN`、`NOT_IN` |
| `NUMBER` | `EQ`、`NEQ`、`GT`、`GTE`、`LT`、`LTE` |
| `BOOLEAN` | `EQ` |
| `SELECT` | `EQ`、`NEQ`、`IN`、`NOT_IN` |

首期每个路由节点只允许一个 `fieldKey`，分支按数组顺序评估。每个分支内嵌自己的线性 `userTask nodes[]` 片段，分支结束后固定汇合，再继续路由节点之后的顶层 authored 节点。分支内允许复用已完成的单人、顺序多人和 `parallelAll` 节点模式，但不允许继续嵌套 `exclusiveRoute`；默认分支必须存在且只能有一个。

### 编译和运行快照

编译器生成固定的：

```text
前置片段 -> exclusiveGateway split -> 条件 sequenceFlow -> 分支内线性片段
         -> exclusiveGateway join -> 路由节点后的下一顶层 authored 节点
```

每条分支快照至少包含：`routeNodeKey`、`branchKey`、`branchName`、`fieldKey`、`valueType`、`operator`、`compareValue`、`isDefault`、`authoredNodes`、`compiledNodeKeys`。运行时只能读取定义快照，不读取模型草稿。

用户配置不会直接变成 Flowable 表达式。Hunyuan 在启动实例或完成路由前置任务之前，用 `BpmRouteConditionEvaluator` 计算分支并写入受控流程变量：

```text
route_<routeNodeKey> = <branchKey>
```

编译器只生成由 Hunyuan 控制的固定等值判断，例如 `${route_amount_route == 'small'}`；默认 sequence flow 不带用户表达式。任何 `compareValue`、字段值或用户输入都不得拼接进 Flowable EL。

### 运行记录

新增 `t_bpm_route_decision`，记录一次实例路由事实：实例、定义、路由节点、输入数据版本、字段 key、字段值摘要、命中分支、计算时间、计算结果和失败原因。字段值按现有员工/管理端数据权限处理，不能把隐藏字段原值直接暴露给员工端。

## 文件职责映射

| 区域 | 文件/目录 | 责任 |
| --- | --- | --- |
| 设计器类型与桥接 | `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/types.ts`、`simple-model-bridge.ts` | 定义路由 DSL、序列化、预览和非法配置提示 |
| 设计器交互 | `.../bpm-process-designer-adapter.vue` | 绑定表单字段、运算符、分支、默认分支和目标节点 |
| 发布校验 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidator.java`、`BpmSimpleModelPublishValidator.java` | 校验字段、类型、分支唯一性、内嵌节点和默认分支 |
| BPMN 编译 | `.../engine/compiler/SimpleModelBpmnCompiler.java` | 编译受限排他网关和条件 sequence flow，写入 compiled snapshot |
| 路由计算 | 新建 `.../module/runtime/service/BpmRouteDecisionService.java`、`.../module/runtime/service/BpmRouteConditionEvaluator.java` | 读取冻结规则和实例当前数据，确定性计算并持久化路由事实 |
| 数据访问 | 新建 `BpmRouteDecisionEntity/Dao/Mapper.xml`；新增 SQL | 保存路由决定、按实例和节点查询 |
| 运行时接入 | `BpmInstanceService.java`、`BpmTaskProjectionService.java`、`BpmInstanceTraceService.java` | 在正确的实例版本和任务完成边界执行路由，展示分支事实 |
| API/前端 | `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`、运行时详情组件 | 输出路由摘要，不暴露 Flowable gateway/execution ID |
| 样板业务 | `module/sampleexpense` 相关 service/VO/test | 用金额条件证明不同路径和最终业务回写 |
| 文档 | `docs/superpowers/specs/2026-07-11-bpm-conditional-routing-acceptance.md`、BPM baseline | 记录真实验证并更新边界 |

## 连续实施批次

### 批次 0：前置门禁和架构自审

- [ ] 确认 `2026-07-11-bpm-approval-data-governance-design.md` 已完成总体验收，尤其是数据版本、字段权限快照和回写幂等。
- [ ] 确认当前 SQL 版本、模型节点契约和编译器保留 ID 规则没有冲突。
- [ ] 固定本模块非目标：无通用网关、无脚本、无运行中改路由。
- [ ] 写出一条完整样板线：费用金额 -> 条件选择 -> 不同审批路径 -> 结果回写。

### 批次 1：DSL、设计器和发布校验

- [ ] 先为 DSL round-trip、字段类型和非法分支配置补充 Vitest/Java 失败测试。
- [ ] 在 `types.ts`、`simple-model-bridge.ts` 和设计器适配器中加入 `exclusiveRoute`，字段候选只来自当前表单 schema。
- [ ] 在后端校验器中阻断：字段不存在、运算符与字段类型不匹配、分支 key 重复、内嵌节点为空、内嵌节点 key 全局冲突、嵌套路由、默认分支缺失。
- [ ] 发布检查报告使用稳定错误码，例如 `ROUTE_FIELD_NOT_FOUND`、`ROUTE_OPERATOR_TYPE_MISMATCH`、`ROUTE_DEFAULT_BRANCH_MISSING`、`ROUTE_BRANCH_NODE_INVALID`、`ROUTE_NESTED_FORBIDDEN`。
- [ ] 门禁：设计器契约测试、`SimpleModelValidatorTest`、`BpmSimpleModelPublishValidatorTest`、系统类型检查。

### 批次 2：受限排他网关编译与快照

- [ ] 为单条件、多分支、默认分支和非法 ID 补充 `SimpleModelBpmnCompilerTest`。
- [ ] 扩展编译器生成稳定 gateway/flow ID、条件 sequence flow 和每个分支的 compiled snapshot。
- [ ] 条件 sequence flow 只比较 Hunyuan 预计算的 `route_<nodeKey>` 与冻结 `branchKey`，不得把用户条件直接编译成 Flowable EL。
- [ ] 保证原有线性、顺序多人、`parallelAll` 编译结果不变；分支可复用现有审批片段，但路由节点不允许嵌套路由，也不允许跨分支引用节点。
- [ ] 运行 Flowable 兼容测试，确认部署后只有命中分支的后续任务出现。

### 批次 3：运行时路由决定、版本和幂等

- [ ] 先为条件计算器建立纯函数测试：缺值、类型转换、边界值、`IN/NOT_IN`、默认分支和不命中行为。
- [ ] 新增 `BpmRouteDecisionService`，以实例当前 `formDataVersion` 和定义节点快照为输入，输出稳定的 `branchKey` 和目标节点。
- [ ] 在实例启动和完成路由前置任务两个边界识别即将进入的顶层 route 节点，在驱动 Flowable 前写入 `route_<nodeKey>`；Flowable 不自行读取表单 JSON 或执行用户条件。
- [ ] 路由决定与触发它的审批动作处于同一事务；版本冲突、条件计算失败或快照损坏时不推进 Flowable。
- [ ] 以 `(instance_id, route_node_key, form_data_version)` 做幂等约束，重试只能复用同一决定，不得创建重复下游任务。
- [ ] 明确并发边界：路由只读取已提交当前版本；旧版本请求返回 `FORM_DATA_VERSION_CONFLICT`。

### 批次 4：运行投影、trace 和业务回写

- [ ] 在任务/实例详情中增加结构化 `routeDecision`，包含路由名、命中分支、输入版本和时间；普通实例返回空值。
- [ ] trace 展示“按字段 X 的值 Y 命中分支 Z”，字段 label 优先，历史 label 缺失时回退 key。
- [ ] 样板费用申请至少覆盖“小额经理审批”和“大额财务+总监审批”两条路径，最终回写携带最终表单版本和路由事实。
- [ ] 回调失败重试复用同一最终快照与路由决定；重复事件不得改写已完成业务单据。
- [ ] 前端不解析 BPMN XML、Flowable ID 或内部 JSON 推断分支。

### 批次 5：兼容、真实验收和基线回写

- [ ] 后端门禁：`hunyuan-bpm` 全量测试、`BpmFlowableCompatibilityTest`、路由并发/幂等测试。
- [ ] 前端门禁：BPM 适配器和运行时契约测试、`@hunyuan/system` typecheck。
- [ ] 浏览器/API 活体验收覆盖：设计、发布、金额 4999、金额 5000、金额 5001、默认分支、拒绝/退回、重提后重新计算、回调失败重试。
- [ ] 验收记录写明每条路径的 definition/instance/task/routeDecision ID 和实际命令结果，不提交运行证据产物。
- [ ] 更新 BPM 基线：新增“受限条件审批路由”已完成能力，并继续明确不支持通用网关、脚本、定时器和子流程。

## 验收矩阵

| 验收面 | 必须证明的行为 |
| --- | --- |
| 配置 | 条件字段只能来自表单 schema，分支和默认分支可视化配置 |
| 发布 | 类型不匹配、字段删除、重复 key、目标缺失、无默认分支均阻断 |
| 编译 | 生成固定排他网关；无条件分支不会产生隐藏下游任务 |
| 主路径 | 4999 与 5001 进入不同审批线，节点权限和数据版本继续生效 |
| 边界 | 5000 的比较语义稳定；缺值/不命中走默认分支 |
| 并发 | 同一实例同一数据版本只产生一个路由决定和一组下游任务 |
| 安全 | 员工不能提交 route branch 或修改已冻结条件；后端是唯一裁决者 |
| 退回重提 | 使用最新表单快照重新计算，旧路由事实保留在历史中 |
| 回写 | 最终业务事件携带最终版本、最终快照和路由事实，失败重试幂等 |
| 兼容 | 旧定义、普通任务、顺序审批和 `parallelAll` 行为不退化 |
| 真实运行 | 浏览器/API 完成至少两条不同分支和一条默认分支的闭环 |

## 完成定义

只有以下条件同时满足才关闭本模块：

1. 设计器能够配置受限条件路由，且发布校验与后端语义一致。
2. 定义快照冻结条件，运行时不读取模型草稿。
3. Flowable 只执行命中的固定分支，不产生未命中分支任务。
4. 路由决定与表单数据版本、审批动作、审计和 trace 可关联。
5. 退回重提按新版本重新计算，历史路由事实不可篡改。
6. 样板业务至少有两条审批路径和默认路径，并完成可靠回写。
7. 自动化门禁、Flowable 兼容验证和真实浏览器/API 验收均有本次执行证据。
8. BPM 基线已更新，且没有把该能力描述成通用 BPMN 网关平台。

## 建议提交边界

```text
feat(bpm): 增加受限条件路由 DSL 与发布校验
feat(bpm): 编译并执行受限排他审批路径
feat(bpm): 记录路由决定并接入运行时追踪
feat(bpm): 完成条件路由样板业务回写
docs(bpm): 记录条件审批路由验收并更新基线
```
