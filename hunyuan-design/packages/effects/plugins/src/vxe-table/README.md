# VXE Table Plugin

基于 vxe-table 和 vxe-pc-ui 的表格组件插件。

## 导出

| 导出                  | 类型 | 说明           |
| --------------------- | ---- | -------------- |
| `setupHunyuanVxeTable`   | 函数 | 初始化配置函数 |
| `useHunyuanVxeGrid`      | 函数 | 表格组合式函数 |
| `HunyuanVxeGrid`         | 组件 | 表格组件       |
| `VxeTableGridColumns` | 类型 | 表格列类型     |
| `VxeTableGridOptions` | 类型 | 表格配置类型   |
| `VxeGridProps`        | 类型 | 表格 Props     |
| `VxeGridListeners`    | 类型 | 表格事件类型   |

## 使用

```ts
import {
  setupHunyuanVxeTable,
  useHunyuanVxeGrid,
  HunyuanVxeGrid,
} from '@hunyuan/plugins/vxe-table';
```

## 初始化

在应用入口处调用：

```ts
import { setupHunyuanVxeTable } from '@hunyuan/plugins/vxe-table';
import { useHunyuanForm } from '@hunyuan-core/form-ui';

setupHunyuanVxeTable({
  configVxeTable: (vxeUI) => {
    // 配置 VXE Table
  },
  useHunyuanForm,
});
```

## 类型

```ts
import type {
  VxeTableGridOptions,
  VxeGridProps,
} from '@hunyuan/plugins/vxe-table';
```
