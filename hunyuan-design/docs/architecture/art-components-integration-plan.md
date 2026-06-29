# Art Design Pro 组件引入方案

## 📋 背景

从 art-design-pro 项目引入优秀的表格组件及其他企业级组件到 hunyuan-design 基座中。

## 🎯 目标

1. 引入 art-design-pro 的表格系统（art-table + useTable）
2. 引入优秀的卡片、表单增强组件
3. 保持组件的独立性和可维护性
4. 适配 hunyuan-design 的技术栈（Vben + Element Plus）

## 🏗️ 架构设计

### 包结构

```
packages/
└── @vben/art-components/           # Art 组件适配层
    ├── src/
    │   ├── table/                  # 表格系统
    │   │   ├── components/
    │   │   │   ├── ArtTable/       # 表格组件
    │   │   │   │   ├── index.vue
    │   │   │   │   └── style.scss
    │   │   │   └── ArtTableHeader/
    │   │   ├── hooks/
    │   │   │   ├── useTable.ts     # 核心 Hook
    │   │   │   ├── useTableColumns.ts
    │   │   │   └── useTableHeight.ts
    │   │   └── utils/
    │   │       ├── tableCache.ts   # 缓存系统
    │   │       ├── tableConfig.ts
    │   │       └── tableUtils.ts
    │   ├── cards/                  # 卡片组件
    │   ├── forms/                  # 表单增强
    │   ├── charts/                 # 图表组件（可选）
    │   └── index.ts                # 统一导出
    ├── package.json
    ├── tsconfig.json
    └── README.md
```

### 依赖关系

```
apps/investment-system
    ↓ 引用
packages/@vben/art-components  (适配层)
    ↓ 依赖
packages/@vben/layouts
packages/@vben/styles
element-plus
```

## 📦 组件清单

### P0 - 必须引入（核心价值）

#### 1. 表格系统 ⭐⭐⭐⭐⭐

**组件：**
- `art-table` - 企业级表格组件
  - 支持 Element Plus Table 全部特性
  - 扩展分页、loading、高度自适应
  - 全局序号列、展开行
  - 移动端适配

**Hooks：**
- `useTable` - 表格数据管理核心
  - 自动 API 请求、分页控制
  - 智能缓存系统（5 种缓存策略）
  - 防抖搜索、参数管理
  - 5 种刷新策略（新增/更新/删除/手动/定时）
  - 列配置管理（显示/隐藏、排序、持久化）

**工具函数：**
- `tableCache.ts` - 智能缓存系统
- `tableUtils.ts` - 数据转换、错误处理
- `tableConfig.ts` - 全局配置

**价值：**
- 💰 节省 80% 的表格开发时间
- 🚀 提供生产级的缓存和优化方案
- 📱 开箱即用的移动端适配

#### 2. 表单增强

- `art-button-table` - 表格操作按钮组
- `art-button-more` - 更多操作下拉按钮

### P1 - 推荐引入（提升体验）

#### 3. 卡片组件

- `art-stats-card` - 统计数据卡片
- `art-data-list-card` - 数据列表卡片
- `art-progress-card` - 进度展示卡片

#### 4. 图表组件（按需）

- `art-bar-chart` - 柱状图
- `art-line-chart` - 折线图
- `art-ring-chart` - 环形图

### P2 - 按需引入

- `art-svg-icon` - SVG 图标
- `art-back-to-top` - 回到顶部

## 🔧 实施步骤

### Phase 1: 提取表格系统（3-5 天）

**Step 1.1: 创建包结构**
```bash
mkdir -p packages/@vben/art-components/src/table/{components,hooks,utils}
```

**Step 1.2: 复制文件**

从 `E:\my-project\huanyuan-pro-jichu\art-design-pro-main` 复制：

```
源文件 → 目标文件
-----------------------------------------------
src/components/core/tables/art-table/
  → packages/@vben/art-components/src/table/components/ArtTable/

src/hooks/core/useTable.ts
  → packages/@vben/art-components/src/table/hooks/useTable.ts

src/hooks/core/useTableColumns.ts
  → packages/@vben/art-components/src/table/hooks/useTableColumns.ts

src/hooks/core/useTableHeight.ts
  → packages/@vben/art-components/src/table/hooks/useTableHeight.ts

src/utils/table/*
  → packages/@vben/art-components/src/table/utils/
```

**Step 1.3: 适配依赖**

原项目依赖需要适配的部分：

| 原依赖 | 适配方案 |
|--------|---------|
| `@/types/component` | 创建 `packages/@vben/art-components/src/types/` |
| `@/store/modules/table` | 迁移到 `@vben/stores` 或内联到组件 |
| `@/hooks/core/useCommon` | 提取需要的部分到 `@vben/hooks` |

**Step 1.4: 创建 package.json**

```json
{
  "name": "@vben/art-components",
  "version": "1.0.0",
  "type": "module",
  "exports": {
    ".": "./src/index.ts",
    "./table": "./src/table/index.ts",
    "./hooks": "./src/table/hooks/index.ts"
  },
  "dependencies": {
    "@vueuse/core": "catalog:",
    "element-plus": "catalog:",
    "pinia": "catalog:",
    "vue": "catalog:"
  },
  "peerDependencies": {
    "@vben/hooks": "workspace:*",
    "@vben/stores": "workspace:*",
    "@vben/types": "workspace:*"
  }
}
```

**Step 1.5: 在 web-ele 中测试**

