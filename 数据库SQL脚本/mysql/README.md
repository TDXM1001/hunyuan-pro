### 数据库脚本

默认数据库为 MySQL。这里的 `hunyuan.sql` 和 `sql-update-log` 从 A1 起只作为历史来源与人工恢复材料，不再是新版本迁移入口。

Flyway 的可执行迁移位于：

```text
hunyuan-backend/hunyuan-admin/src/main/resources/db/migration
```

#### 新部署

1. 创建空的 `hunyuan` 数据库和受限应用账号。
2. 设置 `HUNYUAN_FLYWAY_ENABLED=true`。
3. 启动应用，Flyway 会执行 `V3_64_0` 当前结构基线和后续迁移。

当前基线只包含表结构，不包含本机账号、密码、菜单、角色或业务数据。新环境的初始管理员与平台种子数据需要经过单独审查后通过 bootstrap/seed 机制提供。

#### 已有数据库接管

1. 先备份并确认数据库已经执行到 `v3.64.0`。
2. 设置 `HUNYUAN_FLYWAY_ENABLED=true`。
3. 首次启动只建立 `3.64.0` baseline；后续版本由 Flyway 顺序执行。

禁止修改已经进入 Flyway 历史表的迁移文件，也禁止重新执行本目录中的历史脚本修补生产库。
