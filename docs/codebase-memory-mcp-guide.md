# Codebase Memory MCP 使用与项目应用指南

## 1. 文档信息

| 项目 | 内容 |
| --- | --- |
| 文档目的 | 说明 Codebase Memory MCP 能做什么、每个工具如何使用，以及如何应用于 `hunyuan-pro` |
| 适用仓库 | `E:\my-project\hunyuan-pro` |
| 核对日期 | 2026-07-21 |
| 本机版本 | `codebase-memory-mcp 0.9.0` |
| 上游仓库 | <https://github.com/DeusData/codebase-memory-mcp> |
| 本机主要索引 | `E-my-project-hunyuan-pro-current` |

本文优先以本机已安装的 `0.9.0` 可执行文件和实际 MCP 工具定义为准，同时参考上游仓库当前 README。上游文档会继续更新，因此出现功能名称、工具数量或参数差异时，应先执行 `--version`、`--help` 和 `list_projects` 核对当前环境。

## 2. 它是什么

Codebase Memory MCP 是一个本地优先的代码知识图谱服务。它先扫描代码仓库，通过 Tree-sitter、LSP 和静态分析提取代码结构，再把文件、类、函数、方法、接口、路由、调用关系、导入关系、继承关系、数据流等信息保存为可查询的图。

它通过 MCP 向 Codex、Claude Code 等 AI 编程客户端提供工具。AI 不必每次都从头遍历大量文件，而是可以先从图中回答：

- 系统由哪些模块组成；
- 某个接口、类、函数或路由在哪里；
- 一个方法被谁调用，又调用了谁；
- 一次修改可能影响哪些模块；
- 前端请求与后端 HTTP 路由是否能够关联；
- 模块之间是否存在不合理的反向依赖；
- 哪些函数是高连接度热点或潜在复杂度热点；
- 当前索引是否存在、是否需要更新。

它更接近“代码结构导航与影响分析引擎”，而不是：

- 业务知识库；
- 编译器；
- 单元测试或集成测试框架；
- 运行时链路追踪系统；
- 数据库结构和数据真实性审计工具；
- 可以独立判断需求是否完成的验收系统。

## 3. 工作原理

```text
代码仓库
  |
  | 扫描文件、解析语法、解析类型与引用
  v
AST / LSP / 静态分析
  |
  | 生成节点和关系
  v
本地代码知识图谱
  |
  | MCP 工具查询
  v
架构概览 / 符号搜索 / 调用链 / 数据流 / 变更影响 / ADR / 运行时轨迹
```

图中常见节点包括：

- `File`、`Folder`、`Module`、`Package`
- `Class`、`Interface`
- `Function`、`Method`
- `Variable`
- `Route`
- 与异步通信、服务或运行时轨迹相关的节点

常见关系包括：

- `CALLS`：函数或方法调用；
- `IMPORTS`：模块或文件导入；
- `EXTENDS`、`IMPLEMENTS`：继承与接口实现；
- `DATA_FLOWS`：参数和值的传播；
- `HTTP_CALLS`：HTTP 调用与路由关联；
- `ASYNC_CALLS`：异步调用；
- `CROSS_HTTP_CALLS`、`CROSS_ASYNC_CALLS` 等：跨仓库关联。

实际标签和边类型应通过 `get_graph_schema` 查询，不要假设每个语言、框架和索引模式都会生成相同图结构。

## 4. 是否适合 `hunyuan-pro`

结论：**适合，但应作为代码理解和改动导航工具，而不是最终验收工具。**

`hunyuan-pro` 同时包含 Java/Spring Boot 后端、Vue 3/TypeScript 前端、数据库迁移、模块化目录和较多兼容迁移工作。对于这类多模块仓库，Codebase Memory MCP 的价值主要体现在减少无目的的文件遍历，并帮助发现跨目录、跨模块的真实依赖。

### 4.1 高价值场景

| 场景 | 可以获得的帮助 |
| --- | --- |
| 新任务进入 | 先查看整体架构、模块、入口点、路由和热点，再决定读哪些源码 |
| 后端接口改造 | 从 Controller/Route 追踪到应用服务、领域服务和基础设施调用 |
| 前端接口联调 | 查找 `requestClient.get/post` 等请求及其关联的后端路由 |
| 兼容入口退役 | 查询旧类、旧路由、旧方法的入站调用，判断还有没有引用 |
| 模块边界治理 | 统计模块间调用方向，识别反向依赖和高耦合热点 |
| 变更影响分析 | 根据 Git diff 和调用图分析受影响的函数、模块和风险距离 |
| 复杂逻辑排查 | 查询圈复杂度、认知复杂度、循环深度、循环内线性扫描等属性 |
| 跨仓库服务 | 将多个仓库分别建图，再匹配 HTTP、异步消息或其他跨服务关系 |
| 架构决策记录 | 把 ADR 内容写入图，使架构决策和代码结构一起被检索 |
| 运行时补充 | 导入 trace 数据，用真实运行路径补充纯静态关系 |

### 4.2 不应直接依赖的场景

| 场景 | 原因 |
| --- | --- |
| 判断代码是否能编译 | 图索引不能替代 Maven、TypeScript 或 Vite 编译 |
| 判断数据库迁移是否正确 | 需要 Flyway、真实 MySQL 和迁移测试验证 |
| 判断权限是否正确 | 需要权限数据、菜单、角色、接口鉴权和实际用户验证 |
| 判断动态注入或反射调用 | 静态分析可能无法完整还原 Spring DI、反射和代理 |
| 判断 Vue 页面真实行为 | `<script setup>`、动态组件和运行时路由可能不完整 |
| 宣称功能已完成 | 必须结合源码审查、测试、构建、数据库和运行时验收 |

