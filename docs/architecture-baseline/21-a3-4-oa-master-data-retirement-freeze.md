# A3.4 OA 主数据历史子域退役执行与关闭台账

## 1. 状态与范围

本文件记录 A3.4 P2 的退役冻结边界、实施过程和最终关闭证据。
范围仅包括：

- OA 企业。
- OA 企业员工关联。
- OA 银行信息。
- OA 发票信息。

截至 2026-07-23，四个子域的产品决定均为 `RETIRE_APPROVED`，工程状态为
`P2_CLOSED`。

**本批已完成退役实施和全部关闭门禁。**

本轮已删除企业、企业员工关联、银行和发票的生产实现，并通过 `V3.75.0` 处理数据库
对象、菜单、权限和角色授权。OA 通知、平台 `message` 和 Demo 治理不属于本批，
分别留在 P3、P4，且本轮验收已证明这些保留对象未被误删。

## 2. Codebase Memory 与事实来源

冻结阶段使用 `codebase-memory-mcp` 项目 `E-my-project-hunyuan-pro-current` 复核
Controller、Route、Service、DAO、Mapper 和调用链，并与当前 checkout、迁移脚本及
退役前数据库交叉核对。

冻结结果：

| 子域 | Java 文件 | Mapper | 路由 | 主要内部依赖 |
| --- | ---: | --- | ---: | --- |
| 企业及员工关联 | 17 | `EnterpriseMapper.xml`、`EnterpriseEmployeeMapper.xml` | 11 | 员工公开 Facade、组织部门 Facade |
| 银行 | 8 | `BankMapper.xml` | 6 | `EnterpriseDao`、`DataTracerService` |
| 发票 | 8 | `InvoiceMapper.xml` | 6 | `EnterpriseService`、`DataTracerService` |

关闭阶段已刷新为项目
`E-my-project-hunyuan-pro-a3-4-p2-closed-20260723`，索引包含 13,231 个节点和
34,876 条关系。目标 Controller、Service、DAO 和 23 条旧 Route 均为 0；
`NoticeController` 和 OA 通知 Route 仍可检出。代码图结论已与源码、数据库、
OpenAPI、直接 HTTP 和构建产物交叉核对。

## 3. 源码与路由冻结矩阵

### 3.1 退役前源码范围

后续退役实现必须完整覆盖：

```text
hunyuan-backend/hunyuan-admin/src/main/java/com/hunyuan/sa/admin/module/business/oa/enterprise/
hunyuan-backend/hunyuan-admin/src/main/java/com/hunyuan/sa/admin/module/business/oa/bank/
hunyuan-backend/hunyuan-admin/src/main/java/com/hunyuan/sa/admin/module/business/oa/invoice/
hunyuan-backend/hunyuan-admin/src/main/resources/mapper/business/oa/enterprise/
hunyuan-backend/hunyuan-admin/src/main/resources/mapper/business/oa/bank/
hunyuan-backend/hunyuan-admin/src/main/resources/mapper/business/oa/invoice/
```

共享文件不得随目录批量删除，必须在消费者归零后窄改：

```text
hunyuan-backend/hunyuan-admin/src/main/java/com/hunyuan/sa/admin/constant/AdminSwaggerTagConst.java
hunyuan-backend/hunyuan-base/src/main/java/com/hunyuan/sa/base/module/support/datatracer/constant/DataTracerTypeEnum.java
```

### 3.2 路由冻结

企业及员工关联共 11 条：

```text
POST /oa/enterprise/page/query
POST /oa/enterprise/exportExcel
GET  /oa/enterprise/get/{enterpriseId}
POST /oa/enterprise/create
POST /oa/enterprise/update
GET  /oa/enterprise/delete/{enterpriseId}
GET  /oa/enterprise/query/list
POST /oa/enterprise/employee/add
POST /oa/enterprise/employee/list
POST /oa/enterprise/employee/queryPage
POST /oa/enterprise/employee/delete
```

银行共 6 条：

```text
POST /oa/bank/page/query
GET  /oa/bank/query/list/{enterpriseId}
GET  /oa/bank/get/{bankId}
POST /oa/bank/create
POST /oa/bank/update
GET  /oa/bank/delete/{bankId}
```

发票共 6 条：

```text
POST /oa/invoice/page/query
GET  /oa/invoice/get/{invoiceId}
POST /oa/invoice/create
POST /oa/invoice/update
GET  /invoice/delete/{invoiceId}
GET  /oa/invoice/query/list/{enterpriseId}
```

必须单独记录两个漏检风险：

1. `POST /oa/enterprise/exportExcel` 当前缺少显式 `@SaCheckPermission`，退役验收必须按
   路由检查，不能只按权限码反查。
2. `GET /invoice/delete/{invoiceId}` 不在 `/oa/**` 下，源码搜索、OpenAPI 对账和负向
   HTTP 验收必须单独包含该路径。

## 4. 数据与敏感信息处置

### 4.1 数据对象

