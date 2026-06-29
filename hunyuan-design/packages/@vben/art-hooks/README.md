# @vben/art-hooks

> 从 art-design-pro 提取的企业级表格管理 Hook

## 特性

- ✅ 表格数据管理（useTable）
- ✅ 智能分页控制
- ✅ 防抖搜索
- ✅ 请求缓存系统
- ✅ 5 种刷新策略
- ✅ 列配置管理

## 使用

```typescript
import { useTable } from '@vben/art-hooks/table'

const {
  data,
  loading,
  pagination,
  handleCurrentChange,
  handleSizeChange
} = useTable({
  core: {
    apiFn: fetchUserList
  }
})
```

## 依赖

- Vue 3.5+
- @vueuse/core 14.3+