## 5. 本机当前接入情况

当前 Codex 配置中已经存在如下 MCP 接入：

```toml
[mcp_servers.codebase_memory]
type = "stdio"
command = "G:\\code-mcp\\codebase-memory-mcp-temp\\bin\\codebase-memory-mcp.exe"
startup_timeout_sec = 120

[mcp_servers.codebase_memory.env]
CBM_CACHE_DIR = "G:\\code-mcp\\codebase-memory-mcp-temp\\cache"
CBM_ALLOWED_ROOT = "E:\\my-project\\hunyuan-pro"
CBM_LOG_LEVEL = "warn"
```

这表示：

- Codex 通过标准输入输出启动 MCP 服务；
- 图数据缓存到仓库外部的 `G:\code-mcp\codebase-memory-mcp-temp\cache`；
- 当前允许索引的根目录限制为 `E:\my-project\hunyuan-pro`；
- 默认不会把图数据库提交进 Git；
- 当前无需再把 MCP 可执行文件复制到项目目录。

截至 2026-07-21，本机存在两个指向同一仓库的索引：

| 索引名 | 节点 | 边 | 建议 |
| --- | ---: | ---: | --- |
| `E-my-project-hunyuan-pro-current` | 14,822 | 36,191 | 当前优先使用 |
| `E-my-project-hunyuan-pro` | 15,074 | 35,400 | 旧索引，可能包含已删除结构 |

当前推荐在所有查询中显式指定：

```text
project = E-my-project-hunyuan-pro-current
```

不要只写目录名 `hunyuan-pro`，因为 MCP 查询参数需要的是索引项目名，而不是文件夹显示名。

## 6. 索引模式

`index_repository` 支持四种模式。

### 6.1 `fast`

特点：

- 过滤部分文件以提高速度；
- 执行类型感知的 LSP 调用和引用解析；
- 不生成相似度和语义搜索相关边。

适合：

- 代码频繁变化时快速刷新；
- 只需要类、函数、路由、调用关系和普通搜索；
- 首次试用或临时分析。

限制：

- `semantic_query` 依赖的语义能力不可用或不完整；
- 对完整仓库结构的覆盖弱于 `full`。

### 6.2 `moderate`

特点：

- 过滤部分文件；
- 包含相似度和语义能力；
- 速度和分析深度居中。

适合：

- 日常开发；
- 需要自然语言、语义近义词搜索；
- 仓库较大，不希望每次都做全量索引。

### 6.3 `full`

特点：

- 处理全部支持的文件；
- 包含类型感知解析；
- 包含相似度和语义边；
- 耗时和资源占用最高。

适合：

- 首次建立正式基线；
- 大规模架构审计；
- 模块退役、迁移完成后的干净重建；
- 需要尽可能完整的语义搜索。

对 `hunyuan-pro`，建议重大结构变更结束后使用新项目名执行一次 `full`：

```json
{
  "repo_path": "E:\\my-project\\hunyuan-pro",
  "mode": "full",
  "name": "E-my-project-hunyuan-pro-current",
  "persistence": false
}
```

### 6.4 `cross-repo-intelligence`

特点：

- 不重新提取普通代码结构；
- 在已经索引的项目之间匹配 Route、Channel 等节点；
- 创建跨仓库 HTTP、异步消息、Channel 等关系。

前提：

- 每个目标仓库已经分别完成新鲜索引；
- 通过 `list_projects` 取得准确项目名；
- 使用 `target_projects` 指定目标，或使用 `["*"]`。

适合：

- 前后端分仓；
- 多服务仓库；
- 管理后台、H5、小程序与 API 服务分仓；
- 消息生产者和消费者分仓。

### 6.5 `persistence`

`persistence: true` 会生成：

```text
.codebase-memory/graph.db.zst
```

这是压缩的团队共享图产物。团队成员可以从产物启动，而不必每次完整重建。

使用前应确认：

- 团队是否接受把图产物纳入仓库；
- 文件体积是否可接受；
- 是否包含不应共享的路径或元数据；
- CI 是否负责更新；
- 代码变化后如何保证产物不陈旧。

当前 `hunyuan-pro` 没有 `.codebase-memory` 目录，索引位于外部缓存，因此暂时不建议未经团队讨论就开启 `persistence`。

## 7. 14 个本机工具总览

