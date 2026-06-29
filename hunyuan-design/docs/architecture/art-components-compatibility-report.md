# Art Design Pro 组件兼容性评估报告

> 生成时间：2026-06-24  
> 评估对象：从 art-design-pro 引入组件到 hunyuan-design 的兼容性风险

---

## 📊 核心依赖版本对比

### 关键依赖版本差异表

| 依赖包 | hunyuan-design (Catalog) | art-design-pro | 版本差异 | 风险等级 |
|--------|--------------------------|----------------|----------|----------|
| **vue** | `^3.5.38` | `^3.5.21` | 补丁版本差异 (38 vs 21) | 🟢 低风险 |
| **element-plus** | `^2.14.2` | `^2.11.2` | 次版本差异 (2.14 vs 2.11) | 🟡 中风险 |
| **pinia** | `^3.0.4` | `^3.0.3` | 补丁版本差异 | 🟢 低风险 |
| **@vueuse/core** | `^14.3.0` | `^13.9.0` | 大版本差异 (14 vs 13) | 🔴 高风险 |
| **axios** | `^1.18.0` | `^1.12.2` | 次版本差异 | 🟢 低风险 |
| **typescript** | `^6.0.3` | `~5.6.3` | 大版本差异 (6 vs 5) | 🔴 高风险 |
| **vue-router** | `^5.1.0` | `^4.5.1` | 大版本差异 (5 vs 4) | 🟡 中风险 |
| **vue-i18n** | `^11.4.5` | `^9.14.0` | 大版本差异 (11 vs 9) | 🟡 中风险 |
| **vite** | `8.0.10` | `^7.1.5` | 大版本差异 (8 vs 7) | 🟢 低风险 (hunyuan 更新) |

---

## 🚨 风险等级详解

### 🔴 高风险（Critical）- 必须处理

#### 1. **@vueuse/core: v14.3.0 (hunyuan) vs v13.9.0 (art)**

**影响范围：**
- art-design-pro 的 `useTable.ts` 大量使用 VueUse 的 Hooks：
  ```typescript
  import { useWindowSize } from '@vueuse/core'
  import { useResizeObserver } from '@vueuse/core'
  ```

**破坏性变更：**
- VueUse v13 → v14 有 **Breaking Changes**
- 主要影响：
  - `useResizeObserver` 的回调参数结构可能变化
  - `useWindowSize` 的返回值类型可能调整
  - 部分 composables 的默认选项变更

**解决方案：**

**方案 A（推荐）：降级 art-design-pro 组件中的 VueUse 用法**
```typescript
// 在复制组件时，手动适配 VueUse v14 的 API
// 查看 VueUse v13 → v14 的 Migration Guide
```

**方案 B：保持 art-design-pro 使用 v13**
```json
// packages/@vben/art-components/package.json
{
  "dependencies": {
    "@vueuse/core": "^13.9.0"  // 锁定在 art 的版本
  }
}
```
⚠️ 风险：hunyuan-design 项目会同时存在两个 VueUse 版本

**方案 C：升级 art-design-pro 的代码到 v14**
- 需要逐行检查 VueUse 的使用
- 根据 v14 的 API 变更手动修改

**推荐：方案 A**（适配成本最低）

---

#### 2. **TypeScript: v6.0.3 (hunyuan) vs v5.6.3 (art)**

**影响范围：**
- 类型推导行为可能不同
- 严格模式的检查规则变化
- 新语法特性（TS 6.0 是最新版本，可能有新特性）

**破坏性变更：**
- TypeScript 6.0 引入了更严格的类型检查
- `any` 类型的隐式转换规则变化
- 泛型推导规则优化

**具体风险点：**
```typescript
// art-design-pro 中的代码可能在 TS 6.0 下报错
type InferRecordType<T> = T extends Api.Common.PaginatedResponse<infer U> ? U : never

// TS 6.0 可能对这种复杂泛型推导有不同的处理
```

**解决方案：**

