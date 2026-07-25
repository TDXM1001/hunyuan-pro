# Hunyuan Pro

混元全栈管理项目，包含前端管理应用和后端模块化单体：

| 目录 | 说明 | 技术栈 |
| --- | --- | --- |
| [hunyuan-design](./hunyuan-design) | 正式管理端 `@hunyuan/system` 及 feature 工作区 | Vue 3 + Element Plus + Vite |
| [hunyuan-backend](./hunyuan-backend) | 管理端 API 与平台模块 | Java 17 + Spring Boot 3 |

## 快速开始

### 数据库

数据库结构和后续升级统一由
[Flyway 迁移链](./hunyuan-backend/hunyuan-admin/src/main/resources/db/migration/README.md)
管理。`数据库SQL脚本/mysql/hunyuan.sql` 只用于历史审计或恢复，不可作为新环境初始化脚本。

- 新环境先创建空的 MySQL 8.x 数据库，再设置 `HUNYUAN_FLYWAY_ENABLED=true` 启动后端；Flyway 会从当前结构基线依次执行迁移。
- 既有历史数据库启用 Flyway 前，必须先备份并确认结构已升级到 `3.64.0` 基线。
- 如需创建初始管理员，使用环境变量驱动的 bootstrap 流程，具体边界见
  [平台 Seed 与管理员 Bootstrap](./docs/architecture-baseline/10-platform-seed-bootstrap-and-access-audit.md)。

开发配置默认连接 `127.0.0.1:3306/hunyuan`。连接参数和凭据应按本机环境调整，不应写入迁移脚本。

### 后端

```powershell
cd hunyuan-backend
mvn clean install -DskipTests
cd hunyuan-admin
mvn spring-boot:run
```

后端开发服务默认监听 `http://localhost:1024`。

### 前端

```powershell
cd hunyuan-design
pnpm install
pnpm dev
```

`pnpm dev` 启动正式应用 `@hunyuan/system`，默认地址为 `http://localhost:5788`。上游 Element Plus 示例应用仍可通过 `pnpm dev:ele` 单独启动。