| 工具 | 核心用途 | 常见使用时机 |
| --- | --- | --- |
| `index_repository` | 建立或更新代码图索引 | 首次接入、重大改造后、跨仓库匹配 |
| `list_projects` | 查看已经索引的项目 | 不确定项目名、节点数或索引列表时 |
| `index_status` | 查看单个项目索引状态 | 查询前确认索引可用 |
| `delete_project` | 删除指定索引 | 清理旧索引、解决残留节点 |
| `get_graph_schema` | 查看图标签、关系和属性 | 编写 Cypher 查询前 |
| `get_architecture` | 获取架构、依赖、路由、热点和聚类 | 新任务的第一轮仓库理解 |
| `search_graph` | 按自然语言、正则、标签或语义检索图节点 | 找类、函数、路由、变量和结构关系 |
| `search_code` | 文本搜索后用图增强、去重和排序 | 查字符串、注解、SQL、配置和实现文本 |
| `get_code_snippet` | 读取已定位符号的源码 | 找到精确 `qualified_name` 后查看实现 |
| `trace_path` | 追踪调用链、数据流和跨服务路径 | 调用方、影响范围、参数传播 |
| `query_graph` | 执行 Cypher 图查询 | 复杂统计、跨模块关系、复杂度审计 |
| `detect_changes` | 基于 Git 变更和图关系分析影响 | 提交前、重构后、版本差异检查 |
| `manage_adr` | 写入或查询架构决策记录 | 架构决策和代码知识关联 |
| `ingest_traces` | 导入运行时 trace | 用真实运行链路补充静态图 |

## 8. 工具详细说明

### 8.1 `index_repository`

**作用**

扫描仓库并创建代码知识图，是所有后续查询的前提。

**主要参数**

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `repo_path` | 是 | 仓库绝对路径 |
| `mode` | 否 | `fast`、`moderate`、`full`、`cross-repo-intelligence` |
| `name` | 否 | 自定义索引项目名 |
| `persistence` | 否 | 是否生成团队共享压缩图 |
| `target_projects` | 跨仓库模式必需 | 要建立跨仓库关系的项目名数组 |

**项目示例**

```text
请用 full 模式重新索引 E:\my-project\hunyuan-pro，
索引名使用 E-my-project-hunyuan-pro-current，
完成后报告节点数、边数和状态。
```

**注意**

- 索引可能耗费数分钟，取决于仓库大小和模式；
- 同名重建不一定能消除所有历史残留节点；
- 发生大规模删除或模块退役后，优先新建索引名；
- 若必须沿用旧名字，可先确认后使用 `delete_project` 删除再重建。

### 8.2 `list_projects`

**作用**

列出缓存中所有项目及其根路径、Git 信息、节点数、边数和图文件大小。

**参数**

无。

**项目示例**

```text
列出 Codebase Memory 当前所有索引，
指出哪个索引对应 E:\my-project\hunyuan-pro，
并比较节点数和边数。
```

**适用价值**

- 防止把目录名误当作项目名；
- 发现重复索引；
- 为跨仓库分析准备 `target_projects`；
- 判断索引是否明显陈旧。

### 8.3 `index_status`

**作用**

查询单个项目的索引状态、规模、根目录和 Git 基线。

**主要参数**

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `project` | 是 | `list_projects` 返回的索引名 |

**项目示例**

```text
检查 E-my-project-hunyuan-pro-current 的索引状态，
确认它是否 ready，并报告记录的 Git HEAD。
```

### 8.4 `delete_project`

**作用**

从 Codebase Memory 缓存中删除指定项目索引，不删除源码仓库。

**主要参数**

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `project` | 是 | 要删除的索引名 |

**适用场景**

- 清理重复索引；
- 旧索引含有已删除模块或路由；
- 需要完全干净地重建同名索引；
- 缓存损坏。

**风险边界**

- 删除的是图缓存，不是 Git 文件；
- 删除前仍应核对项目名；
- 若索引生成耗时很长，应先确认是否有共享产物；
- 当前旧索引 `E-my-project-hunyuan-pro` 不应在未确认前删除。

### 8.5 `get_graph_schema`

**作用**

返回当前图中可用的节点标签、边类型和相关属性，是编写 `query_graph` Cypher 的前置步骤。

**主要参数**

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `project` | 是 | 索引项目名 |

**项目示例**

```text
读取 E-my-project-hunyuan-pro-current 的图模式，
重点列出 Route、Class、Method、File 节点和模块依赖相关边。
```

**使用原则**

不要凭经验直接猜标签和属性名。不同版本、语言和框架生成的图模式可能不同。

### 8.6 `get_architecture`

**作用**

提供高层架构概览，包括目录结构、包、模块依赖、路由、语言、入口点、热点、边界、层次和图聚类。

它还可以通过 Leiden 社区发现从真实调用和导入关系中识别“事实模块”。事实模块可能跨越目录边界，因此可以帮助比较“代码实际如何耦合”与“目录设计想表达什么”。

**主要参数**

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `project` | 是 | 索引项目名 |
| `path` | 否 | 限定目录前缀 |
| `aspects` | 否 | 要返回的架构方面 |

`aspects` 可包含：

- `all`
- `overview`
- `structure`
- `dependencies`
- `routes`
- `languages`
- `packages`
- `entry_points`
- `hotspots`
- `boundaries`
- `layers`
- `file_tree`
- `clusters`

**项目示例**

```text
请先读取 E-my-project-hunyuan-pro-current 的整体架构，
再分别限定 hunyuan-backend 和 hunyuan-design，
比较后端模块边界、前端 API、路由入口和高耦合热点。
```

**推荐顺序**

1. 根目录 `overview`；
2. 后端目录的 `dependencies`、`boundaries`、`routes`；
3. 前端目录的 `routes`、`entry_points`、`packages`；
4. 对热点继续使用 `search_graph` 或 `trace_path`。

### 8.7 `search_graph`

**作用**

直接搜索图节点，适合查找函数、方法、类、接口、路由和变量。它不是普通全文搜索，而是结构化图搜索。

**三种搜索方式**

