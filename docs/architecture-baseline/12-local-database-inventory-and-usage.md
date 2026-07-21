# 本机数据库清单与用途

## 1. 文档目的

本文集中记录本机 MySQL 实例中 `hunyuan%` 数据库的用途、使用边界和保留策略，避免将开发库、隔离测试库、迁移验收库和历史备份库混用。

本文中的数量和 Flyway 版本是 2026-07-21 的只读核查快照，会随后续迁移和测试执行发生变化。执行迁移、测试、备份恢复或清理前，必须重新查询实时状态，不能只依赖本文快照。

## 2. 环境关系

当前 dev 配置连接 `127.0.0.1:3306/hunyuan`。因此“开发库”和“本地隔离库”不是两台 MySQL 服务器，而是同一个本机 MySQL 实例中的不同数据库：

```text
本机 MySQL 127.0.0.1:3306
├── hunyuan                       当前开发运行库
├── hunyuan_a1_it                 默认集成测试隔离库
├── hunyuan_seed_it               首次空库迁移失败样本
├── hunyuan_seed2_it              空库迁移成功样本
├── hunyuan_bpm_backup_20260720    BPM 退役前历史备份
└── hunyuan_bpm_test               未被现行脚本引用的空测试库
```

当前应用运行、OpenAPI、前端联调和浏览器验收只使用 `hunyuan`。其他数据库不得作为 dev 运行库。

## 3. 数据库用途总表

| 数据库 | 分类 | 具体用途 | 当前应用连接 | 2026-07-21 实时状态 | 操作边界 |
| --- | --- | --- | --- | --- | --- |
| `hunyuan` | 开发运行库 | 日常开发、接口联调、浏览器验收和保留开发数据 | 是 | 48 张表，Flyway `3.66.1` | 不得由隔离测试重建或清空；迁移前先备份和预检 |
| `hunyuan_a1_it` | 集成测试隔离库 | 验证 Flyway、管理员 bootstrap、Redis 隔离和后端集成契约 | 否 | 48 张表，Flyway `3.66.0` | 只能通过 `_it` 测试入口使用；不能替代开发库 |
| `hunyuan_seed_it` | 迁移失败样本 | 保留第一次从空库验证平台 seed 时的失败现场 | 否 | 48 张表，Flyway `3.64.0` | 不在原库继续修补后宣称验收成功；只用于复盘和对照 |
| `hunyuan_seed2_it` | 迁移成功样本 | 在全新空库验证基线和平台 seed 可完整迁移 | 否 | 48 张表，Flyway `3.65.0` | 用于迁移链路回归；不承载日常开发数据 |
| `hunyuan_bpm_backup_20260720` | 历史备份库 | 保存 BPM/Flowable 退役前的旧表和数据，支持追溯与人工提取 | 否 | 86 张表，无 Flyway 历史表 | 只读保留；不得启动应用连接或执行新迁移 |
| `hunyuan_bpm_test` | 未确认的临时库 | 仓库没有找到现行创建或使用入口 | 否 | 0 张表，无 Flyway 历史表 | 删除前仍需确认创建者和外部用途，不能仅凭空库判断可删 |

## 4. 各数据库详细说明

### 4.1 `hunyuan`：当前开发运行库

dev 配置明确连接该库。后端启动在 1024 端口时，OpenAPI、前端联调、登录和业务验收产生的持久化数据都来自这里。

2026-07-21 核查状态：

- Flyway 已成功记录 `3.66.0` 和 `3.66.1`。
- A2 组织目录模块开关为 `true`。
- 组织目录菜单 `perms_type=1`，4 个 `organization.department.*` 能力已落库。
- 有 7 个部门、11 名员工、6 个角色和 157 条菜单记录。
- BPM/Flowable 相关的 `t_bpm_%`、`ACT_%`、`FLW_%` 表数量均为 0。

该库承载现有开发数据，不能视为自动化测试夹具。Flyway 在 dev 配置中默认关闭；需要接管迁移时必须显式启用，并在执行前完成备份、版本确认和数据预检。

### 4.2 `hunyuan_a1_it`：默认集成测试隔离库

`hunyuan-backend/scripts/run-integration-tests.ps1` 默认使用该库，并强制数据库名称以 `_it` 结尾。脚本不存在时创建数据库，然后运行：

- `FlywayMigrationTest`
- `InitialAdminBootstrapIntegrationTest`
- `RedisIsolationTest`

Redis 使用 DB 15 与日常开发缓存隔离。该库的数据应当最小化和可重复，不要求复制 `hunyuan` 的员工、角色或业务数据。

2026-07-21 核查时该库只迁移到 `3.66.0`，尚未执行 `3.66.1`。因此组织目录菜单存在，但 `perms_type` 仍为空；这表示它与开发库的 A2 修复版本尚未对齐，而不是开发库迁移失败。

