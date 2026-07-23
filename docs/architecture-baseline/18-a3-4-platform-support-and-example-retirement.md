# A3.4 平台支持能力与示例模块退役

## 1. 状态与本批结论

截至 2026-07-22，A3.4 已完成 P1 商品与分类历史示例簇的消费者冻结、数据备份、
代码退役、权限与数据迁移及验证。本批只关闭商品、商品分类和自定义分组示例，不处理
OA、消息、帮助、反馈、定时任务、文件或安全能力。

本批结论：

- `business.goods` 和 `business.category` 只形成内部调用闭环，分类的唯一业务消费者是商品。
- 仓库内没有商品、分类或自定义分组的活跃前端页面、客户端或路由实现。
- 开发库迁移前有 6 条商品、12 条分类、19 个目标菜单或权限节点、11 条角色授权。
- `GOODS_PLACE` 及其 4 个字典项只服务商品示例，不属于平台稳定字典。
- 数据库不存在指向 `t_goods`、`t_category` 的外键，也没有其他表保存 `goods_id` 或
  `category_id` 列。
- 系统尚未投入生产且未开放正式仓库外集成，仓库外消费者审计为不适用（N/A）。
- `V3.74.0` 删除目标授权、菜单、示例字典、代码生成配置以及 `t_goods`、`t_category`。
- OA 与平台支持能力仍处于独立审计范围，不能因同属历史目录而连带删除。

## 2. Codebase Memory 与源码审计

本批先使用全量索引
`E-my-project-hunyuan-pro-a3-3-p5-closed-20260722` 执行架构、图谱和代码搜索；
退役完成后重建全量索引
`E-my-project-hunyuan-pro-a3-4-p1-goods-category-closed-20260722`。
关闭索引状态为 `ready`，包含 15,833 个节点和 39,603 条关系，Git HEAD 为
`684740bf3f91659796e0d5806c3e52777299c232`。

图谱和源码交叉核对结果：

- `GoodsController -> GoodsService -> GoodsDao` 是商品主链路。
- `GoodsService -> CategoryQueryService` 是商品对分类的唯一跨目录依赖。
- `CategoryController -> CategoryService -> CategoryDao` 是分类主链路。
- `CategoryCacheManager` 是 `AdminCacheConst.Category` 三个缓存键的唯一消费者。
- 除商品、分类源码和两个 Mapper 外，没有生产代码引用相关类型、表名或旧路由。
- 前端仓库没有 `/erp/goods/list`、`/erp/catalog/goods`、
  `/erp/catalog/custom` 对应组件。

Codebase Memory 用于定位调用簇和消费者，最终退役判断同时以当前源码、开发库、
Flyway、测试、HTTP 和 OpenAPI 结果为准。

## 3. 数据与菜单冻结

迁移前开发库状态：

| 对象 | 数量 | 处置 |
| --- | ---: | --- |
| `t_goods` | 6 | 已备份，随 `V3.74.0` 删除 |
| `t_category` | 12 | 已备份，随 `V3.74.0` 删除 |
| 商品与分类目标菜单或权限 | 19 | 删除授权后删除 |
| 目标角色授权 | 11 | 先于菜单删除 |
| `GOODS_PLACE` 字典 | 1 | 删除 |
| `GOODS_PLACE` 字典项 | 4 | 先于字典删除 |

目标菜单包括：

- 商品父目录 `/goods`。
- 商品页面 `/erp/goods/list`。
- 商品分类页面 `/erp/catalog/goods`。
- 自定义分组页面 `/erp/catalog/custom`。
- `goods:*`、`category:*` 和 `custom:category:*` 相关授权节点。

名称中带有 `addCategory` 的帮助文档目录权限不属于本批目标，迁移不得按模糊名称删除。

## 4. 备份与恢复边界

执行迁移前已生成完整开发库备份：

```text
数据库SQL脚本/mysql/backups/
hunyuan-before-a3-4-p1-goods-category-retirement-20260722.sql
```

备份采用 UTF-8、单事务并包含触发器、事件和存储过程。恢复只能在独立恢复库中演练，
不得覆盖当前开发库；数据库凭据不进入源码、文档、测试或日志。

备份校验：

- 文件大小：329,811 字节。
- SHA-256：`E35267A9785B409C971424CF86103B9DF6947AB7D8AAFA683D8F92C8393ED27C`。

## 5. 代码与迁移范围

本批删除：

- `module.business.goods` 全部生产源码。
- `module.business.category` 全部生产源码。
- 商品与分类 MyBatis Mapper。
- `AdminCacheConst.Category` 专属缓存常量。
- 旧商品、分类路由和权限节点。
- 商品示例字典与两张示例表。

本批保留：

- `V3.64.0` 历史基线中的原始表定义，历史迁移不可改写。
- OA 通知、企业等业务代码和菜单。
- 消息、帮助、反馈、任务、文件、安全等平台支持能力。
- 与 A3.3 及其他未提交工作相关的全部文件。

## 6. 关闭条件

A3.4 P1 只有同时满足以下条件才能关闭：

1. 完整 Maven reactor 与隔离 MySQL 迁移测试通过。
2. 开发库 Flyway 到达 `3.74.0`。
3. `t_goods`、`t_category`、目标菜单、权限、授权和 `GOODS_PLACE` 均归零。
4. 生产源码、Mapper 和构建产物中商品、分类实现归零。
5. 旧 `/goods/*`、`/category/*` 代表请求返回 HTTP 404。
6. OpenAPI 中旧商品、分类路径为 0。
7. UTF-8 严格检查通过。
8. 刷新 Codebase Memory 后，`GoodsController`、`CategoryController` 图节点均为 0。

截至 2026-07-22，以上关闭条件已全部通过：

| 门禁 | 验收结果 |
| --- | --- |
| 完整 Maven reactor | 3 个模块构建成功；155 项测试，0 失败、0 错误、3 项按环境条件跳过 |
| 独立 MySQL 迁移 | `hunyuan_a3_4_p1_it` 从空库迁移到 `3.71.0`，注入最小悬空样本后继续迁移到 `3.74.0`；1 项集成测试通过 |
| 开发库 Flyway | 当前版本 `3.74.0` |
| 数据库退役对象 | `t_goods`、`t_category`、目标菜单、目标角色授权、`GOODS_PLACE` 字典及字典项、目标代码生成配置均为 0 |
| 保留边界 | `support:helpDocCatalog:addCategory` 权限仍为 1；OA 菜单节点仍为 26 |
| HTTP | 12 条历史 `/goods/*`、`/category/*` 代表请求全部返回 404 |
| OpenAPI | 总路径 168；旧商品、分类路径 0；OA 匹配路径 35；支持域匹配路径 94 |
| 构建产物 | `GoodsController`、`CategoryController` 及两个历史业务包的 JAR 条目均为 0 |
| 源码与编码 | 生产源码旧包、旧路由和缓存常量无命中；A3.4 相关 Java、SQL、Markdown 均通过严格 UTF-8 解码；`git diff --check` 通过 |
| Codebase Memory | 关闭索引 `ready`；图搜索和 Cypher 反查中两个旧 Controller 均为 0 |

仓库中的历史数据库快照仍保留 `GOODS_PLACE` 原始记录，用于历史追溯；空库 Flyway
验收已证明该历史数据会在 `V3.74.0` 被删除，不会出现在最新运行态。

## 7. 后续边界

A3.4 下一批继续逐项盘点平台支持能力的 owner、数据、入口、权限和运行责任。
OA 必须单独完成真实采用审计；在消费者、数据和业务价值没有形成证据前，不扩建也不退役。