1. `query`：BM25 自然语言或关键词搜索，推荐作为默认入口；
2. `name_pattern` / `qn_pattern`：按名称或全限定名正则匹配；
3. `semantic_query`：向量语义搜索，用近义概念寻找实现。

三种方式可以组合，但当提供 `query` 时，`name_pattern` 会被忽略。

**主要参数**

| 参数 | 说明 |
| --- | --- |
| `project` | 索引项目名 |
| `query` | 自然语言或关键词 |
| `label` | 限定节点标签 |
| `name_pattern` | 名称正则 |
| `qn_pattern` | 全限定名正则 |
| `file_pattern` | 文件路径过滤 |
| `relationship` | 关系类型过滤 |
| `min_degree` / `max_degree` | 按节点连接度过滤 |
| `exclude_entry_points` | 排除入口点 |
| `include_connected` | 返回关联节点 |
| `semantic_query` | 关键词数组，需要 `moderate` 或 `full` |
| `limit` / `offset` | 分页参数 |

**项目示例**

```text
在 E-my-project-hunyuan-pro-current 中搜索“组织部门删除”，
只返回 Method、Route 和 Class，
并包含与结果直接连接的节点。
```

```text
查找全限定名中包含 OrganizationDepartment 的方法，
按入站连接度筛选最关键的入口。
```

**分页注意**

默认最多返回 200 条。必须检查：

- `total`
- `has_more`

当 `has_more=true` 时，增加 `offset` 继续查询，或先用标签、路径和连接度缩小范围。不能把首批结果误认为全部结果。

### 8.8 `search_code`

**作用**

先执行文本搜索，再利用代码图把命中结果归并到所属函数或方法，并按结构重要性排序。定义和高连接度函数通常优先，测试通常靠后。

它适合搜索图中不一定有独立节点的内容，例如：

- 注解；
- URL 字符串；
- SQL 片段；
- 配置键；
- 错误信息；
- 常量；
- TODO；
- XML Mapper 内容。

**主要参数**

| 参数 | 说明 |
| --- | --- |
| `pattern` | 必填，文本或正则模式 |
| `project` | 必填，索引项目名 |
| `file_pattern` | 文件 glob，例如 `*.java` |
| `path_filter` | 路径正则，例如 `^hunyuan-backend/` |
| `mode` | `compact`、`full`、`files` |
| `context` | `compact` 模式的上下文行数 |
| `regex` | 是否按正则解释 |
| `limit` | 最大图增强结果数 |

**三种输出模式**

| 模式 | 适合 |
| --- | --- |
| `compact` | 默认，返回签名和少量上下文，节省 token |
| `full` | 需要直接阅读命中源码 |
| `files` | 只想知道涉及哪些文件 |

**项目示例**

```text
在后端 Java 和 XML 中搜索 /department，
先按 files 模式列出文件，再按 compact 模式给出所属方法和结构上下文。
```

**截断注意**

比较：

- `total_grep_matches`
- `total_results`
- `limit`

该工具没有 `offset`。结果过多时应提高 `limit`，或使用 `file_pattern`、`path_filter` 缩小范围。

### 8.9 `get_code_snippet`

**作用**

读取某个已定位函数、方法、类或符号的源码片段。

**主要参数**

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `project` | 是 | 索引项目名 |
| `qualified_name` | 是 | 精确全限定名，或短名称 |
| `include_neighbors` | 否 | 是否带上相邻符号 |

**正确流程**

1. 用 `search_graph` 找到精确 `qualified_name`；
2. 把该值传给 `get_code_snippet`；
3. 如果短名称有歧义，使用工具返回的候选全限定名重试。

**项目示例**

```text
先搜索 OrganizationDepartmentApplicationService 中的删除方法，
取得精确 qualified_name，
再读取该方法及相邻方法源码。
```

它是读取工具，不适合拿一个模糊业务词直接搜索。

### 8.10 `trace_path`

**作用**

沿代码图追踪调用路径、数据流和跨服务路径，适合回答“谁调用它”“它影响谁”“这个参数流向哪里”。

**主要参数**

| 参数 | 说明 |
| --- | --- |
| `function_name` | 必填，起点函数或方法 |
| `project` | 必填，索引项目名 |
| `direction` | `inbound`、`outbound`、`both` |
| `depth` | 最大追踪深度 |
| `mode` | `calls`、`data_flow`、`cross_service` |
| `parameter_name` | 数据流模式下限定参数 |
| `edge_types` | 自定义允许经过的边 |
| `risk_labels` | 根据距离标记风险级别 |
| `include_tests` | 是否包含测试节点，默认不包含 |

**模式解释**

| 模式 | 跟踪内容 |
| --- | --- |
| `calls` | `CALLS` 调用边 |
| `data_flow` | `CALLS` 和 `DATA_FLOWS`，包含参数表达式 |
| `cross_service` | HTTP、异步、数据流及跨仓库关系 |

**项目示例**

```text
从 OrganizationDepartmentApplicationService 的删除方法开始，
向 inbound 追踪 4 层调用，
排除测试并标记风险级别。
```

```text
追踪部门 ID 参数从 Controller 到 Service、DAO 和 Mapper 的传播，
指出在哪一层发生权限校验。
```

**注意**

- 静态追踪结果不是运行时保证；
- Spring 代理、反射、事件总线和动态注册可能造成断链；
- 短函数名有歧义时应先用 `search_graph` 获取全限定名；
- 深度过大会产生噪声，应从 3 到 5 层开始。