### 4.3 `hunyuan_seed_it`：首次迁移失败样本

该库用于首次验证“全新空库能否只依靠 Flyway 建立当前平台结构与非敏感 seed”。首次兼容性验证遇到 MySQL 临时表限制后停止，数据库保留在当时状态，没有自动删除或在原库静默修复。

保留该库的价值是复盘失败条件和证明后续修复不是在污染后的数据库上得出的结果。它不是待继续使用的开发库，也不能作为迁移成功证据。

### 4.4 `hunyuan_seed2_it`：空库迁移成功样本

修复首次迁移问题后，系统使用全新的 `hunyuan_seed2_it` 重新验收，并成功从空库迁移到 `3.65.0`。该库证明平台基线、非敏感 seed、管理员 bootstrap 契约和菜单权限审计可以在干净环境成立。

该库属于迁移回归样本。后续验证最新 Flyway 全链路时应优先新建或清理合规的 `_it` 数据库重新执行，不能把 `seed2` 中的历史成功状态等同于最新版本已经验收。

### 4.5 `hunyuan_bpm_backup_20260720`：BPM 退役前历史备份

该库是 2026-07-20 BPM/Flowable 退役前的数据库内快照。2026-07-21 实时核查共有 86 张表，其中：

- `t_bpm_%`：47 张
- `ACT_%`：30 张
- `FLW_%`：0 张
- 其他表：9 张

当前 `hunyuan` 已不存在上述 BPM/Flowable 表，因此该备份用于历史追溯、清理结果对账和必要时人工提取旧数据。它没有 `flyway_schema_history`，不应被当前应用连接，也不应执行 `3.64.0` 之后的新迁移。

该数据库内备份与仓库外的 SQL 文件备份相互独立。保留或删除任何一种备份，都必须先确认审计、数据留存和恢复要求。

### 4.6 `hunyuan_bpm_test`：用途未确认的空测试库

实时核查显示该库没有任何表、数据或 Flyway 历史；仓库中也没有找到当前创建或连接它的脚本。名称表明它可能来自 BPM 清理期间的临时测试，但这只是推测，不能作为删除依据。

在确认创建者、外部脚本和留存要求前，将其标记为“未确认、未使用”，而不是“可直接删除”。

## 5. 使用与变更规则

1. 日常 dev 服务只连接 `hunyuan`，不得连接 `_it`、备份库或失败样本库。
2. 自动化集成测试只允许使用名称以 `_it` 结尾的隔离库，测试脚本不得指向 `hunyuan`。
3. `hunyuan` 执行 Flyway 前必须核对当前成功版本、目标迁移、备份和重复数据预检。
4. 空库迁移验收应使用全新的 `_it` 数据库；失败库保留现场，修复后使用新库复验。
5. 历史备份库默认只读，不运行应用、不执行 Flyway、不作为测试夹具。
6. 清理数据库前必须重新枚举连接配置、脚本引用和实时表数量，并获得针对准确库名的确认。
7. 文档中的版本与数量只代表核查快照；Flyway 历史和 `information_schema` 才是实时事实来源。

## 6. 只读核查清单

查看本机相关数据库：

```sql
SHOW DATABASES LIKE 'hunyuan%';
```

查看某个库最近的成功迁移：

```sql
SELECT version, description, script, checksum, installed_on
FROM flyway_schema_history
WHERE success = 1
ORDER BY installed_rank DESC;
```

核对 A2 两条迁移：

```sql
SELECT version, description, script, checksum, success, installed_on
FROM flyway_schema_history
WHERE version IN ('3.66.0', '3.66.1')
ORDER BY installed_rank;
```

核对数据库表数量以及 BPM/Flowable 残留：

```sql
SELECT
    table_schema,
    COUNT(*) AS table_count,
    SUM(table_name LIKE 't_bpm_%') AS bpm_table_count,
    SUM(table_name LIKE 'ACT\\_%') AS act_table_count,
    SUM(table_name LIKE 'FLW\\_%') AS flw_table_count
FROM information_schema.tables
WHERE table_schema LIKE 'hunyuan%'
GROUP BY table_schema
ORDER BY table_schema;
```

以上查询均为只读核查。是否执行迁移、恢复备份或删除数据库，必须作为独立操作明确确认。

## 7. 关联文档与入口

- dev 数据源与 Flyway 开关：`hunyuan-backend/hunyuan-base/src/main/resources/dev/hunyuan-base.yaml`
- 隔离测试入口：`hunyuan-backend/scripts/run-integration-tests.ps1`
- A0/A1 迁移映射：`09-current-state-migration-map.md`
- 平台 seed 与开发库接管：`10-platform-seed-bootstrap-and-access-audit.md`
- A2 组织目录迁移与验收：`11-a2-organization-directory-vertical-slice.md`