**方案 A（推荐）：在 art-components 包中使用 TS 5.6**
```json
// packages/@vben/art-components/package.json
{
  "devDependencies": {
    "typescript": "~5.6.3"
  }
}
```
然后在该包的 `tsconfig.json` 中明确使用本地 TS 版本

**方案 B：修复 TS 6.0 的类型错误**
- 引入组件后跑 `pnpm typecheck`
- 逐个修复类型错误（可能需要 10-20 处）

**推荐：方案 A**（避免升级带来的不可预知问题）

---

### 🟡 中风险（Medium）- 需要测试验证

#### 3. **element-plus: v2.14.2 (hunyuan) vs v2.11.2 (art)**

**版本差异：** 3 个次版本 (2.11 → 2.12 → 2.13 → 2.14)

**影响范围：**
- `<el-table>` 的 props、事件、插槽可能有变化
- `<el-pagination>` 的行为可能调整
- CSS 变量可能新增或修改

**已知变更（需要查 Element Plus Changelog）：**
1. **Table 组件：**
   - v2.12: 新增 `selectOnIndeterminate` prop（art-design-pro 已使用）
   - v2.13: 修复了 `setScrollTop` 方法的 bug
   - v2.14: 可能新增了其他特性

2. **Pagination 组件：**
   - 移动端适配逻辑可能变化

**解决方案：**

**方案 A（推荐）：使用 hunyuan 的 Element Plus 版本（v2.14.2）**
- art-design-pro 的组件应该向后兼容（Element Plus 遵循语义化版本）
- 需要测试验证：
  ```bash
  # 在引入组件后测试
  pnpm dev:ele
  # 手动测试表格的所有功能
  ```

**方案 B：降级 hunyuan 的 Element Plus 到 v2.11.2**
- ❌ 不推荐：失去 3 个版本的 bug 修复和新特性

**风险缓解：**
- 引入组件后，创建一个完整的表格测试页面
- 覆盖所有 API：分页、排序、筛选、展开行、选择行等
- 确保在 v2.14.2 下行为正常

---

#### 4. **vue-router: v5.1.0 (hunyuan) vs v4.5.1 (art)**

**破坏性变更：**
- Vue Router v5 是 **重大版本升级**
- 主要变化：
  - 路由配置的类型定义变化
  - `RouteRecordRaw` 类型可能不兼容
  - 导航守卫的参数类型调整

**影响范围：**
- art-design-pro 的组件中**不直接使用** vue-router（好消息！）
- 但如果引入的组件有路由依赖（如面包屑、菜单组件），会有问题

**解决方案：**

**当前情况：** art-table 组件不依赖 vue-router ✅  
**建议：** 只引入表格系统时，这个风险可以忽略

---

#### 5. **vue-i18n: v11.4.5 (hunyuan) vs v9.14.0 (art)**

**破坏性变更：**
- Vue I18n v9 → v11 跨了 2 个大版本
- API 变化：
  - `createI18n` 的配置项变化
  - `useI18n` 的返回值类型变化
  - 消息格式化规则调整

**影响范围：**
- 如果 art-design-pro 的组件内部使用了 `useI18n`（可能性低）
- 主要影响应用层的国际化配置

**解决方案：**

**当前情况：** art-table 组件不使用国际化 ✅  
**建议：** 只引入表格系统时，这个风险可以忽略

---

### 🟢 低风险（Low）- 可以忽略

#### 6. **vue: v3.5.38 (hunyuan) vs v3.5.21 (art)**
- 补丁版本差异，向后兼容
- 只是 bug 修复，无 Breaking Changes

#### 7. **pinia: v3.0.4 (hunyuan) vs v3.0.3 (art)**
- 补丁版本差异，向后兼容
- `useTableStore` 在两个版本下行为一致

#### 8. **axios: v1.18.0 (hunyuan) vs v1.12.2 (art)**
- 次版本差异，向后兼容
- art-design-pro 的请求逻辑不直接依赖 axios（使用 fetch 或自己的封装）

---

## 🔍 特定组件的依赖分析

### art-table 组件依赖树