### 8.11 `query_graph`

**作用**

执行 Cypher 查询，用于普通搜索工具难以表达的多跳关系、聚合、统计、排序和复杂度分析。

**主要参数**

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `project` | 是 | 索引项目名 |
| `query` | 是 | Cypher 查询 |
| `max_rows` | 否 | 返回行数上限 |

返回存在 100,000 行硬上限。宽查询应在 Cypher 中主动添加 `LIMIT`。

**可用于查询的复杂度信号**

- `complexity`：圈复杂度；
- `cognitive`：认知复杂度；
- `loop_count`：循环数量；
- `loop_depth`：最大嵌套循环深度；
- `transitive_loop_depth`：沿调用链传播的最坏循环深度；
- `linear_scan_in_loop`：循环内线性查找；
- `alloc_in_loop`：循环内分配或追加；
- `recursion_in_loop`：循环内递归；
- `unguarded_recursion`：缺少条件保护的递归；
- `param_count`：参数数量；
- `max_access_depth`：结构访问深度；
- `recursive`：是否递归。

**项目示例**

```cypher
MATCH (f)
WHERE f.transitive_loop_depth >= 3
   OR f.linear_scan_in_loop >= 1
RETURN f.qualified_name,
       f.file_path,
       f.transitive_loop_depth,
       f.linear_scan_in_loop
ORDER BY f.transitive_loop_depth DESC
LIMIT 50
```

**模块边界示例思路**

```text
先通过 get_graph_schema 确认 Module、File、CALLS 等标签和属性，
再统计 hunyuan-admin、hunyuan-base 和其他业务模块之间的调用方向，
特别标出反向依赖。
```

不要直接复制不匹配当前 schema 的 Cypher。先查 schema，再写查询。

### 8.12 `detect_changes`

**作用**

结合 Git 变更与代码图，分析某次变更可能影响的函数、模块和调用路径。

**主要参数**

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `project` | 是 | 索引项目名 |
| `scope` | 否 | 限定分析范围 |
| `depth` | 否 | 影响传播深度 |
| `base_branch` | 否 | 比较基准分支 |
| `since` | 否 | Git ref 或 tag，例如 `HEAD~5`、`v0.5.0` |

`since` 的语义是比较：

```text
<ref>...HEAD
```

**项目示例**

```text
分析当前分支相对于 main 的组织目录相关变更，
向外追踪 4 层，
按后端、前端、数据库和测试分类报告潜在影响。
```

**注意**

- 索引必须能识别当前文件与 Git 基线；
- 工作区未提交变更、重命名和删除的处理要结合实际输出确认；
- 它给出的是潜在影响，不等于真实回归；
- 最终仍需选择和执行对应测试。

### 8.13 `manage_adr`

**作用**

管理 Architecture Decision Record，使架构决策可以和代码图一起被检索、查询和用于解释。

**主要参数**

| 参数 | 说明 |
| --- | --- |
| `project` | 必填，索引项目名 |
| `mode` | 操作模式 |
| `content` | ADR 内容 |
| `sections` | 要处理的章节数组 |

本机 CLI 帮助只暴露上述参数，没有详细列出每个 `mode` 的枚举。实际使用前应让 MCP 客户端读取当前工具 schema，或先进行非破坏性的查询操作确认参数。

**适合记录**

- 为什么采用模块化单体；
- 为什么某个兼容入口要退役；
- 数据所有权由哪个模块负责；
- 哪些跨模块调用是临时例外；
- 某个技术方案被接受、暂定或拒绝的原因。

**项目建议**

`hunyuan-pro` 已有 `docs/architecture-baseline` 文档体系。ADR 的权威源仍应是仓库文档，Codebase Memory 中的 ADR 应作为可搜索副本或索引增强，不能只存在于本地缓存。

### 8.14 `ingest_traces`

**作用**

导入运行时 trace 数据，把实际执行路径补充到静态代码图。

**主要参数**

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `project` | 是 | 索引项目名 |
| `traces` | 是 | trace 数组 |

**适合场景**

- 静态图无法还原 Spring 代理或动态分派；
- 希望确认一个 API 在真实请求中经过哪些方法；
- 比较设计调用链与生产/测试环境实际调用链；
- 找出从未运行或仅在特定条件运行的路径。

**使用前提**

- 需要先有可转换为该工具格式的 trace 数据；
- 本机 CLI 帮助没有完整公开 trace 对象字段；
- 应先读取当前 MCP 工具 schema或上游格式文档；
- trace 可能包含 URL、参数、用户标识等敏感信息，导入前要脱敏。

## 9. 上游文档与本机版本差异

截至核对日期：

- 上游 README 的介绍使用“15 个 MCP 工具”的口径；
- 本机 `codebase-memory-mcp 0.9.0 --help` 明确列出 14 个工具；
- 上游工作流文字中出现过 `check_index_coverage`；
- 本机 `0.9.0` 的工具清单中没有 `check_index_coverage`；
- `semantic_query` 在本机是 `search_graph` 的参数，不是独立工具。

因此本文采用以下规则：

1. 本机能直接调用的 14 个工具作为当前正式能力；
2. README 中出现但本机工具清单不存在的能力标为上游差异；
3. 不在项目提示词或自动化中强依赖 `check_index_coverage`；
4. 升级后重新执行 `--version`、`--help` 并刷新本文；
5. 若新会话看不到新安装或新升级的 MCP 工具，先新建 Codex 任务或重启 Codex。