```vue
<!-- apps/web-ele/src/views/demos/table-demo.vue -->
<template>
  <ArtTable
    :data="data"
    :columns="columns"
    :loading="loading"
    :pagination="pagination"
    @pagination:current-change="handleCurrentChange"
    @pagination:size-change="handleSizeChange"
  />
</template>

<script setup lang="ts">
import { ArtTable } from '@vben/art-components/table'
import { useTable } from '@vben/art-components/hooks'
import { fetchUserList } from '#/api/user'

const {
  data,
  loading,
  pagination,
  handleCurrentChange,
  handleSizeChange
} = useTable({
  core: {
    apiFn: fetchUserList,
    columnsFactory: () => [
      { prop: 'name', label: '姓名' },
      { prop: 'email', label: '邮箱' }
    ]
  }
})
</script>
```

**成功标准：**
- ✅ 表格正常渲染
- ✅ 分页、排序、搜索功能正常
- ✅ 缓存系统工作正常
- ✅ 无 TypeScript 错误
- ✅ 构建通过（`pnpm build:ele`）

---

### Phase 2: 引入卡片组件（2-3 天）

复制卡片组件到 `packages/@vben/art-components/src/cards/`

---

### Phase 3: 集成到基座（1-2 天）

在 `packages/@vben/common-ui` 中重新导出：

```typescript
// packages/@vben/common-ui/src/index.ts
export * from '@vben/art-components/table'
export * from '@vben/art-components/cards'
```

这样应用只需要：
```typescript
import { ArtTable, useTable } from '@vben/common-ui'
```

---

## ⚠️ 潜在风险与解决方案

### 风险 1：类型定义不兼容

**问题：** art-design-pro 的类型定义与 Vben 的可能冲突

**解决：**
- 创建独立的类型文件 `@vben/art-components/src/types/`
- 使用 TypeScript 的命名空间隔离：
  ```typescript
  declare namespace ArtComponents {
    interface ColumnOption { ... }
  }
  ```

### 风险 2：Pinia Store 耦合

**问题：** `art-table` 依赖 `useTableStore`（表格全局配置）

**解决方案 A（推荐）：** 迁移 store 到 `@vben/stores`
```typescript
// packages/@vben/stores/src/modules/table.ts
export const useTableStore = defineStore('table', { ... })
```

**解决方案 B：** 组件内部自带 store
```typescript
// packages/@vben/art-components/src/table/stores/table.ts
export const useArtTableStore = defineStore('artTable', { ... })
```

### 风险 3：样式冲突

**问题：** art-design-pro 的 SCSS 变量可能与 Vben 冲突

**解决：**
- 将 art-design-pro 的样式变量映射到 Vben 的 CSS 变量
- 使用 CSS Modules 或 scoped 样式隔离

```scss
// packages/@vben/art-components/src/table/components/ArtTable/style.scss
.art-table {
  // 使用 Vben 的 CSS 变量
  --art-table-header-bg: var(--el-fill-color-lighter);
}
```

### 风险 4：Element Plus 版本差异

**问题：** 两个项目的 Element Plus 版本不同

**当前版本：**
- art-design-pro: `element-plus@2.11.2`
- hunyuan-design: `element-plus@catalog:` (需要查看 pnpm-workspace.yaml)

**解决：**
- 统一 Element Plus 版本到 catalog 中的版本
- 测试组件在当前版本下是否正常工作

---

## 📊 成本收益分析

### 开发成本

| 阶段 | 预估时间 | 关键风险 |
|------|----------|----------|
| Phase 1: 表格系统 | 3-5 天 | 类型适配、Store 迁移 |
| Phase 2: 卡片组件 | 2-3 天 | 样式适配 |
| Phase 3: 基座集成 | 1-2 天 | 构建配置 |
| **总计** | **6-10 天** | - |

### 收益

| 收益项 | 量化 |
|--------|------|
| 表格开发效率提升 | 节省 80% 时间 |
| 缓存优化 | 减少 60% API 请求 |
| 代码复用 | 避免重复开发 10+ 表格页面 |
| 用户体验 | 移动端适配、智能分页 |

**ROI：** 投入 6-10 天，未来每个表格页面节省 2-3 天开发时间。如果有 5+ 个表格页面，立即回本。

---

## 🎯 决策建议

### 立即执行（推荐）

**理由：**
1. ✅ 你有充足的时间（个人开发，无催促）
2. ✅ art-design-pro 的表格系统是生产级质量
3. ✅ 与你的"完善基座"目标完全一致
4. ✅ 两个项目都用 Element Plus，兼容性好

**行动：**
- 先做 Phase 1（表格系统），3-5 天可完成
- 在 web-ele 中验证可用后，再决定是否引入其他组件

### 备选方案：按需提取

如果担心引入太多：
1. 只提取 `useTable` Hook（不要组件）
2. 在你的项目中自己封装 `<VbenTable>` 组件
3. 使用 art-design-pro 的缓存、分页逻辑

---

## 📝 下一步

你需要决定：

**选项 A：** 我帮你创建 `packages/@vben/art-components` 的完整结构，包括 package.json、tsconfig.json、目录骨架

**选项 B：** 我直接开始复制表格系统文件，并适配依赖

**选项 C：** 先帮你分析 hunyuan-design 和 art-design-pro 的依赖差异，写详细的适配清单

**选项 D：** 只提取 `useTable` Hook，不引入组件（最小化引入）

你想选哪一个？
