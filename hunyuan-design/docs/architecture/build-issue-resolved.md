# 构建问题解决报告

> 完成时间：2026-06-24 23:58  
> 状态：✅ 完全解决

---

## 🎉 问题解决

### 原始问题

**错误信息：**
```
"Dayjs" is not exported by "dayjs/index.d.ts"
CommonJS dts modules cannot be reliably bundled by rolldown-plugin-dts
```

**根本原因：**
- tsdown 默认会尝试打包所有依赖的类型定义
- Element Plus 依赖 dayjs，而 dayjs@1.11.21 使用 CommonJS 导出
- rolldown-plugin-dts 无法正确处理 CommonJS 类型定义

---

## ✅ 解决方案

### 方案：配置 tsdown 的 neverBundle

在 `package.json` 中添加 tsdown 配置：

```json
{
  "tsdown": {
    "deps": {
      "neverBundle": ["element-plus", "dayjs", "@vue/reactivity"]
    }
  }
}
```

**含义：**
- `element-plus` - 不打包 Element Plus 的类型（作为外部依赖）
- `dayjs` - 不打包 dayjs 的类型
- `@vue/reactivity` - 不打包 Vue 响应式系统的类型

**效果：**
- ✅ 构建成功，无错误
- ✅ 无警告信息
- ✅ 生成完整的类型定义文件

---

## 📦 构建产物

### 生成的文件

```
dist/
├── index.mjs           32.81 KB  (主入口，ES Module)
├── index.mjs.map       70.98 KB  (源码映射)
├── index.d.mts          8.11 KB  (TypeScript 类型定义)
└── index.d.mts.map      2.95 KB  (类型定义源码映射)

总大小: 114.85 KB
```

### 类型定义验证

```typescript
// dist/index.d.mts 包含完整的类型导出
export { useTable, useTableColumns, useTableHeight } from './table/hooks';
export type { UseTableConfig } from './table/hooks';
export { CacheInvalidationStrategy } from './table/utils/tableCache';
export type { ApiResponse, CacheItem } from './table/utils/tableCache';
// ... 更多类型
```

---

## ✅ 最终配置

### package.json

```json
{
  "name": "@vben/art-hooks",
  "version": "1.0.0",
  "private": true,
  "type": "module",
  "exports": {
    ".": "./src/index.ts",
    "./table": "./src/table/hooks/index.ts"
  },
  "scripts": {
    "stub": "tsdown --dts"
  },
  "dependencies": {
    "@vueuse/core": "catalog:",
    "ohash": "catalog:",
    "vue": "catalog:"
  },
  "peerDependencies": {
    "@vben/stores": "workspace:*",
    "@vben/types": "workspace:*",
    "element-plus": "catalog:"
  },
  "devDependencies": {
    "@vben/tsconfig": "workspace:*",
    "typescript": "catalog:"
  },
  "tsdown": {
    "deps": {
      "neverBundle": ["element-plus", "dayjs", "@vue/reactivity"]
    }
  }
}
```

### tsconfig.json

```json
{
  "extends": "@vben/tsconfig/base.json",
  "compilerOptions": {
    "composite": true,
    "rootDir": "./src",
    "outDir": "./dist",
    "skipLibCheck": true,
    "declaration": true,
    "declarationMap": true
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "dist"]
}
```

---

## 🎯 验证清单

- [x] 构建成功（`pnpm run stub` 无错误）
- [x] 无警告信息
- [x] 生成 ESM 格式的 JS 文件
- [x] 生成完整的类型定义文件
- [x] 生成 source map
- [x] 包大小合理（114.85 KB）

---

## 📝 经验总结

### 关键点

1. **tsdown 需要明确的外部依赖配置**
   - 不是所有依赖都适合打包到输出中
   - UI 库（如 Element Plus）应该保持为外部依赖

2. **CommonJS 类型定义的问题**
   - dayjs 使用 CommonJS 导出
   - 现代打包工具（rolldown）可能无法正确处理
   - 解决方案：标记为外部依赖

3. **skipLibCheck 不够**
   - `skipLibCheck: true` 只跳过类型检查
   - 不影响打包过程
   - 需要配合 `neverBundle` 使用

### 推荐配置模式

对于任何基于 tsdown 的包，如果依赖了大型 UI 库，建议：

```json
{
  "tsdown": {
    "deps": {
      "neverBundle": [
        "vue",
        "element-plus", 
        "ant-design-vue",
        "naive-ui",
        // 其他 UI 库
        "dayjs",
        "moment",
        // 日期库
        "@vue/reactivity"
        // Vue 核心包
      ]
    }
  }
}
```

---

## 🚀 下一步

构建问题已完全解决，可以：

1. ✅ 在 web-ele 中使用 `@vben/art-hooks`
2. ✅ 创建测试页面验证功能
3. ✅ 开始使用 useTable 开发真实的表格页面

---

**状态：🎉 完全解决，可以投入使用！**