## 10. 推荐工作流

### 10.1 新需求或陌生模块

```text
list_projects
  -> index_status
  -> get_architecture(overview)
  -> get_architecture(path=目标模块)
  -> search_graph
  -> get_code_snippet
  -> 读取真实源码确认
```

目标不是一次生成“完整答案”，而是先把需要人工阅读的范围从数百个文件缩小到少量关键入口。

### 10.2 后端调用链

```text
搜索 Route 或 Controller
  -> 定位精确方法
  -> trace_path(mode=calls, outbound)
  -> 识别应用服务、领域服务、DAO/Mapper
  -> 对关键节点读取源码
  -> 用测试验证
```

适合组织目录、权限、审批、通知等跨层逻辑。

### 10.3 数据流与权限校验

```text
定位入口方法
  -> trace_path(mode=data_flow, parameter_name=...)
  -> 观察参数跨层传播
  -> 标记权限判断、转换、过滤和持久化位置
  -> 阅读源码确认条件分支
```

特别适合回答：

- `employeeId` 是否经过数据权限过滤；
- `departmentId` 是否在删除前检查引用；
- 请求 DTO 的字段最终写入哪个实体或 SQL；
- 前端传入参数是否在后端被忽略或重命名。

### 10.4 前后端路由对齐

```text
search_code 查找前端 URL 字符串
  -> search_graph 查找 Route 节点
  -> trace_path(mode=cross_service)
  -> 对照 Controller 注解和前端 API 文件
  -> 运行接口契约测试或实际请求
```

静态关联只能提供线索。动态路由前缀、网关重写、环境变量和请求封装仍需读取配置。

### 10.5 模块边界审计

```text
get_architecture(aspects=[dependencies,boundaries,clusters])
  -> get_graph_schema
  -> query_graph 统计模块间调用
  -> 找反向依赖与高连接度节点
  -> 对照架构基线判断是否违规
```

对本项目应重点关注：

- `hunyuan-admin` 是否承担了本应属于业务模块的领域逻辑；
- 基础模块是否反向依赖业务模块；
- 前端共享包是否反向依赖具体应用；
- 已退役兼容模块是否仍被其他模块调用；
- 实际聚类是否与目标模块边界一致。

### 10.6 兼容入口退役

```text
使用 search_graph 查旧类、旧方法、旧 Route
  -> trace_path(direction=inbound)
  -> search_code 查字符串、XML、配置和测试
  -> 删除或迁移代码
  -> 用新项目名 full 重建索引
  -> 再次查询旧符号和旧路由
  -> 执行编译、测试、数据库和运行时验收
```

此流程必须使用“新索引名或删除旧索引后重建”，否则旧图中可能保留没有文件路径的历史 Route 节点，造成“代码已删除但图中仍存在”的假阳性。

### 10.7 提交前影响分析

```text
detect_changes(base_branch=main, depth=3~5)
  -> 按风险和模块整理影响
  -> 补充对应测试
  -> 运行 Maven / Vitest / typecheck / build
  -> 检查 Git diff
```

### 10.8 跨仓库分析

```text
分别 full/moderate 索引各仓库
  -> list_projects 核对项目名
  -> cross-repo-intelligence 建立跨仓库关系
  -> trace_path(mode=cross_service)
  -> 对照真实网关、服务发现和消息配置
```

## 11. 索引新鲜度策略

### 11.1 日常小改动

- 开始分析前执行 `index_status`；
- 若工具支持增量更新或文件监听，可使用增量能力；
- 发现查询结果和当前源码不一致时立即重建；
- 不要把旧索引结论当作当前 checkout 的事实。

### 11.2 大规模删除、移动或模块退役

推荐：

1. 保留旧索引用于前后对比；
2. 使用新名称执行 `full`；
3. 在新索引中检查被删除符号是否仍存在；
4. 确认无误后再决定是否删除旧索引。

例如：

```text
E-my-project-hunyuan-pro-before-a2-retirement
E-my-project-hunyuan-pro-current
```

### 11.3 本项目已验证的残留问题

本项目曾对旧索引 `E-my-project-hunyuan-pro` 执行同名全量重建，但其中仍保留了已经删除、没有有效文件路径的 BPM Route 节点。新建的 `E-my-project-hunyuan-pro-current` 索引则没有这些 BPM Route。

这说明：

- 同名全量索引不应被简单等同于完全清空后重建；
- 路由节点尤其需要用源码路径复核；
- 重大删除后应优先使用新项目名；
- “图中存在”不等于“当前源码仍存在”。

## 12. 已知限制与误差来源

### 12.1 静态分析不是运行时事实

以下机制可能导致调用链缺失或误判：

- Spring AOP 和代理；
- 依赖注入和接口动态绑定；
- 反射；
- SPI；
- 事件总线；
- 表达式和配置驱动调用；
- 动态类加载；
- 运行时生成代码。

### 12.2 Vue 3 `<script setup>`

上游 issue #415 报告 Vue 3 Composition API 和 `<script setup>` 内的调用可能不能完整出现在图中：

<https://github.com/DeusData/codebase-memory-mcp/issues/415>

因此在 `hunyuan-design` 中：

- 图搜索适合定位文件、API 封装、路由和显式符号；
- 组件内部响应式调用链不能只依赖图；
- 必须配合 `rg`、源码阅读、TypeScript 检查和组件测试。