| 数据对象 | 退役前数量 | 退役关系 |
| --- | ---: | --- |
| `t_oa_enterprise` | 2 | 企业主数据，最后删除 |
| `t_oa_enterprise_employee` | 2 | 企业与员工关系，先于企业主表删除 |
| `t_oa_bank` | 2 | 企业从属数据，先于企业主表删除 |
| `t_oa_invoice` | 1 | 企业从属数据，先于企业主表删除 |
| `t_data_tracer` 中 `type = 3` | 5 | OA 企业相关共享追踪残留 |

退役前数据库没有为这些 OA 表声明外键。原 `deleteEnterprise` 仅软删除企业记录，
不会级联处理银行、发票或企业员工关联，因此 `V3.75.0` 已显式按依赖顺序处理数据
和表。冻结阶段曾将字段记录为 `data_type`，实施核验后已纠正为真实字段 `type`。

### 4.2 备份与恢复

已生成退役前完整备份：

```text
数据库SQL脚本/mysql/backups/
hunyuan-before-a3-4-p2-oa-master-data-retirement-20260723-095825.sql
```

备份大小为 321,732 字节，SHA-256 为
`647207DC05E0B322CCA9DB8DB4B81CC38263BD22B3FF1D8A78051A729025A89D`。
备份 owner 为当前开发环境维护者，访问范围限制在本地工作区；至少保留到 A3.4
P3、P4 完成并由 owner 复核后再决定清理。

备份已完整恢复到隔离库 `hunyuan_a3_4_p2_restore_it`。Flyway 版本 `3.74.0`、
四张目标表记录数、`type = 3` 的 5 条追踪记录、18 个目标菜单节点和 18 条角色授权
均与退役前一致。文档、测试日志和提交记录未写入银行账号、纳税人识别号、联系人
电话、邮箱或营业执照等明文样本。

### 4.3 文件引用

恢复库中有 2 条企业记录包含 `enterprise_logo` 或 `business_license` 引用。P2 仅退役
OA 主数据表，没有调用文件删除能力；这些引用随数据库备份归档，对象存储文件继续由
平台文件能力保留。后续如需清理，必须由文件能力在备份保留期结束后单独完成引用审计，
本批不执行孤儿文件删除。

## 5. 菜单、权限与角色授权矩阵

退役前完整备份中，本批目标节点共 18 个。

页面节点：

| 菜单 ID | 名称 | 组件路径 |
| ---: | --- | --- |
| 144 | 企业管理 | `/oa/enterprise/enterprise-list` |
| 145 | 企业详情 | `/oa/enterprise/enterprise-detail` |

权限节点：

```text
oa:enterprise:query
oa:enterprise:add
oa:enterprise:update
oa:enterprise:delete
oa:enterprise:detail
oa:enterprise:queryEmployee
oa:enterprise:addEmployee
oa:enterprise:deleteEmployee
oa:bank:query
oa:bank:add
oa:bank:update
oa:bank:delete
oa:invoice:query
oa:invoice:add
oa:invoice:update
oa:invoice:delete
```

退役前上述 18 个节点均授权给角色 ID `1`（技术总监），目标角色授权共 18 条。
`V3.75.0` 先删除目标角色授权，再删除目标权限和页面节点，并验证无悬空授权。

父节点 `功能Demo`（`menu_id = 138`）还承载 OA 通知等非 P2 范围，本批禁止删除。
只有 P3 完成通知处置并确认子节点归零后，才能重新判断父节点是否可退役。

`V3.75.0` 执行后，18 个目标节点和 18 条目标角色授权均为 0；`menu_id = 138`
仍为 1，OA 通知相关菜单数量在开发库与恢复库中均为 8。

## 6. 消费者与共享残留

退役前仓库内生产消费者基本收敛在 OA Controller 和 OA 内部服务：

- `InvoiceService.createInvoice`、`InvoiceService.updateInvoice` 调用
  `EnterpriseService.getDetail` 校验企业。
- `BankService` 直接调用 `EnterpriseDao` 校验企业。
- 企业员工 Mapper 联表读取 `t_oa_enterprise`、`t_oa_enterprise_employee` 和
  `t_employee`。
- `EnterpriseService.queryPageEmployeeList` 调用
  `OrganizationDepartmentFacade.pathForCollaboration`。
- `EnterpriseEmployeeDao.deleteByEmployeeId` 当前没有入站调用者，属于随模块一起
  退役的未使用残留，不能据此扩大到员工模块。

以下共享残留已单独归零：

- `DataTracerTypeEnum.OA_ENTERPRISE`，枚举值为 `3`。
- `t_data_tracer.type = 3` 的历史追踪记录。
- `AdminSwaggerTagConst.Business.OA_ENTERPRISE`。
- `AdminSwaggerTagConst.Business.OA_BANK`。
- `AdminSwaggerTagConst.Business.OA_INVOICE`。