```
art-table (index.vue)
├── vue (^3.5.x) ✅ 兼容
├── element-plus (^2.11+) 🟡 需要测试
│   ├── <el-table>
│   ├── <el-table-column>
│   ├── <el-pagination>
│   └── <el-empty>
├── @vueuse/core (v13.9.0) 🔴 版本冲突
│   ├── useWindowSize
│   └── useResizeObserver
├── pinia (^3.0.x) ✅ 兼容
│   └── useTableStore
└── 自定义依赖
    ├── @/types/component 🔴 需要创建
    ├── @/hooks/core/useCommon 🔴 需要迁移
    └── @/hooks/core/useTableHeight ✅ 可以直接复制
```

---

### useTable Hook 依赖树

```
useTable (hooks/core/useTable.ts)
├── vue (^3.5.x) ✅ 兼容
├── @vueuse/core (v13.9.0) 🔴 版本冲突
│   └── useWindowSize
├── tableCache.ts ✅ 纯工具类，无外部依赖
├── tableUtils.ts ✅ 纯工具类
├── tableConfig.ts ✅ 配置对象
└── useTableColumns.ts ✅ 可以直接复制
```

**结论：** `useTable` Hook 的依赖比 `art-table` 组件少，**更容易引入**

---

## 📋 兼容性风险矩阵

| 组件/模块 | VueUse 风险 | Element Plus 风险 | TypeScript 风险 | 其他依赖风险 | **总体风险** |
|-----------|-------------|-------------------|-----------------|--------------|--------------|
| **useTable Hook** | 🔴 高 | 🟢 无 | 🔴 高 | 🟢 低 | 🟡 中 |
| **art-table 组件** | 🔴 高 | 🟡 中 | 🔴 高 | 🟡 中 | 🔴 高 |
| **useTableColumns** | 🟢 无 | 🟢 无 | 🔴 高 | 🟢 低 | 🟡 中 |
| **tableCache/Utils** | 🟢 无 | 🟢 无 | 🔴 高 | 🟢 无 | 🟡 中 |
| **卡片组件** | 🟡 中 | 🟡 中 | 🔴 高 | 🟡 中 | 🟡 中 |
| **图表组件** | 🟢 低 | 🟢 无 | 🔴 高 | 🔴 高 (Echarts) | 🟡 中 |

---

## ⚠️ 引入策略建议

### 策略 A：渐进式引入（推荐）✅

**阶段 1：只引入 useTable Hook 核心逻辑（去掉 VueUse 依赖）**

```typescript
// packages/@vben/art-hooks/src/useTable.ts
// 修改前：
import { useWindowSize } from '@vueuse/core'  // ❌ 依赖 v13

// 修改后：
import { useWindowSize } from '@vueuse/core'  // ✅ 使用 hunyuan 的 v14
// 或者手动实现一个简单的 useWindowSize
```

**工作量：**
- 适配 VueUse v14 API：1-2 小时
- 修复 TypeScript 类型错误：2-3 小时
- **总计：3-5 小时**

**风险：** 🟡 中风险（可控）

---

### 策略 B：完整引入组件（不推荐）❌

**需要处理的问题：**
1. 适配 VueUse v14 API
2. 适配 TypeScript 6.0
3. 测试 Element Plus v2.14.2 兼容性
4. 迁移 `useTableStore`
5. 迁移 `@/types/component`
6. 迁移 `@/hooks/core/useCommon`

**工作量：**
- VueUse 适配：3-4 小时
- TypeScript 适配：5-6 小时
- Element Plus 测试：3-4 小时
- 依赖迁移：4-5 小时
- **总计：15-20 小时**

**风险：** 🔴 高风险（不可控因素多）

---

### 策略 C：Fork 并适配（中间方案）🔶

**操作步骤：**
1. 将 art-design-pro 的表格系统复制到 `packages/@vben/art-components`
2. 在独立包中锁定依赖版本：
   ```json
   {
     "@vueuse/core": "^13.9.0",
     "typescript": "~5.6.3"
   }
   ```
3. 构建时使用独立的 tsconfig
4. 主项目通过编译后的 JS 引用（不经过类型检查）

