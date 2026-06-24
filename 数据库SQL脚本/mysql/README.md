### 数据库脚本

默认数据库为 MySQL。

#### 第一次部署

首次部署请执行 `hunyuan.sql` 中的 SQL 语句，将创建名为 `hunyuan` 的数据库并初始化表结构与基础数据。

```bash
mysql -uroot -proot < hunyuan.sql
```

#### 更新

若从旧版本升级，请按版本顺序执行 `sql-update-log` 目录中的增量脚本。