仓库内生产消费者经源码扫描和 Codebase Memory 复核均为 0。本次处置对象属于开发库
历史示例能力，当前没有正式外部集成登记，仓库外消费者按 `N/A` 记录；若后续发现
未登记集成，只能通过备份恢复或替代接口单独处理，不恢复旧路由。

## 7. 删除顺序

本次实施采用以下顺序：

```text
备份与恢复抽样
  -> 仓库内外消费者确认
  -> 角色授权
  -> 页面和权限节点
  -> 银行、发票代码与数据
  -> 企业员工关联代码与数据
  -> 企业主表代码与数据
  -> OA 专属 Swagger 常量、DataTracer 枚举和追踪残留
  -> 负向路由、OpenAPI、菜单、数据库和构建产物验收
  -> 刷新 Codebase Memory
```

银行和发票先于企业主表退役，企业员工关联也先于企业主表退役，实际执行顺序符合
冻结约束。

## 8. Flyway 执行结果

已新增并执行：

```text
hunyuan-backend/hunyuan-admin/src/main/resources/db/migration/
V3_75_0__a3_4_retire_oa_master_data.sql
```

迁移满足：

1. 使用新的递增版本，不修改已经执行的迁移。
2. 在删除菜单前先删除本批 18 条目标角色授权。
3. 精确删除 2 个页面节点和 16 个权限节点，不删除 `menu_id = 138`。
4. 显式清理四张 OA 表及 `t_data_tracer.type = 3`，不依赖级联。
5. 按 `t_oa_bank`、`t_oa_invoice`、`t_oa_enterprise_employee`、`t_oa_enterprise`
   的依赖方向删除。
6. 迁移前后对记录数、授权数、节点数和目标表存在性进行断言或审计。
7. 在全新隔离库和当前合规开发库分别验证，失败时不得以手工改库代替迁移修复。

迁移已在全新隔离库 `hunyuan_a3_4_p2_verify_it` 和当前开发库 `hunyuan` 通过，
最终版本均为 `3.75.0`。开发库对账结果为：四张目标表 0、`type = 3` 追踪 0、
目标菜单 0、目标角色授权 0、目标代码生成配置 0。

## 9. 测试与运行态验收矩阵

| 维度 | 关闭证据 |
| --- | --- |
| 备份 | 完整备份、SHA-256 和隔离库恢复对账通过 |
| 源码 | 33 个 Java、4 个 Mapper 退役；生产源码目标引用扫描为 0 |
| 编译 | `mvn clean verify` 成功；170 个测试，0 失败、0 错误、3 个环境测试跳过 |
| 数据库 | 全新隔离库和当前开发库 Flyway 均到 `3.75.0`，目标对象归零 |
| 权限 | 18 个目标节点、18 条角色授权归零 |
| 菜单 | 企业管理和企业详情归零；`menu_id = 138` 和 8 个 OA 通知菜单保留 |
| HTTP | 23 条旧路由全部返回 404，已覆盖导出接口和 `/invoice/delete/1` |
| OpenAPI | 文档返回 200；旧路径和旧标签均为 0，8 条 OA 通知路径保留 |
| 前端 | 仓库内旧客户端和组件引用为 0，动态菜单入口已删除 |
| 构建产物 | 干净 JAR 中目标 Controller、Service、DAO、Mapper 条目为 0，通知条目为 36 |
| 消费者 | 仓库内生产消费者为 0；仓库外正式消费者按 `N/A` 记录 |
| 代码图 | 新索引中目标 Controller、Service、DAO、Route 为 0，OA 通知仍存在 |
| 编码 | 改动文件严格 UTF-8 解码和 `git diff --check` 均通过 |

## 10. P2 关闭条件

以下关闭条件已经全部满足：

1. 数据备份、访问边界和恢复抽样有可审计证据。
2. 仓库内外消费者完成归零或事实性 `N/A` 判定。
3. 退役 Flyway 在空库和当前开发库通过。
4. OA 源码、Mapper、菜单、权限、角色授权和目标数据库对象达到冻结矩阵要求。
5. 23 条旧路由、OpenAPI、直接 URL 和干净构建产物完成负向验收。
6. 共享追踪枚举、Swagger 标签和 `type = 3` 数据完成处置。
7. Codebase Memory 刷新并与源码、数据库和运行态结果对账。
8. 严格 UTF-8、后端测试和 `git diff --check` 全部通过。

最终结论：

```text
A3.4 P2 = P2_CLOSED
OA 企业、企业员工关联、银行、发票 = RETIRED
OA 整体 = RETIRE_IN_PROGRESS
```

P2 关闭时 OA 通知仍留在 P3。后续 P3 已于 2026-07-23 按直接退役路径关闭，
OA 整体状态更新为 `RETIRE_CLOSED`；平台 `message` 和 Demo 工程资产边界保持不变。
P3 的独立关闭证据见
[22-a3-4-oa-notice-retirement-closeout.md](22-a3-4-oa-notice-retirement-closeout.md)。