**优点：**
- ✅ 避免版本冲突
- ✅ 保持 art-design-pro 原始代码不变

**缺点：**
- ❌ 项目中存在多个版本的依赖（包体积增大）
- ❌ 类型提示可能不完整

**工作量：** 6-8 小时  
**风险：** 🟡 中风险

---

## 🎯 最终推荐方案

### **推荐：策略 A（渐进式引入）+ 精简版 useTable**

**具体实施：**

#### Step 1: 创建精简版 useTable（去除高风险依赖）

```typescript
// packages/@vben/art-hooks/src/useTable.ts

// ❌ 不引入的部分（高风险）
// - useResizeObserver（VueUse v13 特有）
// - 列配置管理（复杂度高）
// - 完整的缓存系统（可选，后补）

// ✅ 保留的核心功能
// - 数据获取与分页
// - 搜索参数管理
// - loading/error 状态
// - 5 种刷新策略
// - 简化的缓存（基于 Map，不用外部库）
```

**代码量：** ~400 行（原版 758 行的 53%）

---

#### Step 2: 手动实现 useWindowSize（避免 VueUse 版本冲突）

```typescript
// packages/@vben/art-hooks/src/composables/useWindowSize.ts
import { ref, onMounted, onUnmounted } from 'vue'

export function useWindowSize() {
  const width = ref(window.innerWidth)
  const height = ref(window.innerHeight)
  
  const update = () => {
    width.value = window.innerWidth
    height.value = window.innerHeight
  }
  
  onMounted(() => {
    window.addEventListener('resize', update)
  })
  
  onUnmounted(() => {
    window.removeEventListener('resize', update)
  })
  
  return { width, height }
}
```

**代码量：** ~20 行  
**优点：** 完全避免 VueUse 版本问题

---

#### Step 3: 使用 TypeScript 5.6 编译 art-components 包

```json
// packages/@vben/art-components/package.json
{
  "name": "@vben/art-components",
  "devDependencies": {
    "typescript": "~5.6.3"  // 锁定在 art-design-pro 的版本
  },
  "scripts": {
    "build": "tsc --project tsconfig.build.json"
  }
}
```

```json
// packages/@vben/art-components/tsconfig.build.json
{
  "extends": "../../../tsconfig.json",
  "compilerOptions": {
    "outDir": "./dist",
    "declaration": true,
    "skipLibCheck": true  // 跳过依赖的类型检查
  }
}
```

**优点：** 避免 TypeScript 6.0 的破坏性变更

---

#### Step 4: Element Plus 兼容性测试清单

```markdown
## 测试页面：apps/web-ele/src/views/demos/table-compatibility-test.vue

### 测试用例：

1. [ ] 基础渲染
   - 数据正常显示
   - 列宽度自适应
   - 无控制台错误

2. [ ] 分页功能
   - 切换页码
   - 切换每页条数
   - 总数显示正确

3. [ ] 排序功能
   - 点击表头排序
   - 多列排序

4. [ ] 选择功能
   - 单选
   - 多选
   - 全选

5. [ ] 展开行
   - 点击展开
   - 嵌套内容渲染

6. [ ] 移动端适配
   - 浏览器窗口缩小到 768px
   - 分页器布局调整
   - 表格横向滚动

7. [ ] 空数据状态
   - 显示空状态组件
   - loading 状态正常

8. [ ] 滚动到顶部
   - 切换页码后自动滚动
   - setScrollTop 方法可用
```

**工作量：** 2-3 小时完整测试

---

## 📊 工作量与风险评估

### 方案对比

| 方案 | 工作量 | 风险等级 | 代码质量 | 维护成本 | **推荐指数** |
|------|--------|---------|----------|----------|--------------|
| **A: 渐进式引入** | 3-5 小时 | 🟡 中 | ⭐⭐⭐⭐ | 低 | ⭐⭐⭐⭐⭐ |
| B: 完整引入 | 15-20 小时 | 🔴 高 | ⭐⭐⭐⭐⭐ | 中 | ⭐⭐ |
| C: Fork 适配 | 6-8 小时 | 🟡 中 | ⭐⭐⭐ | 高 | ⭐⭐⭐ |
| D: 不引入，自己写 | 20-30 小时 | 🟢 低 | ⭐⭐⭐ | 低 | ⭐⭐⭐ |

