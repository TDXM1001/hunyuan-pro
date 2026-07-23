# 系统架构设计基线

## 1. 文档定位

本目录记录系统在进入具体业务设计之前形成的架构共识，作为后续需求分析、模块划分、详细设计和工程实现的共同基线。

这是一套目标架构说明，不是当前代码实现审计，也不表示文档中出现的抽象模块已经成为产品需求。文档使用“模块 A”“模块 B”“业务用例 X”等名称时，仅用于解释架构关系。

文档中的 `frontend/`、`backend/`、`module-a/` 等目录均为逻辑结构示意，不要求立即重命名现有目录、迁移现有模块或调整当前构建工程。涉及现有代码的改造必须先完成现状审计，再形成单独迁移方案。

当前讨论范围是：

- 本期只建设管理后台。
- 未来可能接入 H5、小程序、桌面端或其他客户端。
- 前端采用“应用 + 主包 + 子包”的模块化组织方式。
- 后端采用模块化单体，而不是直接拆分微服务。
- 技术基线以 Java 17、Spring Boot 3、Vue 3 和 MySQL 8.x 为主。

## 2. 阅读顺序

| 文档 | 说明 |
| --- | --- |
| [01-architecture-principles.md](01-architecture-principles.md) | 设计目标、原则、系统上下文和明确边界 |
| [02-module-model.md](02-module-model.md) | 应用、主包、子包、后端模块及启用机制 |
| [03-frontend-architecture.md](03-frontend-architecture.md) | 管理后台和未来多终端的前端架构 |
| [04-backend-architecture.md](04-backend-architecture.md) | 模块化单体、分层、事务和模块协作 |
| [05-data-api-security.md](05-data-api-security.md) | MySQL、Flyway、REST、OpenAPI、身份与权限 |
| [06-testing-and-quality.md](06-testing-and-quality.md) | 测试分层、架构守卫和验收边界 |
| [07-deployment-and-evolution.md](07-deployment-and-evolution.md) | 当前部署形态及未来演进触发条件 |
| [08-technology-decisions.md](08-technology-decisions.md) | 已接受、暂定、延后和当前不采用的决策 |
| [09-current-state-migration-map.md](09-current-state-migration-map.md) | 当前代码、数据和质量基线到目标架构的迁移映射 |
| [10-platform-seed-bootstrap-and-access-audit.md](10-platform-seed-bootstrap-and-access-audit.md) | 平台 seed、初始管理员 bootstrap 与菜单角色权限盘点 |
| [11-a2-organization-directory-vertical-slice.md](11-a2-organization-directory-vertical-slice.md) | A2 组织目录首个垂直切片的业务契约与验收边界 |
| [12-local-database-inventory-and-usage.md](12-local-database-inventory-and-usage.md) | 本机开发库、隔离测试库、迁移样本与历史备份的用途和操作边界 |
| [13-a2-1-organization-directory-compatibility-retirement.md](13-a2-1-organization-directory-compatibility-retirement.md) | A2.1 组织目录兼容入口退役、授权迁移与迁移链路收口 |
| [14-a3-platform-capability-migration-roadmap.md](14-a3-platform-capability-migration-roadmap.md) | A3 平台能力盘点、迁移顺序、A3.1 关闭结论与 A3.2 后续边界 |
| [15-a3-1-employee-contract-and-consumer-freeze.md](15-a3-1-employee-contract-and-consumer-freeze.md) | A3.1 冻结账本、P1-P4 执行记录与最终关闭证据 |
| [16-a3-2-access-contract-and-consumer-freeze.md](16-a3-2-access-contract-and-consumer-freeze.md) | A3.2 访问控制冻结账本、实施批次与执行记录 |
| [17-a3-3-position-contract-and-consumer-freeze.md](17-a3-3-position-contract-and-consumer-freeze.md) | A3.3 岗位目录 P0-P5 契约、后端、数据库、前端纵切、兼容退役执行记录与最终关闭证据 |
| [18-a3-4-platform-support-and-example-retirement.md](18-a3-4-platform-support-and-example-retirement.md) | A3.4 平台支持能力盘点边界与商品、分类历史示例第一批退役证据 |
| [19-a3-4-oa-adoption-audit.md](19-a3-4-oa-adoption-audit.md) | A3.4 OA 代码、数据、权限、消费者与运行态采用审计 |
| [20-a3-4-oa-and-demo-retirement-decision.md](20-a3-4-oa-and-demo-retirement-decision.md) | A3.4 OA、商品、分类及 Demo 的产品退役决定、依赖矩阵与执行批次 |
| [21-a3-4-oa-master-data-retirement-freeze.md](21-a3-4-oa-master-data-retirement-freeze.md) | A3.4 P2 企业、企业员工关联、银行和发票的退役冻结、实施记录与最终关闭证据 |
| [22-a3-4-oa-notice-retirement-closeout.md](22-a3-4-oa-notice-retirement-closeout.md) | A3.4 P3 OA 通知直接退役、数据备份、迁移、运行态与代码图关闭证据 |

## 3. 当前架构摘要

```text
当前管理后台
    ↓
Admin API (/api/admin/v1)
    ↓
应用服务与用例编排
    ↓
后端领域模块
    ↓
MySQL 8.x
```

未来增加客户端时，优先增加新的前端应用和接口适配层，复用已有应用服务与领域能力：

```text
管理后台 ──> Admin API  ──┐
H5       ──> App API    ──┤
小程序    ──> App API    ──┼─> 应用服务 -> 领域模块
桌面端    ──> Admin API  ──┤
外部系统  ──> Open API   ──┘
```

当前不会因为未来存在多终端可能性，就提前建设所有客户端、BFF、微前端、微服务或消息队列。

## 4. 文档使用规则

1. 业务模块必须从真实用户、任务、流程和数据所有权中识别，不从技术目录反推。
2. 新设计若违反本目录中的依赖方向、数据所有权或安全边界，应记录原因和替代方案。
3. 暂定决策在需求明确或实施前必须重新确认。
4. 架构文档描述边界与约束，业务详细设计应放入后续独立文档。
5. 当真实需求证明当前决策不再适用时，应更新决策记录，而不是在代码中静默绕过架构。