### 12.3 MyBatis XML 与动态 SQL

Java 方法到 XML Mapper、动态 SQL 标签、字符串表名和条件分支的关联可能不完整。搜索 Mapper 时应同时使用：

- `search_graph` 查 Java 接口和方法；
- `search_code` 查 XML namespace、statement ID、表名和列名；
- 数据库迁移和集成测试确认实际行为。

### 12.4 Spring 路由和动态前缀

类级别与方法级别注解、配置前缀、网关改写、上下文路径可能造成路由展示与实际 URL 不完全一致。

### 12.5 删除后的陈旧节点

同名重建可能残留历史节点。任何没有 `file_path`、源码片段或当前文件支撑的结果都应视为可疑。

### 12.6 搜索结果截断

- `search_graph` 默认最多 200 条，需要看 `has_more`；
- `search_code` 默认最多 10 个图增强结果，没有 offset；
- `query_graph` 有 100,000 行硬上限；
- 宽查询应优先缩小标签、目录和关系范围。

### 12.7 语义搜索依赖索引模式

`semantic_query` 需要 `moderate` 或 `full`。使用 `fast` 时不要期待完整语义结果。

## 13. 与现有工具的分工

| 工具 | 最适合做什么 |
| --- | --- |
| Codebase Memory MCP | 架构理解、结构化搜索、关系查询、调用链、影响分析 |
| `rg` | 精确文本、配置、SQL、XML、动态字符串的事实核对 |
| IDE/LSP | 类型跳转、重构、即时诊断 |
| Maven | Java 编译、单元测试、集成测试、模块构建 |
| Vitest | 前端单元测试与契约测试 |
| `vue-tsc` / TypeScript | Vue 和 TS 类型检查 |
| Vite/生产构建 | 打包和静态资源验证 |
| Flyway/MySQL | 迁移顺序、DDL/DML 和真实数据约束 |
| Playwright/浏览器 | 页面、路由、交互和运行时请求验证 |
| Git | 当前差异、提交边界和历史基准 |

推荐原则：

> Codebase Memory 用于快速找到应该验证什么；源码、构建、测试、数据库和运行时用于确认它是否真的正确。

## 14. 项目提示词手册

### 14.1 架构概览

```text
使用 Codebase Memory 分析 E-my-project-hunyuan-pro-current。
先给出根目录架构概览，再分别分析 hunyuan-backend 和 hunyuan-design。
重点列出模块依赖、HTTP 路由、入口点、事实聚类和高连接度热点。
所有关键结论必须附文件路径或图关系证据。
```

### 14.2 查找实现

```text
在 E-my-project-hunyuan-pro-current 中查找“组织部门删除”的实现。
先用 search_graph 找 Route、Method 和 Class，
再用 get_code_snippet 读取最关键的实现，
最后用源码搜索确认 Mapper XML 和配置中是否还有相关内容。
```

### 14.3 调用链

```text
找到 OrganizationDepartmentApplicationService 的目标方法，
取得精确 qualified_name 后，
分别追踪 inbound 和 outbound 调用 4 层。
排除测试节点，标记风险级别，并指出跨模块调用。
```

### 14.4 旧接口退役

```text
检查旧 DepartmentController、DepartmentService、DepartmentDao
及 /department 路由是否仍被引用。
图搜索、入站调用链和文本搜索必须同时执行。
对无 file_path 的节点标记为疑似陈旧索引，不得当作当前源码事实。
```

### 14.5 前后端 API 对齐

```text
查找前端所有与组织目录相关的 requestClient 请求，
再查找后端 Route 和 Controller，
按 HTTP 方法与路径建立对照表。
无法静态匹配的项列为待运行时验证，不要自行推断为已对齐。
```

### 14.6 模块边界

```text
使用 get_architecture 的 dependencies、boundaries、clusters，
分析 hunyuan-admin 与各业务模块的依赖方向。
再使用 query_graph 统计跨模块 CALLS，
重点找基础模块反向依赖业务模块的情况。
最后对照 docs/architecture-baseline 判断是否违反目标边界。
```

### 14.7 变更影响

```text
使用 detect_changes 分析当前分支相对于 main 的差异，
深度为 4。
按直接影响、间接影响、测试影响和运行时待验证项分类，
不要把潜在调用关系写成已发生回归。
```

### 14.8 复杂度热点

```text
先读取图 schema，
再查询 transitive_loop_depth >= 3、
linear_scan_in_loop >= 1、
unguarded_recursion = true 的函数。
限制 50 条，按风险排序，并读取前 10 个函数源码确认。
```

## 15. 推荐的日常使用约定

1. 每次任务先确认索引项目名，不使用含糊的文件夹名。
2. 架构类问题优先 `get_architecture`，不要先遍历全部文件。
3. 符号查找优先 `search_graph`，文本事实查找使用 `search_code` 或 `rg`。
4. `get_code_snippet` 前先取得精确 `qualified_name`。
5. 调用链默认从 3 到 5 层开始，避免一次展开过深。
6. 所有搜索都检查截断字段。
7. 无文件路径的节点不作为有效源码证据。
8. 大规模删除后新建索引名，而不是只做同名重建。
9. 架构决策以仓库文档为权威源，图中的 ADR 为辅助。
10. 在最终验收中明确区分“图分析通过”和“测试/运行时通过”。