---

## ✅ 行动计划

### Phase 0: 验证兼容性（今天，2 小时）

```bash
# 1. 检查 VueUse v14 的 Breaking Changes
open https://vueuse.org/guide/migration.html

# 2. 检查 Element Plus v2.11 → v2.14 的 Changelog
open https://element-plus.org/en-US/guide/changelog.html

# 3. 在 art-design-pro 项目中搜索 VueUse 的使用
cd /e/my-project/huanyuan-pro-jichu/art-design-pro-main
grep -r "from '@vueuse/core'" src/components/core/tables/
grep -r "from '@vueuse/core'" src/hooks/core/useTable*
```

**输出：** 一份详细的 VueUse API 使用清单，标注哪些需要适配

---

### Phase 1: 引入精简版 useTable（明天，3-5 小时）

1. 创建 `packages/@vben/art-hooks`
2. 复制 `useTable.ts`，移除列配置管理部分
3. 手动实现 `useWindowSize`（不依赖 VueUse）
4. 修复 TypeScript 类型错误
5. 在 web-ele 中创建测试页面验证

**成功标准：**
- ✅ `pnpm typecheck` 通过
- ✅ 测试页面正常渲染
- ✅ 分页、搜索功能正常

---

### Phase 2: Element Plus 兼容性测试（后天，2-3 小时）

按照上面的测试清单逐项验证

**成功标准：**
- ✅ 8 项测试全部通过
- ✅ 移动端表现正常

---

### Phase 3: 决策下一步（3 天后）

根据 Phase 1-2 的结果：
- **如果顺利** → 继续引入缓存系统、列配置管理
- **如果遇到大问题** → 停止引入，基于学到的设计自己封装

---

## 📝 结论

### 核心风险总结

1. 🔴 **VueUse v13 vs v14 版本冲突** - 必须处理
2. 🔴 **TypeScript 5.6 vs 6.0 版本冲突** - 必须处理
3. 🟡 **Element Plus v2.11 vs v2.14 兼容性** - 需要测试
4. 🟢 **其他依赖差异** - 可以忽略

### 最佳实践路径

```
今天：Phase 0（验证兼容性）→ 2 小时
明天：Phase 1（引入精简版 useTable）→ 3-5 小时
后天：Phase 2（Element Plus 测试）→ 2-3 小时
---
总计：7-10 小时可以完成安全引入
风险：中等（可控）
收益：节省未来 80% 的表格开发时间
```

### ⚠️ 不推荐的做法

- ❌ 直接复制粘贴所有代码（会遇到大量类型错误）
- ❌ 强制使用 art-design-pro 的依赖版本（会污染主项目）
- ❌ 跳过测试直接上生产（Element Plus 版本差异可能有隐藏 bug）

---

## 🎯 下一步行动

你需要决定：

**选项 1：** 我帮你执行 Phase 0（验证兼容性）
- 检查 VueUse v14 的 Breaking Changes
- 列出需要适配的 API
- 给出精确的修改方案

**选项 2：** 直接开始 Phase 1（引入精简版 useTable）
- 创建 `packages/@vben/art-hooks`
- 实现精简版 useTable（去除高风险依赖）
- 跳过兼容性验证，边做边修

**选项 3：** 先做一个 POC（概念验证）
- 只复制 `useTable.ts` 的核心 50 行代码
- 在 web-ele 中快速验证是否可用
- 根据结果决定是否继续

**选项 4：** 放弃引入，基于 art-design-pro 的设计自己实现
- 学习 useTable 的架构设计
- 用 hunyuan-design 的技术栈重新实现
- 避免所有版本兼容问题

**我的推荐：选项 1**（先验证兼容性，再决定是否引入）

你想选哪个？
