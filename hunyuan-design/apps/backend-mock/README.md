# @vben/backend-mock

## Description

Vben Admin 数据 Mock 服务，没有对接任何数据库，所有数据都是模拟的，仅用于前端开发、组件验收和自动化测试。它不是生产业务服务，也不得作为生产 API、生产菜单或正式业务导航的数据来源。

生产构建不会加载 Nitro Mock 插件；开发环境通过 `VITE_NITRO_MOCK=true` 显式开启，测试环境可以按测试需要单独启用。生产环境应关闭该开关并配置真实后端接口。

由于 `mock.js` 等工具有一些限制，比如上传文件不行、无法模拟复杂的逻辑等，所以这里使用了真实的后端服务来实现。该服务不需要手动启动，已经集成在 Vite 开发插件内。

## Running the app

```bash
# development
$ pnpm run start

# production mode
$ pnpm run build
```