## 16. 故障排查

### 16.1 MCP 安装后看不到工具

处理顺序：

1. 新建 Codex 任务；
2. 重启 Codex；
3. 检查 MCP 配置中的 `command` 路径；
4. 执行可执行文件 `--version`；
5. 检查启动超时和日志级别；
6. 再判断安装是否失败。

MCP 工具列表通常不会在已经打开的旧会话中自动热刷新。

### 16.2 `project not found or not indexed`

原因通常是使用了目录名，而不是索引项目名。

处理：

1. 执行 `list_projects`；
2. 复制准确的 `name`；
3. 使用 `index_status` 验证；
4. 再发起查询。

本项目当前应使用：

```text
E-my-project-hunyuan-pro-current
```

### 16.3 查询结果与源码不一致

处理：

1. 检查结果是否有 `file_path`；
2. 用 `search_code` 或 `rg` 查当前源码；
3. 检查索引记录的 Git HEAD；
4. 使用新名称执行 `full`；
5. 在新索引中重复查询。

### 16.4 语义搜索没有结果

检查：

- 索引是否由 `moderate` 或 `full` 创建；
- `semantic_query` 是否传入字符串数组；
- 是否同时使用了过窄的标签或路径过滤；
- 是否应该改用 BM25 `query`。

### 16.5 查询结果太多

- 为 `search_graph` 增加 `label`、`file_pattern`、`min_degree`；
- 使用 `limit` 和 `offset` 分页；
- 为 `search_code` 增加 `path_filter`、`file_pattern`；
- 为 Cypher 增加 `WHERE`、聚合和 `LIMIT`。

### 16.6 本地 UI

本机 `0.9.0` 帮助显示支持：

```text
--ui=true
--ui=false
--port=N
```

默认端口为 `9749`。stdio MCP 接入并不等于 UI 已自动启动。若需要启用 UI，应先确认端口、安全边界和是否允许启动额外本地服务，再使用对应启动参数。不要把“浏览器打不开 9749”直接判断为 MCP 工具不可用。

## 17. 安全与存储边界

### 17.1 本地数据

Codebase Memory 以本地优先方式运行，不要求外部图数据库。当前索引缓存位于：

```text
G:\code-mcp\codebase-memory-mcp-temp\cache
```

缓存可能包含：

- 源码路径；
- 符号名；
- 函数和类结构；
- 路由；
- 源码片段；
- Git 元数据；
- ADR；
- 导入的运行时 trace。

应按源码同等级别保护。

### 17.2 允许根目录

当前配置：

```text
CBM_ALLOWED_ROOT=E:\my-project\hunyuan-pro
```

它用于限制可索引路径。增加其他仓库时，应显式评估并调整允许范围，不要无边界开放整个磁盘。

### 17.3 团队共享产物

开启 `persistence` 前，应把 `.codebase-memory/graph.db.zst` 纳入：

- 安全审查；
- Git 体积评估；
- 更新责任；
- 忽略规则或版本控制策略；
- 离职和权限回收策略。

### 17.4 Runtime Trace

导入 trace 前应移除：

- 用户身份和令牌；
- Cookie；
- 请求体敏感字段；
- 数据库连接信息；
- 内部主机和凭据；
- 生产数据样本。

## 18. 验收边界

一次由 Codebase Memory 辅助的项目任务，建议分别报告：

| 层次 | 可接受证据 |
| --- | --- |
| 图分析 | 索引状态、节点、关系、调用链、路由和影响结果 |
| 源码确认 | 当前文件路径、精确行号、实现和配置 |
| 静态质量 | 编译、类型检查、lint |
| 自动化测试 | 单元、契约、集成、架构守卫 |
| 数据库 | Flyway、MySQL、迁移回放和数据约束 |
| 运行时 | 服务启动、接口请求、浏览器流程和日志 |
| Git | 当前 diff、提交边界、目标分支差异 |

只有图分析成功时，应表述为：

```text
已完成结构分析，尚未完成编译、测试、数据库或运行时验收。
```

不能表述为：

```text
功能已完成并可发布。
```

## 19. 对本项目的最终建议

1. 保留当前外部缓存接入，不把 MCP 二进制复制进仓库。
2. 统一使用 `E-my-project-hunyuan-pro-current` 作为当前查询项目名。
3. 新任务采用“架构概览 -> 结构搜索 -> 调用链 -> 源码确认”的顺序。
4. 组织目录兼容退役等大规模删除完成后，用新项目名做 `full` 索引。
5. 前端 Vue 3 `<script setup>`、MyBatis XML、Spring 动态绑定必须补充文本搜索。
6. 把 `detect_changes` 加入大改动后的测试选择流程，但不代替测试。
7. 暂不启用 `persistence`，除非团队决定共享图产物。
8. 暂不把 `manage_adr` 当作唯一 ADR 存储，仓库文档仍是权威来源。
9. 引入 runtime trace 前先设计脱敏和格式转换流程。
10. 升级 Codebase Memory 后重新核对工具清单和本文版本差异章节。

## 20. 参考资料

- Codebase Memory MCP 上游仓库：<https://github.com/DeusData/codebase-memory-mcp>
- Vue 3 `<script setup>` 图关系限制 issue：<https://github.com/DeusData/codebase-memory-mcp/issues/415>
- MCP 官方介绍：<https://modelcontextprotocol.io/introduction>
