<div align="center">
  <h1>混元管理前端</h1>
  <p>基于 Vue Vben Admin 5.x，使用 Element Plus 组件库</p>
</div>

**中文** | [English](./README.md)

## 简介

本项目为混元全栈管理系统的前端部分，基于 [Vue Vben Admin](https://github.com/vbenjs/vue-vben-admin) 精简而来，仅保留 **Element Plus** 版本（`apps/web-ele`）。

技术栈：Vue 3、Vite、TypeScript、Element Plus、Pinia、Vue Router。

## 安装使用

1. 安装依赖

```bash
cd hunyuan-design
npm i -g corepack
pnpm install
```

2. 开发运行

```bash
pnpm dev
```

默认端口 `5777`，Mock 服务默认开启（可在 `apps/web-ele/.env.development` 中配置 `VITE_NITRO_MOCK`）。

3. 打包

```bash
pnpm build
# 或
pnpm build:ele
```

构建产物位于 `apps/web-ele/dist/`。

## 项目结构

```
hunyuan-design/
├── apps/
│   ├── backend-mock/   # 开发 Mock 服务
│   └── web-ele/        # Element Plus 主应用
├── internal/           # 内部工具与配置
├── packages/           # 共享包
└── scripts/            # 脚本工具
```

## 常用命令

| 命令 | 说明 |
|------|------|
| `pnpm dev` | 启动 web-ele 开发服务 |
| `pnpm build` | 构建所有包及应用 |
| `pnpm build:ele` | 仅构建 web-ele |
| `pnpm lint` | 代码检查 |
| `pnpm check:type` | 类型检查 |

## 浏览器支持

支持现代浏览器（Chrome 111+、Firefox 128+、Safari 16.4+、Edge 最新两个版本），不支持 IE。

## 许可证

[MIT © Vben-2020](./LICENSE)
