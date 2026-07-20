# 后端架构

## 1. 架构形态

后端采用模块化单体：

- 一个主要 Spring Boot 应用。
- 当前统一构建和部署。
- 业务代码按真实领域模块隔离。
- 模块拥有自己的规则、用例、数据和公开接口。
- 当前使用同一 MySQL 实例和本地事务。

模块化单体不是“暂时不整理的单体”，而是具有明确边界和自动化依赖守卫的单体。

## 2. 总体分层

```text
bootstrap
interfaces
application
modules
shared-kernel
infrastructure
```

| 区域 | 职责 |
| --- | --- |
| `bootstrap` | 启动、配置和模块装配 |
| `interfaces` | HTTP、任务、事件消费等外部入口适配 |
| `application` | 系统级用例和跨模块编排 |
| `modules` | 真实业务领域模块 |
| `shared-kernel` | 极少量稳定公共概念 |
| `infrastructure` | 数据库、缓存、文件、外部系统等技术实现 |

未来增加终端时，优先增加接口适配层：

```text
interfaces/
├─ admin-api/
├─ app-api/
└─ open-api/
```

不同接口层可以返回不同查询模型，但必须复用同一套业务规则和应用用例。

## 3. 模块内部结构

```text
module-a/
├─ api/
├─ application/
├─ domain/
└─ infrastructure/
```

### 3.1 API 层

负责 Controller、请求参数校验、协议转换、HTTP 状态码和请求响应 DTO。

API 层不承载核心状态转换，不直接访问 Mapper 或 Repository。

### 3.2 Application 层

负责一个完整业务用例：

- 组织执行顺序。
- 建立事务边界。
- 调用领域对象和模块公开接口。
- 处理幂等性和用例级权限入口。
- 返回适合接口适配层使用的结果。

### 3.3 Domain 层

在业务确实存在规则时承载：

- 实体和值对象。
- 状态转换和不变量。
- 领域服务。
- Repository 接口。
- 领域事件。

领域层不依赖 Spring MVC、ORM、Redis 或具体外部服务。

### 3.4 Infrastructure 层

提供技术实现：

- Repository 和 Mapper 实现。
- MySQL 持久化。
- 缓存和文件存储。
- 外部服务客户端。
- 事件发布技术实现。

## 4. 查询与命令

不要求所有代码都使用复杂领域模型。

简单查询可以使用：

```text
Controller -> QueryService -> QueryRepository -> 查询模型
```

存在业务规则的写操作使用：

```text
Controller -> ApplicationService -> Domain -> Repository
```

原则是“查询保持简单，写操作保护规则，复杂度按需引入”。报表和聚合查询可以跨模块读取数据，但查询路径必须保持只读。

## 5. 模块公开接口

模块之间通过公开 Facade 或应用接口协作：

```text
允许：模块 B -> ModuleAFacade
禁止：模块 B -> ModuleARepository
禁止：模块 B -> ModuleAEntity
禁止：模块 B -> 模块 A 的数据表完成写操作
```

公开接口返回稳定数据结构，不暴露 ORM 实体和模块内部实现。

## 6. 跨模块用例编排

跨模块操作由业务用例拥有者编排：

```text
Interface
   ↓
UseCase Coordinator
   ├─ 调用模块 A 公开能力
   ├─ 调用模块 B 公开能力
   └─ 发布用例完成事件
```

编排可以位于发起模块应用层、独立流程应用层或系统级应用服务。不能建立一个无边界的万能 Service 接管所有模块协作。

## 7. 事务和事件

模块化单体阶段允许多个模块公开能力参与同一个本地数据库事务，但不允许共享 Repository。

事务内保留：

- 核心状态变更。
- 必须共同成功的业务记录。
- 关键关联关系。
- 待可靠发布的事件记录。

事务提交后执行：

- 短信、邮件和推送。
- 非关键报表更新。
- 搜索索引更新。
- 第三方通知。

初期可以使用事务提交后事件。只有出现可靠异步和故障恢复要求时，才引入 Outbox；只有进一步出现独立消费、削峰或服务解耦需求时，才评估消息队列。

## 8. Spring 工程约束

当前技术基线：

```text
Java 17
Spring Boot 3
Spring Security
Spring Validation
Spring Transaction
Spring Modulith 和/或 ArchUnit
JUnit 5
Testcontainers
```

Spring Modulith可用于表达模块、验证依赖和管理应用事件；ArchUnit可补充项目特定依赖规则。二者可以单独使用，也可以组合使用。

初期不要求每个领域都是独立 Maven 模块。可以先用 Java 包和架构测试建立边界，只有在编译隔离确实有价值时才拆分构建模块。

如果当前工程已经存在 Maven 子模块，本基线也不要求立即合并或重拆。应先确认现有子模块是部署边界、平台能力边界还是业务边界，再决定如何映射到目标模块模型。

## 9. 数据访问选择

MyBatis 与 JPA 的最终选择仍需结合真实业务：

- CRUD、复杂筛选、报表和 SQL 可控性优先时，倾向 MyBatis。
- 聚合规则、对象关系和领域写模型明显时，可以评估 JPA。

第一阶段不建议同时建立两套数据访问体系。如果采用 MyBatis-Plus，应限制在基础设施层，不能让业务层普遍继承万能 Service 并直接暴露数据库 CRUD。
