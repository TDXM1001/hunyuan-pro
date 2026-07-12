# BPM 模块 M1：流程定义中心设计

- 日期：2026-07-11
- 重审日期：2026-07-12
- 状态：Graph 实现、自动化、Flowable 部署及认证浏览器业务链验收已关闭
- 优先级：P0
- 总体蓝图：`docs/superpowers/specs/2026-07-11-bpm-enterprise-blueprint.md`
- 前置条件：无业务模块前置；BPM 数据已清空

## 1. 结论

M1 建立流程定义的唯一事实源，完整负责管理员“配置了什么、能否发布、发布后如何冻结”。正式作者模型是 `HunyuanProcessDefinitionGraph`，Flowable BPMN 只是编译产物。

M1 不建设旧作者模型导入、双读、双写或兼容发布链。现有树形 AST 和编译器只能提供片段编译、路由 delegate、稳定 ID 和验证断言等算法参考。

## 2. 服务对象与边界

- 普通业务管理员使用业务化节点、自动布局和表单式属性。
- 专业流程管理员配置连接、分支、汇合、策略和版本。
- 实施人员只读查看编译 BPMN、元素映射和部署诊断。

M1 包含流程分类、模板、复制、Graph、草稿、布局、结构校验、设计器、模拟、编译、部署、发布/下线权限、发布版本和 authored/compiled 映射。M1 定义跨模块版本引用与只读 resolver 端口，但不解析实际审批人、不拥有业务对象数据、不处理人工任务，也不迁移运行实例。M2/M3 尚未提供真实引用时，只能完成 M1 的结构与发布机制验收，不能开放生产业务发起。

## 3. 核心契约

```text
ProcessDefinitionDraft
  draftId / revision / graph / layout / status

HunyuanProcessDefinitionGraph
  schemaVersion / rootScopeId / scopes[] / nodes[] / edges[] / policies

DefinitionVersionSnapshot
  immutableGraph / dependencyVersions / semanticHash / compilerVersion
  compiledBpmn / authoredCompiledMappings / publishedBy / publishedAt
```

节点、边和作用域使用系统生成且不可复用的稳定 ID。显示名称与布局可以变化，但稳定 ID 不变。布局不参与语义 hash，纯拖动不能生成新的定义版本。

M1 核心节点目录包括开始、结束、审批、办理、抄送、条件、并行和包容；高级节点由 M5 在同一注册、schema 和版本机制上扩展。

## 4. 端到端业务线

```text
创建分类、模板或流程资产
-> 编辑正式 Graph
-> 保存草稿 revision
-> 局部与全局校验
-> 样例路由/候选/字段模拟
-> 语义 Diff
-> 发布确认
-> 编译与 Flowable 解析验证
-> 原子发布不可变定义版本
```

设计器、API 和编译器只能读写同一 Graph 契约，不能由前端生成另一种树形 JSON 再让后端猜测语义。

## 5. 校验与失败闭环

- 草稿可暂时不完整，但 wire schema、未知节点类型、非法 ID 和越权配置立即拒绝。
- 发布阻断无入口/出口、不可达节点、环、跨作用域边、端口不匹配、分叉/汇合不成对、缺少默认路径和并发字段冲突。
- 跨模块引用由版本化 resolver 校验；候选、字段、业务契约或连接器不存在时不能发布。
- revision 冲突不得覆盖他人草稿。
- 编译或部署失败不得产生半发布定义；无法反查 authored 元素的产物不得发布。

## 6. 编译边界

```text
Graph
-> GraphValidator
-> Structured Process IR
-> NodeCompiler / FragmentComposer
-> BPMN + CompiledElementMappings
-> Flowable parse/deploy
```

内部 IR 可以吸收旧 AST 的有效算法，但不能成为新的持久化作者模型。任一 BPMN activity、gateway、sequence flow、timer 或 delegate 都必须能反查 authored node、edge 或 scope。

## 7. 完成定义

1. 可保存、读取、导入导出同一 Graph，canonical round-trip 语义不变。
2. 核心节点可由业务化设计器配置、校验、模拟和发布。
3. 发布版本冻结 Graph、依赖版本、BPMN、编译器版本和元素映射；M1 测试 resolver 只用于证明发布机制，生产发布必须由 M2/M3 的真实版本目录通过校验。
4. 发布失败不产生半发布记录，草稿修改不影响已发布定义。
5. 所有错误定位到节点、边、作用域或具体属性，并给出修复建议。
6. 新数据库、API 和前端中不存在旧作者模型字段、解析分支或双写逻辑。
7. 分类、模板复制、定义发布/下线权限和一条包含审批、条件、并行、抄送的编译部署均有证明。

M1 完成只表示定义能力面闭环；M1-M4 全部完成后，才形成第一个可用审批产品基线。

## 8. M2 审批阶段控制契约

`APPROVAL` 节点的候选、完成和退回语义由 M2 的 `CandidatePolicyVersion`、`ApprovalPolicyVersion` 与节点级绑定定义；本节只定义 M1 的编译和冻结责任。发布时，M1 必须把完整 canonical 策略内容、schema 版本、摘要和节点级 `returnTargetNodeId` 一并冻结到定义依赖快照，不能只保留目录 ID 或摘要。

每个已发布 `APPROVAL` 节点必须编译为一个稳定 `compiledElementId` 的 `ApprovalStageControl` 等待点，并在 authored/compiled 映射中保存 authored 节点、策略依赖和退回目标。该等待点不直接向员工公开分配，不以 Flowable multi-instance 表达多人语义；底层可以是受控 `receiveTask` 或内部 `userTask`，但必须由 M4 以一个 `stageInvocationId` 完成、关闭或恢复。M1 负责验证该端口可部署、可反查和在定义快照中唯一，M2/M4 负责成员解析、成员投影和运行推进。
