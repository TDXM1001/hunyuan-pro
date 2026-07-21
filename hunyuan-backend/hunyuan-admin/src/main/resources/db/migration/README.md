# Flyway migration ownership

- `V3_64_0__current_schema_baseline.sql` is a schema-only snapshot of the phpStudy MySQL 8 `hunyuan` database after BPM retirement.
- Historical `hunyuan.sql` and `sql-update-log` files are audit/recovery inputs only; they are not replay-safe and must not be copied into the Flyway execution chain.
- Existing databases must be verified as already upgraded through `v3.64.0` before setting `HUNYUAN_FLYWAY_ENABLED=true` for the first time.
- New migrations are authored only in this directory, start after `V3_64_0`, and must never modify an applied file.
- Flyway placeholder replacement stays disabled so business `${...}` templates remain literal data.
- Flyway clean stays disabled in every application environment.
- `V3_65_0__platform_seed.sql` adds only reviewed non-sensitive platform configuration, dictionaries, real frontend menus, the `platform_admin` role, its grants/data scope, and the bootstrap audit table.
- Flyway never inserts an employee or credential. The initial administrator is created only when `HUNYUAN_BOOTSTRAP_ADMIN_ENABLED=true` and a strong `HUNYUAN_BOOTSTRAP_ADMIN_PASSWORD` is supplied.
- After the first successful login, remove the password environment variable and disable the administrator bootstrap switch.
- Existing databases must have duplicate config keys, login names, role-menu grants, role data scopes, and dictionary values resolved before applying `V3.65.0`; the migration fails rather than silently deleting conflicts.
