# BPM 管理端基座设计

## 背景

当前 `hunyuan-pro` 已经有一层 BPM 管理端薄壳，但还没有形成完整、可用、符合当前系统风格的管理端基座。

当前仓库已经存在以下前端 BPM 入口与页面雏形：

- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/category/category-list.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/form/form-list.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/model/model-list.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/model/model-editor.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/definition/definition-list.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/instance/instance-list.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/task/task-list.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/listener/listener-catalog.vue`

这些页面说明当前仓已经具备：

- 本地真实路由页，而不是纯桥接占位页
- `ArtSearchPanel + ArtTablePanel` 的列表页壳层
- `ArtEditPage + ArtEditSection` 的编辑页壳层
- `/bpm/category`、`/bpm/form`、`/bpm/model`、`/bpm/definition`、`/bpm/designer`、`/bpm/task`、`/bpm/listener` 的前端 API contract 雏形

但当前薄壳仍存在明显短板：

- `form-list.vue` 仍以 `schemaJson / layoutJson` 文本框作为核心交互
- `model-editor.vue` 仍以 `simpleModelJson / startRuleJson / variableMappingJson` 文本框作为核心交互
- `definition / instance / task` 详情仍偏向简单 `ElDialog`，没有形成统一的详情抽屉语义
- 设计器能力没有真正接入当前系统风格，只停留在“能录 JSON”的阶段

参考仓 `yudao-ui-admin-vue3-master` 已经具备：

- 表单设计器能力
- 流程设计器能力
- BPM 管理面完整模块

但本次设计不照搬它的页面壳层。参考仓只作为能力仓，不作为页面模板仓。

本设计同时依赖当前仓的前端页面规范：

- `docs/frontend-list-table-page-standard.md`
- `docs/frontend-edit-detail-page-standard.md`

并遵循 Karpathy 风格的 understanding-first 原则：

- 不复制参考仓外壳
- 先钉死边界，再讨论实现
- 先形成完整模块面，再追求深度能力
- 让第三方设计器成为隐藏内核，而不是页面主角

## 关联设计稿

当前设计稿聚焦 **前端管理端基座**。它是现有后端 BPM 底座设计稿的配套前端稿，不替代后端底座设计：

- `docs/superpowers/specs/2026-07-05-bpm-foundation-design.md`

两者边界如下：

- 后端稿关注：模块拆分、引擎内核、表结构、发布快照、运行投影
- 前端稿关注：信息架构、页面壳层、设计器适配、交互统一、验证路径

## 目标

本次 P0 目标是把当前 `hunyuan-system` 中的 BPM 管理端补齐为一个可复用、可扩展、符合现有系统风格的管理端基座。

P0 需要满足以下目标：

1. 以当前系统风格补齐 BPM 管理端 8 个交付块，而不是引入第二套页面体系
2. 完成管理端一期闭环：分类、表单、模型、定义、实例、任务、监听器、设计器
3. 保留当前 `ArtSearchPanel / ArtTablePanel / ArtEditPage / ArtEditSection` 作为主壳层
4. 让表单设计器与流程设计器成为内嵌能力，而不是新系统外壳
5. 让 `定义 / 实例 / 任务` 三个模块形成统一的右侧详情抽屉语义
6. 让表单和模型不再依赖手填 JSON 完成核心设计工作
7. 让第三方设计器依赖只沉到 adapter 层，不污染业务页
8. 保持与当前后端 `/bpm/*` contract 对齐，避免前端自造中间语义

## 非目标

P0 明确不做以下事情：

- 不新增业务侧发起端、员工端、移动端 BPM 页面
- 不新增独立 BPM 门户式子系统外壳
- 不把设计器做成一级菜单平级模块
- 不把所有底层 JSON 规则都图形化
- 不做高级流程分析、看板、大屏
- 不做定义版本 diff、流程图差异对比
- 不做监听器脚本执行台、可视化脚本编辑器
- 不重做后端 BPM 引擎内核和定义发布逻辑
- 不为了 BPM 单独造一套 UI 组件库或页面生成器

## 全局约束

- 遵循 `AGENTS.md`：一次只推进一个可验证增量
- 遵循 `AGENTS.md`：优先复用当前项目模式，不新增无必要依赖
- 前端列表页遵循 `docs/frontend-list-table-page-standard.md`
- 前端编辑页与详情页遵循 `docs/frontend-edit-detail-page-standard.md`
- 全部文本、文案、注释、设计稿内容使用 UTF-8 和中文
- 不复制 Yudao 的 `ContentWrap`、页面头、页面布局语义
- 不让页面直接访问 `fc-designer`、`bpmn-js`、`window.bpmnInstances`
- 第三方设计器依赖只允许存在于 BPM adapter 层
- 后端即使默认采用 Flowable 作为隐藏内核，前端页面也不暴露 Flowable 语义
- 不把当前薄壳页面抽象成“万能 BPM 页面生成器”

## 当前证据

### 当前前端已有真实 BPM 壳层

当前仓已存在：

- 列表页壳层：`category-list.vue`、`form-list.vue`、`model-list.vue`、`definition-list.vue`、`instance-list.vue`、`task-list.vue`、`listener-catalog.vue`
- 编辑页壳层：`model-editor.vue`
- 源码契约测试：`hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`

这说明当前工作不是“从零做 BPM 页面”，而是“把已存在的壳层补齐成完整可用能力面”。

### 当前页面规范已明确

仓库已经沉淀了两类强约束规范：

- 列表页使用 `ArtSearchPanel + ArtTablePanel + ArtTableHeader + ArtTable`
- 编辑页使用 `ArtEditPage + ArtEditSection`
- 主从详情场景优先 `列表 + 右侧抽屉`，而不是强制另起整页

因此 BPM 页面必须延续当前系统视觉和交互密度，而不是借设计器机会切换页面世界观。

### 参考仓是能力线索，不是外壳模板

Yudao 前端已具备关键能力来源：

- 表单设计器页：`src/views/bpm/form/editor/index.vue`
- 流程设计器页：`src/views/bpm/model/form/editor/index.vue`

参考仓同时暴露了潜在依赖线索：

- 表单设计器相关依赖
- `bpmn-js`
- `bpmn-js-properties-panel`

但这些只应作为能力来源和依赖线索，不应将其页面结构、`ContentWrap` 风格和页面语义整包带入当前仓。

## 方案比较

### 方案 A：整包迁移参考仓 BPM 页面

优点：

- 页面与设计器能力覆盖速度快
- 参考路径最直接

缺点：

- 会把当前仓拉入第二套页面风格
- `hunyuan-system` 原有页面壳层被破坏
- 权限、路由、抽屉语义、菜单语义容易分裂
- 设计器第三方细节会外溢到业务页

### 方案 B：只保留当前薄壳，不引入真实设计器

优点：

- 改动最小
- 依赖最少

缺点：

- 管理端仍然停留在“录 JSON”的阶段
- 不能称为完整 BPM 基座
- 表单、模型两个模块无法支撑真实使用

### 方案 C：壳层先行 + 能力适配器嵌入

优点：

- 最符合当前系统风格
- 既补齐模块面，也控制第三方依赖扩散
- 设计器能力可复用、可替换、可隐藏
- 后续可持续演进，而不是一次性拼装

缺点：

- 前期需要把页面壳层与 adapter contract 设计清楚
- 实现阶段需要同时关注 UI 一致性和依赖封装

### 推荐方案

推荐采用 **方案 C：壳层先行 + 能力适配器嵌入**。

一句话概括：

`把 Yudao 当作 BPM 能力仓，不当作页面模板仓。`

## 总体设计

### 设计主线

本次 P0 的主线是：

- 先补齐当前 `hunyuan-system` 下 BPM 管理端模块面
- 再把设计器能力以内嵌 adapter 的方式接进当前壳层
- 最终形成一个“看起来完全属于当前系统”的 BPM 管理端基座

### 交付形态

本次一期不是“8 个平级菜单”，而是“8 个交付块”：

1. 分类
2. 表单
3. 模型
4. 定义
5. 实例
6. 任务
7. 监听器
8. 设计器

其中：

- 前 7 个以管理菜单和页面体现
- 第 8 个设计器作为独立交付块体现，但不强制做一级菜单

### 页面分层

推荐采用三层结构：

1. 页面壳层
   - 负责列表、编辑页、抽屉页的当前系统风格
2. 设计器适配层
   - 负责第三方设计器集成、导入导出、脏状态、生命周期清理
3. 第三方能力内核层
   - 负责表单设计器、BPMN 设计器、属性面板等真实能力

核心原则：

`业务页只管业务壳和保存发布动作，第三方设计器细节全部藏进 adapter。`

## 信息架构

### 左侧菜单与能力入口

P0 推荐左侧菜单保持 7 个管理入口：

- 分类
- 表单
- 模型
- 定义
- 实例
- 任务
- 监听器

设计器不作为独立一级菜单，而采用能力入口方式：

- 从表单列表点击“设计”进入表单设计页
- 从模型列表点击“设计”进入流程设计页

这样做的原因：

- 避免形成双入口、双权限、双面包屑
- 更符合“基座能力内嵌”而不是“再造一套 BPM 子系统”
- 与当前系统管理端心智更一致

### 模块语义

- `分类`
  - 流程基础分类治理
- `表单`
  - BPM 表单管理与设计入口
- `模型`
  - 流程模型管理与设计入口
- `定义`
  - 已发布资产管理与查看
- `实例`
  - 运行态实例观测
- `任务`
  - 运行态任务观测与轻处理
- `监听器`
  - BPM 通知型监听器管理
- `设计器`
  - 表单设计器与流程设计器能力集合，不独立菜单化

## 一期范围边界

### In Scope

#### 分类

- 完整管理闭环：列表、查询、新增、编辑、状态管理

#### 监听器

- 完整管理闭环：列表、查询、新增、编辑、状态管理
- 一期只做管理面，不做复杂脚本编排

#### 表单

- 列表管理
- 元数据编辑
- 表单设计页
- 保存设计结果
- 基础预览能力

#### 模型

- 列表管理
- 元数据编辑
- 流程设计页
- 保存草稿
- 校验
- 模拟
- 发布

#### 定义

- 列表查询
- 右侧详情抽屉
- 查看部署信息、版本信息、流程图或 XML 快照、关联表单

#### 实例

- 列表查询
- 右侧详情抽屉
- 查看流程图、表单快照、变量、审批轨迹

#### 任务

- 列表查询
- 右侧详情抽屉
- 保留少量管理端轻操作

#### 设计器

- 交付 `BpmFormDesignerAdapter`
- 交付 `BpmProcessDesignerAdapter`
- 不强制独立一级菜单

### Out of Scope

- 员工端发起页、待办页、已办页
- 移动端 BPM 页面
- 流程分析、运营看板、大屏
- 高级 diff 与版本比较页面
- 监听器脚本执行台
- 第二套 BPM 专用 UI 组件库
- 所有 JSON 底层配置的可视化编排

## 页面设计

### 列表页统一规则

所有 BPM 普通管理页均遵循当前仓列表页标准：

- 外层：`Page`
- 搜索区：`ArtSearchPanel`
- 表格区：`ArtTablePanel + ArtTableHeader + ArtTable`
- 不额外增加重复菜单语义的页面标题大块
- 行操作保持当前仓紧凑按钮密度

适用页面：

- 分类
- 表单
- 模型
- 定义
- 实例
- 任务
- 监听器

### 详情展示统一规则

`定义 / 实例 / 任务` 三个模块统一采用：

- `列表 + 右侧详情抽屉`

不采用：

- 弹窗详情
- 独立整页详情

原因：

- 这三个模块的主要心智是“先定位，再查看”
- 管理员需要保留列表上下文
- 与当前仓主从详情场景风格一致

### 详情抽屉内部结构

详情抽屉统一采用：

- 顶部固定概要
- 中部 `Tab` 分面
- 底部动作区

不采用长滚动详情页，原因是 BPM 详情天然包含多视角信息：

- 基础信息
- 流程图
- 表单快照
- 变量
- 轨迹

使用 `Tab` 能让用户明确当前正在查看的维度，避免抽屉失控增长。

### 定义详情抽屉

推荐 Tab：

- `基础信息`
- `部署与版本`
- `流程图/XML`
- `关联表单`

默认只读。

### 实例详情抽屉

推荐 Tab：

- `基础信息`
- `流程图`
- `表单快照`
- `变量`
- `审批轨迹`

默认只读。

### 任务详情抽屉

推荐 Tab：

- `基础信息`
- `所属实例`
- `表单快照`
- `变量`
- `流转记录`

底部保留轻操作。

### 任务轻操作边界

P0 默认采用：

- `定义 / 实例` 抽屉只读
- `任务` 抽屉允许轻操作

原因：

- `定义` 更像发布资产
- `实例` 一期优先做观测面
- `任务` 天然带有待处理属性，如果完全只读，管理端价值偏弱

P0 推荐轻操作范围：

- 签收
- 委派
- 转办
- 完成

最终动作项以当前后端 contract 为准，不在前端先行扩义。

## 表单设计页设计

### 页面定位

表单设计页不是普通 CRUD 页，也不是独立低代码平台页。它是：

- 当前系统业务编辑页外壳
- 内嵌表单设计能力的重型工作页

### 页面骨架

推荐采用：

- 顶部：业务页头和动作区
- 上部：表单元数据区
- 下部：全宽设计器工作区

即：

- 使用 `Page`
- 使用 `ArtEditPage`
- 使用 `ArtEditSection` 管理元数据
- 用独立大工作区承载 `BpmFormDesignerAdapter`

### 页面内容

顶部动作：

- 返回
- 保存
- 预览

元数据区字段：

- 表单名称
- 表单编码
- 分类
- 状态
- 备注

工作区：

- `BpmFormDesignerAdapter`

### 交互原则

- 不再把 `schemaJson / layoutJson` 文本框当作主交互界面
- 文本 JSON 只是存储态，不是编辑态
- 保存时由页面统一从 adapter 拉取设计快照，再组装业务提交对象

## 流程设计页设计

### 页面定位

流程设计页是模型管理的重能力编辑页，不是独立 BPM 子系统页面。

### 页面骨架

推荐采用：

- 顶部：业务页头和动作区
- 上部：模型元数据区
- 中部：设计器工作区

页面外层继续使用当前系统编辑页壳层：

- `Page`
- `ArtEditPage`
- `ArtEditSection`

### 页面内容

顶部动作：

- 返回
- 保存草稿
- 校验
- 模拟
- 发布

元数据区字段：

- 模型名称
- 模型编码
- 分类
- 关联表单
- 版本状态

设计器工作区内部布局：

- 左侧 BPMN 画布
- 右侧属性面板

注意：

- 这是设计器工作区内部布局
- 不是整页套用 Yudao 的页面布局语义

### 交互原则

- 不再把 `simpleModelJson / startRuleJson / variableMappingJson` 文本框当作主交互界面
- 页面层保留“业务元数据 + 保存发布动作”
- 流程图设计、属性面板、导入导出、实例清理由 adapter 处理

## 设计器适配层

### 适配层定位

适配层的职责不是“包一层 UI”，而是正式拥有设计态 contract。

它需要负责：

- 第三方设计器实例创建与销毁
- 初始快照导入
- 设计快照导出
- 校验
- 脏状态跟踪
- 生命周期清理
- 设计器错误屏蔽与标准化上报

它不负责：

- 后端 API 调用
- 路由跳转
- 菜单权限
- 最终保存发布业务动作

### 文件建议

建议将 adapter 放在：

- `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/`

一期至少交付：

- `BpmFormDesignerAdapter`
- `BpmProcessDesignerAdapter`

### 表单设计器 Adapter Contract

页面传入：

- `readonly`
- `disabled`
- `initialSnapshot`

推荐对外能力：

- `load(snapshot)`
- `getSnapshot()`
- `validate()`
- `isDirty()`
- `resetDirty()`

推荐向页面抛出：

- `ready`
- `change`

原则：

- `change` 只输出标准化后的设计结果
- 不向页面暴露第三方实例对象

### 流程设计器 Adapter Contract

页面传入：

- `readonly`
- `disabled`
- `modelKey`
- `modelName`
- `initialSnapshot`
- `formContext`

推荐对外能力：

- `load(snapshot)`
- `getSnapshot()`
- `validate()`
- `simulate()`
- `isDirty()`
- `resetDirty()`

推荐向页面抛出：

- `ready`
- `change`
- `error`

原则：

- `formContext` 负责关联表单字段与变量候选等上下文输入
- adapter 只输出标准化设计结果，不直接保存发布

### 硬边界

必须明确写死以下规则：

- 页面不得直接访问 `fc-designer`
- 页面不得直接访问 `bpmn-js`
- 页面不得直接访问 `window.bpmnInstances`
- adapter 不直接调用保存、发布、校验后端接口
- 第三方依赖只允许出现在 adapter 及其内层能力代码

## 依赖策略

### 依赖原则

本次允许引入真实设计器所需的最小依赖集，但必须遵守：

- 只为“不能自己轻量实现”的核心能力引入依赖
- 依赖只服务于 adapter 层
- 不新增页面壳层库
- 不新增图标库
- 不新增第二套管理页框架

### 依赖预算

P0 只接受两类真实依赖：

1. 表单设计器所需依赖
2. 流程设计器与属性面板所需依赖

具体包名和版本以当前 `Vue 3 + Element Plus + 项目构建链` 兼容性验证后再落定，不在设计稿中硬编码引用参考仓版本。

### 依赖风险控制

- 所有依赖只进 `@hunyuan/system`，不扩散到全仓公共包
- 所有 adapter 必须处理销毁和全局实例清理
- 如果某个设计器依赖无法与当前构建链稳定兼容，P0 允许先落页面壳层与 contract，再将内核接入拆成后续实现批次

## 模块级页面设计摘要

### 分类

- 标准管理列表页
- 弹窗或侧面轻编辑均可
- 不需要独立详情页

### 监听器

- 标准管理列表页
- 一期先做通知型监听器配置管理
- 不引入脚本编排工作台

### 表单

- 列表页为主入口
- 行操作提供 `编辑 / 设计`
- `设计` 进入表单设计页

### 模型

- 列表页为主入口
- 行操作提供 `编辑 / 设计`
- `设计` 进入流程设计页

### 定义

- 标准列表页
- 行操作提供 `查看详情`
- 详情使用右侧 `Tab` 抽屉

### 实例

- 标准列表页
- 行操作提供 `查看详情`
- 详情使用右侧 `Tab` 抽屉

### 任务

- 标准列表页
- 行操作提供 `查看详情`
- 详情使用右侧 `Tab` 抽屉
- 抽屉底部允许轻操作

## 菜单与路由建议

### 一级菜单

P0 推荐保留当前 BPM 管理树中的 7 个主入口：

- 分类
- 表单
- 模型
- 定义
- 实例
- 任务
- 监听器

### 设计器路由

建议使用独立路由页，但不做独立一级菜单：

- `/system/bpm/form/designer`
- `/system/bpm/model/designer`

页面进入方式：

- 由 `表单` 列表行操作进入
- 由 `模型` 列表行操作进入

### 路由语义

- 设计器路由属于 `表单 / 模型` 的能力延伸
- 面包屑仍然回归 BPM 管理树
- 不单独造“设计器模块中心页”

## 验证与测试

### 源码契约验证

扩展或新增 BPM 模块源码契约测试，至少覆盖：

- BPM 真实页面存在，而不是桥接页
- 页面仍使用当前共享壳层组件
- 设计器页面存在真实本地视图文件
- API barrel 仍对齐 `/bpm/*` contract

建议基于现有：

- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`

继续扩展断言。

### Adapter 级验证

建议为 adapter 增加最小契约测试：

- 初始快照导入
- 脏状态变化
- 导出快照结构
- 生命周期销毁不报错

### 前端类型校验

必须执行：

```bash
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

并按当前仓前端壳层规范补充执行：

```bash
pnpm --dir hunyuan-design -F @vben/web-ele run typecheck
```

### 可视回归关注点

实现后必须重点检查：

- 列表页操作列按钮密度是否延续当前系统风格
- 详情抽屉是否保留列表上下文
- 设计器页是否仍然像当前系统编辑页，而不是像外来系统页面
- 设计器依赖加载后是否破坏页面滚动、高度、布局稳定性

## 风险与应对

### 风险 1：设计器依赖侵入业务页

症状：

- 页面直接持有第三方实例
- 页面保存逻辑与实例逻辑耦合

应对：

- 强制 adapter contract
- 页面只处理业务动作与标准快照

### 风险 2：页面风格被参考仓带偏

症状：

- 出现 `ContentWrap` 语义
- 出现另一套标题、卡片、间距体系

应对：

- 所有页面壳层以当前共享组件为准
- 只迁能力，不迁外壳

### 风险 3：详情抽屉失控增长

症状：

- 一个抽屉里堆满字段和文本框

应对：

- 统一 `Tab` 分面
- 按模块职责收紧视角

### 风险 4：前后端 contract 漂移

症状：

- 前端自造中间字段或页面语义

应对：

- 以前端 `/bpm/*` API contract 和后端真实接口为准
- 设计器 adapter 不自造业务保存语义

## 验收标准

本设计成立后，后续实现必须满足：

1. BPM 管理端以当前系统风格补齐完整管理面，而不是迁入第二套页面体系
2. `定义 / 实例 / 任务` 统一使用右侧 `Tab` 详情抽屉
3. `表单 / 模型` 不再依赖手填 JSON 完成核心设计工作
4. 设计器以 adapter 方式接入，第三方实例不外溢到业务页
5. 设计器不作为独立一级菜单，而作为 `表单 / 模型` 能力页交付
6. 设计稿定义的 8 个交付块在一期都能被访问和使用
7. 页面继续复用当前共享壳层和前端规范
8. 前端与后端 `/bpm/*` contract 保持一致，不引入额外页面语义漂移

## 实施顺序建议

推荐后续实现顺序如下：

1. 先补 BPM 前端设计契约测试，锁定真实页面与壳层约束
2. 优先补齐轻模块管理页：分类、监听器
3. 再补齐定义、实例、任务的统一右侧详情抽屉
4. 再收口表单列表和模型列表的页面交互
5. 落表单设计页壳层与 `BpmFormDesignerAdapter`
6. 落流程设计页壳层与 `BpmProcessDesignerAdapter`
7. 最后做设计器联调、抽屉回归、类型校验与路由回归

## 设计结论

本次 BPM 管理端基座设计的最终结论是：

- `当前系统风格壳层优先`
- `Yudao 能力内核内嵌`
- `设计器是独立交付块，不是独立一级菜单块`
- `第三方设计器依赖必须被 adapter 吞掉`

一句话总结：

`把 Yudao 当作 BPM 能力仓，不当作页面模板仓；把 BPM 设计器做成当前系统里的能力页，而不是外来系统页面。`
