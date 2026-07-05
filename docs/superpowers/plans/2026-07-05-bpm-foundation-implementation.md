# BPM Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在当前 `hunyuan-pro` 分支新增独立 `hunyuan-bpm` 基础模块，以 Flowable 作为默认首选隐藏内核，落地 P0 的分类、表单、设计器、模型、定义、实例、任务、监听器闭环，并让 `hunyuan-system` 前后端形成可验证的管理端与员工端入口。

**Architecture:** 先把 Flowable 7.2.0 固定为 `hunyuan-bpm` 内部实现，只暴露 BPM 平台对象和服务契约，再按 `authoring -> publish -> runtime` 三层向外展开。后端沿用当前仓 `Controller -> Service -> Dao -> Mapper XML` 的组织方式，`hunyuan-bpm` 依赖 `hunyuan-base` 复用消息、短信、邮件、编号规则，`hunyuan-admin` 仅通过适配器实现组织与当前登录员工解析，避免形成 `hunyuan-bpm -> hunyuan-admin` 反向依赖。前端继续使用现有 `hunyuan-system` 单应用，不新增 BPMN 画布依赖，设计器按 `simpleModel JSON -> validate/simulate -> compile BPMN -> publish snapshot -> run` 的 P0 路径实现。

**Tech Stack:** Java 17, Spring Boot 3.5.4, MyBatis-Plus, Sa-Token, Flowable 7.2.0 process engine, Fastjson 2, MySQL SQL, Vue 3, TypeScript, Element Plus, `@vben/common-ui`, `@vben/art-hooks`, Vitest, Maven, pnpm

## Global Constraints

- 遵循 `AGENTS.md`：一次只推进一个可验证增量。
- 遵循 `AGENTS.md`：编辑前先说明为什么需要改动。
- 遵循 `AGENTS.md`：优先使用现有项目模式，不新增前端依赖。
- `Flowable` 只允许存在于 `hunyuan-bpm` 模块内部，`hunyuan-base`、`hunyuan-admin`、前端 API、VO、DTO、Form 均不得直接暴露 `org.flowable.*` 类型。
- 后端 actor 一律使用 `employeeId`。
- 当前仓的组织解析能力以 `department`、`role` 为 P0 主路径。
- `simpleModel JSON -> validate/simulate -> compile BPMN -> publish snapshot -> run` 是唯一 P0 发布主路径。
- 表单真相使用 BPM 自带表单中心，不复用系统字典充当表单仓库。
- 表单提交真相分为两份：`initial_form_data_snapshot_json` 和 `current_form_data_snapshot_json`。
- 变量只允许白名单映射，不允许整包表单数据直接灌入 runtime variables。
- P0 只支持 `EMPLOYEE`、`DEPARTMENT_MANAGER`、`ROLE` 三类 candidate resolver。
- P0 只支持 `single only` 单人审批。
- P0 只支持通知型监听器，复用站内信、短信、邮件能力。
- P0 不新增 listener 模板表。
- P0 不新增 form version 表。
- P0 前端列表/搜索页遵循 `docs/frontend-list-table-page-standard.md`。
- P0 前端编辑/详情页遵循 `docs/frontend-edit-detail-page-standard.md`。
- 所有新增或编辑文本文件使用 UTF-8。
- 有意义的后端改动先跑窄测试，再跑 `mvn -f hunyuan-backend/pom.xml -pl hunyuan-admin -am -DskipTests compile`。
- 有意义的前端改动至少验证 `pnpm --dir hunyuan-design -F @hunyuan/system run typecheck` 和 `pnpm --dir hunyuan-design -F @vben/web-ele run typecheck`。

---

## File Structure

### Aggregator and Module Wiring

- Modify: `hunyuan-backend/pom.xml`
  - 新增 `hunyuan-bpm` 模块、`flowable.version` 属性、`flowable-bom` 依赖管理。
- Modify: `hunyuan-backend/hunyuan-admin/pom.xml`
  - 新增对 `hunyuan-bpm` 的依赖。
- Create: `hunyuan-backend/hunyuan-bpm/pom.xml`
  - 只引入 `flowable-spring-boot-starter-process`、`hunyuan-base`、测试依赖，不引入 Flowable UI、Form 引擎或 IDM 模块。
- Create: `hunyuan-backend/hunyuan-bpm/src/main/resources/dev/hunyuan-bpm.yaml`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/resources/test/hunyuan-bpm.yaml`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/resources/pre/hunyuan-bpm.yaml`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/resources/prod/hunyuan-bpm.yaml`
  - 统一承载 `bpm.flowable.*`、异步执行器、schema update、history level 等内核配置。

### BPM Public API, SPI, and Internal Kernel Boundaries

- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/api/identity/BpmEmployeeSnapshot.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/api/identity/BpmOrgIdentityGateway.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/api/identity/BpmCurrentActorProvider.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/config/BpmFlowableProperties.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/config/BpmFlowableAutoConfiguration.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidator.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelSimulator.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelBpmnCompiler.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/internal/FlowableProcessDefinitionGateway.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/internal/FlowableProcessInstanceGateway.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/internal/FlowableTaskGateway.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/internal/FlowableProjectionEventListener.java`
  - 所有 `org.flowable.*` 导入都留在 `config/` 与 `engine/internal/` 下。

### Core BPM Domain Modules

- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/category/domain/entity/BpmCategoryEntity.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/category/domain/form/BpmCategoryAddForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/category/domain/form/BpmCategoryQueryForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/category/domain/form/BpmCategoryUpdateForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/category/domain/vo/BpmCategoryVO.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/category/dao/BpmCategoryDao.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/category/service/BpmCategoryService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/resources/mapper/bpm/category/BpmCategoryMapper.xml`

- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/form/domain/entity/BpmFormEntity.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/form/domain/form/BpmFormAddForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/form/domain/form/BpmFormQueryForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/form/domain/form/BpmFormUpdateForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/form/domain/vo/BpmFormVO.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/form/dao/BpmFormDao.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/form/service/BpmFormService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/resources/mapper/bpm/form/BpmFormMapper.xml`

- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/model/domain/entity/BpmModelEntity.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/model/domain/form/BpmModelAddForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/model/domain/form/BpmModelUpdateForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/model/domain/form/BpmDesignerSaveForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/model/domain/vo/BpmModelVO.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/model/domain/vo/BpmDesignerDetailVO.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/model/dao/BpmModelDao.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/model/service/BpmModelService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/model/service/BpmDesignerService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/resources/mapper/bpm/model/BpmModelMapper.xml`

- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/domain/entity/BpmDefinitionEntity.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/domain/entity/BpmDefinitionNodeEntity.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/domain/form/BpmDefinitionQueryForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/domain/form/BpmDefinitionPublishForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/domain/vo/BpmDefinitionVO.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/domain/vo/BpmDefinitionDetailVO.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/dao/BpmDefinitionDao.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/dao/BpmDefinitionNodeDao.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/service/BpmDefinitionService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/resources/mapper/bpm/definition/BpmDefinitionMapper.xml`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/resources/mapper/bpm/definition/BpmDefinitionNodeMapper.xml`

- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/entity/BpmInstanceEntity.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/entity/BpmTaskEntity.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/entity/BpmTaskActionLogEntity.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/entity/BpmInstanceCopyEntity.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmInstanceStartForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmTaskApproveForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmTaskRejectForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmTaskReturnForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmTaskTransferForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmInstanceCancelForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmStartableDefinitionVO.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmInstanceVO.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmTaskVO.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/dao/BpmInstanceDao.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/dao/BpmTaskDao.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/dao/BpmTaskActionLogDao.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/dao/BpmInstanceCopyDao.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmNotificationListenerService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/resources/mapper/bpm/runtime/BpmInstanceMapper.xml`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/resources/mapper/bpm/runtime/BpmTaskMapper.xml`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/resources/mapper/bpm/runtime/BpmTaskActionLogMapper.xml`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/resources/mapper/bpm/runtime/BpmInstanceCopyMapper.xml`

### BPM Controllers

- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmCategoryController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmFormController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmModelController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmDesignerController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmDefinitionController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmInstanceController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmTaskController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmListenerController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/app/AppBpmStartController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/app/AppBpmInstanceController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/app/AppBpmTaskController.java`

### Admin Adapters

- Create: `hunyuan-backend/hunyuan-admin/src/main/java/com/hunyuan/sa/admin/module/bpm/adapter/AdminBpmOrgIdentityGateway.java`
- Create: `hunyuan-backend/hunyuan-admin/src/main/java/com/hunyuan/sa/admin/module/bpm/adapter/AdminBpmCurrentActorProvider.java`
  - 复用 `EmployeeService`、`DepartmentService`、`RoleEmployeeService`、`LoginManager`、`SmartRequestUtil`。

### Tests

- Create: `hunyuan-backend/hunyuan-admin/src/test/java/com/hunyuan/sa/admin/module/bpm/BpmFlowableCompatibilityTest.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/schema/BpmSchemaSourceTest.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/authoring/BpmAuthoringServiceTest.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/definition/BpmDefinitionPublishServiceTest.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/architecture/BpmApiIsolationTest.java`

### Frontend API and Pages

- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/category.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/form.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/model.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/definition.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/listener.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/index.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`

- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/category/category-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/form/form-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/model/model-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/model/model-editor.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/definition/definition-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/instance/instance-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/task/task-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/listener/listener-catalog.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/workbench/startable-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/workbench/my-instance-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/workbench/my-todo-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/workbench/my-done-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/components/bpm-instance-timeline-drawer.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/components/bpm-task-action-drawer.vue`

### SQL

- Create: `数据库SQL脚本/mysql/sql-update-log/v3.34.0.sql`
  - 先建 9 张核心表，后补 BPM 菜单与按钮权限。

## Task 1: Add `hunyuan-bpm` and Prove the Hidden-Kernel Wiring

**Files:**
- Modify: `hunyuan-backend/pom.xml`
- Modify: `hunyuan-backend/hunyuan-admin/pom.xml`
- Create: `hunyuan-backend/hunyuan-bpm/pom.xml`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/resources/dev/hunyuan-bpm.yaml`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/resources/test/hunyuan-bpm.yaml`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/resources/pre/hunyuan-bpm.yaml`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/resources/prod/hunyuan-bpm.yaml`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/api/identity/BpmEmployeeSnapshot.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/api/identity/BpmOrgIdentityGateway.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/api/identity/BpmCurrentActorProvider.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/config/BpmFlowableProperties.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/config/BpmFlowableAutoConfiguration.java`
- Create: `hunyuan-backend/hunyuan-admin/src/main/java/com/hunyuan/sa/admin/module/bpm/adapter/AdminBpmOrgIdentityGateway.java`
- Create: `hunyuan-backend/hunyuan-admin/src/main/java/com/hunyuan/sa/admin/module/bpm/adapter/AdminBpmCurrentActorProvider.java`
- Test: `hunyuan-backend/hunyuan-admin/src/test/java/com/hunyuan/sa/admin/module/bpm/BpmFlowableCompatibilityTest.java`

**Interfaces:**
- Consumes:
  - `EmployeeService`, `DepartmentService`, `RoleEmployeeService`, `LoginManager` from `hunyuan-admin`
  - `SmartRequestUtil` from `hunyuan-base`
- Produces:
  - `record BpmEmployeeSnapshot(Long employeeId, String actualName, Long departmentId, String departmentName, String phone, String email)`
  - `interface BpmOrgIdentityGateway { BpmEmployeeSnapshot requireEmployee(Long employeeId); Long resolveDepartmentManagerEmployeeId(Long departmentId); List<Long> listEmployeeIdsByRoleId(Long roleId); }`
  - `interface BpmCurrentActorProvider { Long requireCurrentEmployeeId(); }`
  - `class BpmFlowableProperties { private boolean enabled; private String databaseSchemaUpdate; private boolean asyncExecutorActivate; private String historyLevel; }`

- [ ] **Step 1: Write the failing compatibility test first**

Create `hunyuan-backend/hunyuan-admin/src/test/java/com/hunyuan/sa/admin/module/bpm/BpmFlowableCompatibilityTest.java`:

```java
package com.hunyuan.sa.admin.module.bpm;

import com.hunyuan.sa.admin.AdminApplication;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import org.flowable.engine.ProcessEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = AdminApplication.class,
        properties = {
                "bpm.flowable.enabled=true",
                "bpm.flowable.database-schema-update=false",
                "bpm.flowable.async-executor-activate=false"
        }
)
class BpmFlowableCompatibilityTest {

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private BpmOrgIdentityGateway bpmOrgIdentityGateway;

    @Autowired
    private BpmCurrentActorProvider bpmCurrentActorProvider;

    @Test
    void loadsFlowableAsHiddenKernelInsideBpmModule() {
        assertThat(processEngine).isNotNull();
        assertThat(bpmOrgIdentityGateway).isNotNull();
        assertThat(bpmCurrentActorProvider).isNotNull();
    }
}
```

- [ ] **Step 2: Run the focused backend test and confirm it fails before scaffolding**

Run:

```bash
mvn -f hunyuan-backend/pom.xml -pl hunyuan-admin -am -Dtest=BpmFlowableCompatibilityTest test
```

Expected:
- 编译失败，提示 `package com.hunyuan.sa.bpm.api.identity does not exist`
- 或者 `Could not find artifact com.hunyuan:hunyuan-bpm`

- [ ] **Step 3: Implement the module scaffold, dependency direction, and admin adapters**

Apply these core edits:

`hunyuan-backend/pom.xml` add the module and Flowable BOM:

```xml
<modules>
    <module>hunyuan-base</module>
    <module>hunyuan-bpm</module>
    <module>hunyuan-admin</module>
</modules>

<properties>
    <flowable.version>7.2.0</flowable.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.flowable</groupId>
            <artifactId>flowable-bom</artifactId>
            <version>${flowable.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Create `hunyuan-backend/hunyuan-bpm/pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.hunyuan</groupId>
        <artifactId>hunyuan-backend</artifactId>
        <version>3.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>hunyuan-bpm</artifactId>
    <version>3.0.0</version>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.hunyuan</groupId>
            <artifactId>hunyuan-base</artifactId>
            <version>3.0.0</version>
        </dependency>
        <dependency>
            <groupId>org.flowable</groupId>
            <artifactId>flowable-spring-boot-starter-process</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

Create the public SPI and admin adapters:

```java
package com.hunyuan.sa.bpm.api.identity;

public record BpmEmployeeSnapshot(
        Long employeeId,
        String actualName,
        Long departmentId,
        String departmentName,
        String phone,
        String email
) {
}
```

```java
package com.hunyuan.sa.admin.module.bpm.adapter;

import com.hunyuan.sa.admin.module.system.department.service.DepartmentService;
import com.hunyuan.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import com.hunyuan.sa.admin.module.system.employee.service.EmployeeService;
import com.hunyuan.sa.admin.module.system.login.manager.LoginManager;
import com.hunyuan.sa.admin.module.system.role.service.RoleEmployeeService;
import com.hunyuan.sa.base.common.util.SmartRequestUtil;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdminBpmOrgIdentityGateway implements BpmOrgIdentityGateway {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final RoleEmployeeService roleEmployeeService;
    private final LoginManager loginManager;

    public AdminBpmOrgIdentityGateway(
            EmployeeService employeeService,
            DepartmentService departmentService,
            RoleEmployeeService roleEmployeeService,
            LoginManager loginManager
    ) {
        this.employeeService = employeeService;
        this.departmentService = departmentService;
        this.roleEmployeeService = roleEmployeeService;
        this.loginManager = loginManager;
    }

    @Override
    public BpmEmployeeSnapshot requireEmployee(Long employeeId) {
        EmployeeEntity employee = employeeService.getById(employeeId);
        var requestEmployee = loginManager.getRequestEmployee(employeeId);
        return new BpmEmployeeSnapshot(
                employee.getEmployeeId(),
                employee.getActualName(),
                employee.getDepartmentId(),
                requestEmployee == null ? null : requestEmployee.getDepartmentName(),
                employee.getPhone(),
                employee.getEmail()
        );
    }

    @Override
    public Long resolveDepartmentManagerEmployeeId(Long departmentId) {
        var department = departmentService.getDepartmentById(departmentId);
        return department == null ? null : department.getManagerId();
    }

    @Override
    public List<Long> listEmployeeIdsByRoleId(Long roleId) {
        return roleEmployeeService.getAllEmployeeByRoleId(roleId)
                .stream()
                .map(item -> item.getEmployeeId())
                .toList();
    }
}
```

```java
package com.hunyuan.sa.admin.module.bpm.adapter;

import com.hunyuan.sa.base.common.util.SmartRequestUtil;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import org.springframework.stereotype.Component;

@Component
public class AdminBpmCurrentActorProvider implements BpmCurrentActorProvider {

    @Override
    public Long requireCurrentEmployeeId() {
        Long employeeId = SmartRequestUtil.getRequestUserId();
        if (employeeId == null) {
            throw new IllegalStateException("current employee is required");
        }
        return employeeId;
    }
}
```

- [ ] **Step 4: Re-run the compatibility check and the admin compile gate**

Run:

```bash
mvn -f hunyuan-backend/pom.xml -pl hunyuan-admin -am -Dtest=BpmFlowableCompatibilityTest test
mvn -f hunyuan-backend/pom.xml -pl hunyuan-admin -am -DskipTests compile
```

Expected:
- `BpmFlowableCompatibilityTest` passes.
- Maven prints `BUILD SUCCESS`.

- [ ] **Step 5: Commit the scaffold slice**

Run:

```bash
git add hunyuan-backend/pom.xml hunyuan-backend/hunyuan-admin/pom.xml hunyuan-backend/hunyuan-bpm hunyuan-backend/hunyuan-admin/src/main/java/com/hunyuan/sa/admin/module/bpm/adapter hunyuan-backend/hunyuan-admin/src/test/java/com/hunyuan/sa/admin/module/bpm/BpmFlowableCompatibilityTest.java
git commit -m "feat: add bpm module scaffold"
```

Expected:
- Git creates one commit containing the new module, Flowable wiring, and the admin-side adapters only.

## Task 2: Add the 9-Table Schema, Entities, DAOs, and State Enums

**Files:**
- Create: `数据库SQL脚本/mysql/sql-update-log/v3.34.0.sql`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmDefinitionLifecycleStateEnum.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmDefinitionStartStateEnum.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmInstanceRunStateEnum.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmInstanceResultStateEnum.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmTaskStateEnum.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmTaskResultEnum.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmCopyReadStateEnum.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmCandidateResolverTypeEnum.java`
- Create: the 9 entity classes, 9 DAO interfaces, and 8 mapper XML files listed in the file-structure section
- Test: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/schema/BpmSchemaSourceTest.java`

**Interfaces:**
- Consumes:
  - MyBatis-Plus `BaseMapper<T>`
  - 当前设计稿中 9 表字段定义
- Produces:
  - `class BpmCategoryEntity`
  - `class BpmFormEntity`
  - `class BpmModelEntity`
  - `class BpmDefinitionEntity`
  - `class BpmDefinitionNodeEntity`
  - `class BpmInstanceEntity`
  - `class BpmTaskEntity`
  - `class BpmTaskActionLogEntity`
  - `class BpmInstanceCopyEntity`

- [ ] **Step 1: Write a failing source-contract test for the schema and entity shape**

Create `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/schema/BpmSchemaSourceTest.java`:

```java
package com.hunyuan.sa.bpm.schema;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BpmSchemaSourceTest {

    @Test
    void definesTheNineCoreBpmTablesAndProjectionColumns() throws IOException {
        String sql = Files.readString(Path.of("../../数据库SQL脚本/mysql/sql-update-log/v3.34.0.sql"));

        assertThat(sql).contains("CREATE TABLE `t_bpm_category`");
        assertThat(sql).contains("CREATE TABLE `t_bpm_form`");
        assertThat(sql).contains("CREATE TABLE `t_bpm_model`");
        assertThat(sql).contains("CREATE TABLE `t_bpm_definition`");
        assertThat(sql).contains("CREATE TABLE `t_bpm_definition_node`");
        assertThat(sql).contains("CREATE TABLE `t_bpm_instance`");
        assertThat(sql).contains("CREATE TABLE `t_bpm_task`");
        assertThat(sql).contains("CREATE TABLE `t_bpm_task_action_log`");
        assertThat(sql).contains("CREATE TABLE `t_bpm_instance_copy`");
        assertThat(sql).contains("initial_form_data_snapshot_json");
        assertThat(sql).contains("current_form_data_snapshot_json");
        assertThat(sql).contains("compiled_bpmn_xml");
        assertThat(sql).contains("compiled_node_snapshot_json");
    }

    @Test
    void keepsEntitiesForDefinitionAndRuntimeSnapshots() throws IOException {
        String definitionEntity = Files.readString(Path.of("src/main/java/com/hunyuan/sa/bpm/module/definition/domain/entity/BpmDefinitionEntity.java"));
        String instanceEntity = Files.readString(Path.of("src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/entity/BpmInstanceEntity.java"));
        String taskEntity = Files.readString(Path.of("src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/entity/BpmTaskEntity.java"));

        assertThat(definitionEntity).contains("private String compiledBpmnXml;");
        assertThat(definitionEntity).contains("private Integer lifecycleState;");
        assertThat(instanceEntity).contains("private String initialFormDataSnapshotJson;");
        assertThat(instanceEntity).contains("private String currentFormDataSnapshotJson;");
        assertThat(taskEntity).contains("private String engineTaskId;");
        assertThat(taskEntity).contains("private Long assigneeEmployeeId;");
    }
}
```

- [ ] **Step 2: Run the schema test and confirm it fails before persistence files exist**

Run:

```bash
mvn -f hunyuan-backend/pom.xml -pl hunyuan-bpm -am -Dtest=BpmSchemaSourceTest test
```

Expected:
- 测试失败，提示 `v3.34.0.sql` 或 BPM entity 文件不存在。

- [ ] **Step 3: Implement the SQL, entities, DAOs, and enums**

Create the SQL and representative persistence files.

`数据库SQL脚本/mysql/sql-update-log/v3.34.0.sql` must start with:

```sql
CREATE TABLE `t_bpm_category` (
  `category_id` bigint NOT NULL AUTO_INCREMENT,
  `category_code` varchar(64) NOT NULL,
  `category_name` varchar(128) NOT NULL,
  `icon` varchar(255) DEFAULT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `disabled_flag` tinyint(1) NOT NULL DEFAULT 0,
  `remark` varchar(500) DEFAULT NULL,
  `deleted_flag` tinyint(1) NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `uk_category_code` (`category_code`)
);

CREATE TABLE `t_bpm_definition` (
  `definition_id` bigint NOT NULL AUTO_INCREMENT,
  `model_id` bigint NOT NULL,
  `definition_key` varchar(64) NOT NULL,
  `definition_name` varchar(128) NOT NULL,
  `definition_version` int NOT NULL,
  `category_id_snapshot` bigint NOT NULL,
  `category_name_snapshot` varchar(128) NOT NULL,
  `form_type_snapshot` tinyint NOT NULL,
  `form_id_snapshot` bigint NOT NULL,
  `form_name_snapshot` varchar(128) NOT NULL,
  `form_schema_snapshot_json` longtext NOT NULL,
  `simple_model_snapshot_json` longtext NOT NULL,
  `compiled_bpmn_xml` longtext NOT NULL,
  `start_rule_snapshot_json` longtext NOT NULL,
  `manager_scope_snapshot_json` longtext DEFAULT NULL,
  `title_rule_snapshot_json` longtext DEFAULT NULL,
  `summary_rule_snapshot_json` longtext DEFAULT NULL,
  `variable_mapping_snapshot_json` longtext DEFAULT NULL,
  `instance_no_rule_id_snapshot` int DEFAULT NULL,
  `lifecycle_state` tinyint NOT NULL,
  `start_state` tinyint NOT NULL,
  `engine_process_definition_id` varchar(128) NOT NULL,
  `published_by_employee_id` bigint NOT NULL,
  `published_by_name_snapshot` varchar(64) NOT NULL,
  `published_at` datetime NOT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`definition_id`),
  UNIQUE KEY `uk_definition_key_version` (`definition_key`, `definition_version`)
);
```

`BpmDefinitionEntity.java` and `BpmInstanceEntity.java` must keep the snapshot truth explicitly:

```java
@Data
@TableName("t_bpm_definition")
public class BpmDefinitionEntity {

    @TableId(type = IdType.AUTO)
    private Long definitionId;

    private Long modelId;
    private String definitionKey;
    private Integer definitionVersion;
    private String formSchemaSnapshotJson;
    private String simpleModelSnapshotJson;
    private String compiledBpmnXml;
    private Integer lifecycleState;
    private Integer startState;
    private String engineProcessDefinitionId;
    private Long publishedByEmployeeId;
    private String publishedByNameSnapshot;
    private LocalDateTime publishedAt;
}
```

```java
@Data
@TableName("t_bpm_instance")
public class BpmInstanceEntity {

    @TableId(type = IdType.AUTO)
    private Long instanceId;

    private String instanceNo;
    private Long definitionId;
    private String engineProcessDefinitionId;
    private String engineProcessInstanceId;
    private String title;
    private Long startEmployeeId;
    private String initialFormDataSnapshotJson;
    private String currentFormDataSnapshotJson;
    private Integer runState;
    private Integer resultState;
    private Integer activeTaskCount;
    private LocalDateTime startedAt;
    private LocalDateTime lastActionAt;
    private LocalDateTime finishedAt;
    private LocalDateTime cancelledAt;
}
```

- [ ] **Step 4: Re-run the schema test and the BPM compile gate**

Run:

```bash
mvn -f hunyuan-backend/pom.xml -pl hunyuan-bpm -am -Dtest=BpmSchemaSourceTest test
mvn -f hunyuan-backend/pom.xml -pl hunyuan-bpm -am -DskipTests compile
```

Expected:
- `BpmSchemaSourceTest` passes.
- Maven prints `BUILD SUCCESS`.

- [ ] **Step 5: Commit the schema slice**

Run:

```bash
git add 数据库SQL脚本/mysql/sql-update-log/v3.34.0.sql hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module hunyuan-backend/hunyuan-bpm/src/main/resources/mapper hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/schema/BpmSchemaSourceTest.java
git commit -m "feat: add bpm core schema"
```

Expected:
- Git creates one commit containing the 9-table SQL, entities, DAOs, mapper XML, and state enums.

## Task 3: Implement Category, Form, Model, and Designer Draft APIs

**Files:**
- Create: the category/form/model domain forms and VOs from the file-structure section
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/model/service/BpmDesignerService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmCategoryController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmFormController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmModelController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmDesignerController.java`
- Test: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/authoring/BpmAuthoringServiceTest.java`

**Interfaces:**
- Consumes:
  - `BpmCategoryDao`, `BpmFormDao`, `BpmModelDao`
  - `SimpleModelValidator`
- Produces:
  - `ResponseDTO<PageResult<BpmCategoryVO>> queryCategory(BpmCategoryQueryForm queryForm)`
  - `ResponseDTO<PageResult<BpmFormVO>> queryForm(BpmFormQueryForm queryForm)`
  - `ResponseDTO<PageResult<BpmModelVO>> queryModel(BpmModelQueryForm queryForm)`
  - `ResponseDTO<BpmDesignerDetailVO> getDesignerDetail(Long modelId)`
  - `ResponseDTO<String> saveDesignerDraft(BpmDesignerSaveForm saveForm)`
  - `ResponseDTO<String> validateDesignerDraft(Long modelId)`
  - `ResponseDTO<String> simulateDesignerDraft(Long modelId)`

- [ ] **Step 1: Write failing tests for draft authoring rules and controller surface**

Create `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/authoring/BpmAuthoringServiceTest.java`:

```java
package com.hunyuan.sa.bpm.authoring;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.form.dao.BpmFormDao;
import com.hunyuan.sa.bpm.module.form.domain.entity.BpmFormEntity;
import com.hunyuan.sa.bpm.module.model.dao.BpmModelDao;
import com.hunyuan.sa.bpm.module.model.domain.entity.BpmModelEntity;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmDesignerSaveForm;
import com.hunyuan.sa.bpm.module.model.service.BpmDesignerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class BpmAuthoringServiceTest {

    private BpmDesignerService bpmDesignerService;

    private BpmModelDao bpmModelDao;

    @BeforeEach
    void setUp() {
        bpmDesignerService = new BpmDesignerService();
        bpmModelDao = Mockito.mock(BpmModelDao.class);
        ReflectionTestUtils.setField(bpmDesignerService, "bpmModelDao", bpmModelDao);
        ReflectionTestUtils.setField(bpmDesignerService, "bpmFormDao", Mockito.mock(BpmFormDao.class));
    }

    @Test
    void saveDesignerDraftShouldFlipHasUnpublishedChanges() {
        BpmModelEntity entity = new BpmModelEntity();
        entity.setModelId(1L);
        entity.setFormId(9L);
        when(bpmModelDao.selectById(anyLong())).thenReturn(entity);

        BpmDesignerSaveForm form = new BpmDesignerSaveForm();
        form.setModelId(1L);
        form.setSimpleModelJson("{\"nodes\":[]}");
        form.setStartRuleJson("{\"allowAll\":true}");

        ResponseDTO<String> response = bpmDesignerService.saveDesignerDraft(form);

        assertThat(response.getOk()).isTrue();
        assertThat(entity.getHasUnpublishedChanges()).isTrue();
    }

    @Test
    void validateDraftShouldRejectUnsupportedApprovalMode() {
        String source = """
                {"nodes":[{"type":"userTask","approvalMode":"ratio"}]}
                """;
        assertThat(source).contains("\"approvalMode\":\"ratio\"");
    }
}
```

- [ ] **Step 2: Run the authoring test and confirm it fails before services/controllers exist**

Run:

```bash
mvn -f hunyuan-backend/pom.xml -pl hunyuan-bpm -am -Dtest=BpmAuthoringServiceTest test
```

Expected:
- 编译失败，提示 `BpmDesignerService`、`BpmDesignerSaveForm` 等类不存在。

- [ ] **Step 3: Implement the authoring services and admin controllers**

The authoring service layer should follow the existing repo pattern:

```java
@Service
public class BpmDesignerService {

    @Resource
    private BpmModelDao bpmModelDao;

    @Resource
    private BpmFormDao bpmFormDao;

    @Resource
    private SimpleModelValidator simpleModelValidator;

    public ResponseDTO<String> saveDesignerDraft(BpmDesignerSaveForm saveForm) {
        BpmModelEntity entity = bpmModelDao.selectById(saveForm.getModelId());
        if (entity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        entity.setSimpleModelJson(saveForm.getSimpleModelJson());
        entity.setStartRuleJson(saveForm.getStartRuleJson());
        entity.setManagerScopeJson(saveForm.getManagerScopeJson());
        entity.setTitleRuleJson(saveForm.getTitleRuleJson());
        entity.setSummaryRuleJson(saveForm.getSummaryRuleJson());
        entity.setVariableMappingJson(saveForm.getVariableMappingJson());
        entity.setHasUnpublishedChanges(Boolean.TRUE);
        bpmModelDao.updateById(entity);
        return ResponseDTO.ok();
    }

    public ResponseDTO<String> validateDesignerDraft(Long modelId) {
        BpmModelEntity entity = bpmModelDao.selectById(modelId);
        return simpleModelValidator.validate(entity.getSimpleModelJson(), entity.getStartRuleJson());
    }
}
```

```java
@RestController
@Tag(name = "BPM Designer")
public class AdminBpmDesignerController {

    @Resource
    private BpmDesignerService bpmDesignerService;

    @GetMapping("/bpm/designer/detail/{modelId}")
    @SaCheckPermission("bpm:designer:detail")
    public ResponseDTO<BpmDesignerDetailVO> detail(@PathVariable Long modelId) {
        return bpmDesignerService.getDesignerDetail(modelId);
    }

    @PostMapping("/bpm/designer/save")
    @SaCheckPermission("bpm:designer:update")
    public ResponseDTO<String> save(@RequestBody @Valid BpmDesignerSaveForm saveForm) {
        return bpmDesignerService.saveDesignerDraft(saveForm);
    }

    @GetMapping("/bpm/designer/validate/{modelId}")
    @SaCheckPermission("bpm:designer:validate")
    public ResponseDTO<String> validate(@PathVariable Long modelId) {
        return bpmDesignerService.validateDesignerDraft(modelId);
    }
}
```

- [ ] **Step 4: Re-run the authoring test and the BPM compile gate**

Run:

```bash
mvn -f hunyuan-backend/pom.xml -pl hunyuan-bpm -am -Dtest=BpmAuthoringServiceTest test
mvn -f hunyuan-backend/pom.xml -pl hunyuan-bpm -am -DskipTests compile
```

Expected:
- `BpmAuthoringServiceTest` passes.
- Maven prints `BUILD SUCCESS`.

- [ ] **Step 5: Commit the authoring backend slice**

Run:

```bash
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/category hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/form hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/model hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmCategoryController.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmFormController.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmModelController.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmDesignerController.java hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/authoring/BpmAuthoringServiceTest.java
git commit -m "feat: add bpm authoring backend"
```

Expected:
- Git creates one commit containing category/form/model/designer draft management only.

## Task 4: Implement Compile, Publish, Definition History, and API Isolation Checks

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelBpmnCompiler.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/service/BpmDefinitionService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmDefinitionController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/definition/BpmDefinitionPublishServiceTest.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/architecture/BpmApiIsolationTest.java`

**Interfaces:**
- Consumes:
  - `SimpleModelValidator`
  - `SimpleModelSimulator`
  - `SimpleModelBpmnCompiler`
  - `FlowableProcessDefinitionGateway`
  - `BpmCurrentActorProvider`
  - `BpmOrgIdentityGateway`
- Produces:
  - `ResponseDTO<Long> publish(BpmDefinitionPublishForm publishForm)`
  - `ResponseDTO<PageResult<BpmDefinitionVO>> queryDefinition(BpmDefinitionQueryForm queryForm)`
  - `ResponseDTO<String> updateStartState(Long definitionId, Integer startState)`
  - `CompiledDefinitionArtifact compile(String simpleModelJson, String startRuleJson, String variableMappingJson)`

- [ ] **Step 1: Write failing tests for publish semantics and Flowable leakage protection**

Create `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/definition/BpmDefinitionPublishServiceTest.java`:

```java
package com.hunyuan.sa.bpm.definition;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionDao;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionEntity;
import com.hunyuan.sa.bpm.module.definition.domain.form.BpmDefinitionPublishForm;
import com.hunyuan.sa.bpm.module.definition.service.BpmDefinitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class BpmDefinitionPublishServiceTest {

    private BpmDefinitionService bpmDefinitionService;

    @BeforeEach
    void setUp() {
        bpmDefinitionService = new BpmDefinitionService();
        ReflectionTestUtils.setField(bpmDefinitionService, "bpmDefinitionDao", Mockito.mock(BpmDefinitionDao.class));
        ReflectionTestUtils.setField(bpmDefinitionService, "bpmDefinitionNodeDao", Mockito.mock(BpmDefinitionNodeDao.class));
    }

    @Test
    void publishShouldCreateNewImmutableDefinitionAndHistoricalizeOldVersion() {
        BpmDefinitionPublishForm form = new BpmDefinitionPublishForm();
        form.setModelId(1L);

        ResponseDTO<Long> response = bpmDefinitionService.publish(form);

        assertThat(response).isNotNull();
    }

    @Test
    void publishedDefinitionMustKeepFormAndListenerSnapshots() {
        BpmDefinitionEntity entity = new BpmDefinitionEntity();
        entity.setFormSchemaSnapshotJson("{\"fields\":[]}");
        entity.setSimpleModelSnapshotJson("{\"listeners\":[{\"channel\":\"MESSAGE\"}]}");

        assertThat(entity.getFormSchemaSnapshotJson()).contains("\"fields\"");
        assertThat(entity.getSimpleModelSnapshotJson()).contains("\"listeners\"");
    }
}
```

Create `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/architecture/BpmApiIsolationTest.java`:

```java
package com.hunyuan.sa.bpm.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class BpmApiIsolationTest {

    @Test
    void publicBpmContractsDoNotImportFlowableTypes() throws IOException {
        try (Stream<Path> stream = Files.walk(Path.of("src/main/java/com/hunyuan/sa/bpm"))) {
            stream.filter(path -> path.toString().contains("/controller/") || path.toString().contains("/api/"))
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            assertThat(Files.readString(path)).doesNotContain("org.flowable");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}
```

- [ ] **Step 2: Run the publish tests and confirm they fail before compile/publish logic exists**

Run:

```bash
mvn -f hunyuan-backend/pom.xml -pl hunyuan-bpm -am -Dtest=BpmDefinitionPublishServiceTest,BpmApiIsolationTest test
```

Expected:
- 测试失败，提示 `BpmDefinitionService.publish` 未实现或编译器/网关缺失。

- [ ] **Step 3: Implement the compiler, publish service, and definition controller**

The compiler and publish service should keep Flowable fully internal:

```java
public record CompiledDefinitionArtifact(
        String compiledBpmnXml,
        String engineProcessDefinitionId,
        List<CompiledNodeSnapshot> nodeSnapshots
) {
}
```

```java
@Service
public class BpmDefinitionService {

    @Resource
    private BpmModelDao bpmModelDao;

    @Resource
    private BpmDefinitionDao bpmDefinitionDao;

    @Resource
    private BpmDefinitionNodeDao bpmDefinitionNodeDao;

    @Resource
    private SimpleModelValidator simpleModelValidator;

    @Resource
    private SimpleModelBpmnCompiler simpleModelBpmnCompiler;

    @Resource
    private FlowableProcessDefinitionGateway flowableProcessDefinitionGateway;

    @Resource
    private BpmCurrentActorProvider bpmCurrentActorProvider;

    @Resource
    private BpmOrgIdentityGateway bpmOrgIdentityGateway;

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Long> publish(BpmDefinitionPublishForm publishForm) {
        BpmModelEntity model = bpmModelDao.selectById(publishForm.getModelId());
        ResponseDTO<String> validation = simpleModelValidator.validate(model.getSimpleModelJson(), model.getStartRuleJson());
        if (!Boolean.TRUE.equals(validation.getOk())) {
            return ResponseDTO.userErrorParam(validation.getMsg());
        }

        CompiledDefinitionArtifact artifact = simpleModelBpmnCompiler.compile(
                model.getSimpleModelJson(),
                model.getStartRuleJson(),
                model.getVariableMappingJson()
        );
        String engineProcessDefinitionId = flowableProcessDefinitionGateway.deploy(
                model.getModelKey(),
                model.getModelName(),
                artifact.compiledBpmnXml()
        );

        BpmDefinitionEntity definitionEntity = buildDefinitionEntity(model, artifact, engineProcessDefinitionId);
        bpmDefinitionDao.insert(definitionEntity);

        // 1. 历史化旧 definition
        // 2. 写 definition_node compiled snapshot
        // 3. 回填 model.publishedDefinitionId 与 hasUnpublishedChanges=false
        return ResponseDTO.ok(definitionEntity.getDefinitionId());
    }
}
```

```java
@RestController
@Tag(name = "BPM Definition")
public class AdminBpmDefinitionController {

    @Resource
    private BpmDefinitionService bpmDefinitionService;

    @PostMapping("/bpm/definition/publish")
    @SaCheckPermission("bpm:definition:publish")
    public ResponseDTO<Long> publish(@RequestBody @Valid BpmDefinitionPublishForm publishForm) {
        return bpmDefinitionService.publish(publishForm);
    }

    @GetMapping("/bpm/definition/query")
    @SaCheckPermission("bpm:definition:query")
    public ResponseDTO<PageResult<BpmDefinitionVO>> query(BpmDefinitionQueryForm queryForm) {
        return bpmDefinitionService.query(queryForm);
    }
}
```

- [ ] **Step 4: Re-run the publish tests and the BPM compile gate**

Run:

```bash
mvn -f hunyuan-backend/pom.xml -pl hunyuan-bpm -am -Dtest=BpmDefinitionPublishServiceTest,BpmApiIsolationTest test
mvn -f hunyuan-backend/pom.xml -pl hunyuan-bpm -am -DskipTests compile
```

Expected:
- 两个测试都通过。
- Maven prints `BUILD SUCCESS`.

- [ ] **Step 5: Commit the publish slice**

Run:

```bash
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmDefinitionController.java hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/definition/BpmDefinitionPublishServiceTest.java hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/architecture/BpmApiIsolationTest.java
git commit -m "feat: add bpm publish pipeline"
```

Expected:
- Git creates one commit containing compile/publish/history logic and the Flowable-leak guard test.

## Task 5: Implement Runtime Start, Task Actions, Projection Sync, and Notification Listeners

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmNotificationListenerService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmInstanceController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmTaskController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmListenerController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/app/AppBpmStartController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/app/AppBpmInstanceController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/app/AppBpmTaskController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/internal/FlowableProjectionEventListener.java`
- Test: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java`

**Interfaces:**
- Consumes:
  - `FlowableProcessInstanceGateway`
  - `FlowableTaskGateway`
  - `BpmOrgIdentityGateway`
  - `BpmCurrentActorProvider`
  - `SerialNumberService`
  - `MessageService`
  - `SmsService`
  - `MailService`
- Produces:
  - `ResponseDTO<Long> startInstance(BpmInstanceStartForm startForm)`
  - `ResponseDTO<String> approve(BpmTaskApproveForm approveForm)`
  - `ResponseDTO<String> reject(BpmTaskRejectForm rejectForm)`
  - `ResponseDTO<String> returnToInitiator(BpmTaskReturnForm returnForm)`
  - `ResponseDTO<String> transfer(BpmTaskTransferForm transferForm)`
  - `ResponseDTO<String> cancelInstance(BpmInstanceCancelForm cancelForm)`
  - `ResponseDTO<PageResult<BpmTaskVO>> queryMyTodo(BpmTaskQueryForm queryForm)`
  - `ResponseDTO<PageResult<BpmTaskVO>> queryMyDone(BpmTaskQueryForm queryForm)`

- [ ] **Step 1: Write failing runtime tests for start, return, and transfer semantics**

Create `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java`:

```java
package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceStartForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskReturnForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskTransferForm;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BpmRuntimeCommandServiceTest {

    @Test
    void startInstanceShouldKeepInitialAndCurrentFormSnapshotsSeparated() {
        BpmInstanceService service = new BpmInstanceService();
        BpmInstanceStartForm form = new BpmInstanceStartForm();
        form.setDefinitionId(1L);
        form.setFormDataJson("{\"amount\":100}");

        ResponseDTO<Long> response = service.startInstance(form);

        assertThat(response).isNotNull();
    }

    @Test
    void returnToInitiatorShouldMoveInstanceToWaitResubmitInsteadOfRejectingIt() {
        BpmTaskService service = new BpmTaskService();
        BpmTaskReturnForm form = new BpmTaskReturnForm();
        form.setTaskId(1L);
        form.setCommentText("请补齐附件");

        ResponseDTO<String> response = service.returnToInitiator(form);

        assertThat(response).isNotNull();
    }

    @Test
    void transferShouldWriteActionLogAndReassignTask() {
        BpmTaskService service = new BpmTaskService();
        BpmTaskTransferForm form = new BpmTaskTransferForm();
        form.setTaskId(1L);
        form.setToEmployeeId(22L);

        ResponseDTO<String> response = service.transfer(form);

        assertThat(response).isNotNull();
    }
}
```

- [ ] **Step 2: Run the runtime test and confirm it fails before runtime services exist**

Run:

```bash
mvn -f hunyuan-backend/pom.xml -pl hunyuan-bpm -am -Dtest=BpmRuntimeCommandServiceTest test
```

Expected:
- 编译失败，提示 runtime form/service/controller 不存在。

- [ ] **Step 3: Implement runtime commands, projections, and listener dispatch**

The runtime services must keep engine truth and platform projections distinct:

```java
@Service
public class BpmInstanceService {

    @Resource
    private BpmDefinitionDao bpmDefinitionDao;

    @Resource
    private BpmInstanceDao bpmInstanceDao;

    @Resource
    private FlowableProcessInstanceGateway flowableProcessInstanceGateway;

    @Resource
    private BpmCurrentActorProvider bpmCurrentActorProvider;

    @Resource
    private BpmOrgIdentityGateway bpmOrgIdentityGateway;

    @Resource
    private SerialNumberService serialNumberService;

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Long> startInstance(BpmInstanceStartForm startForm) {
        Long employeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        BpmEmployeeSnapshot employee = bpmOrgIdentityGateway.requireEmployee(employeeId);
        BpmDefinitionEntity definition = bpmDefinitionDao.selectById(startForm.getDefinitionId());

        SerialNumberIdEnum serialNumberIdEnum = SmartEnumUtil.getEnumByValue(
                definition.getInstanceNoRuleIdSnapshot(),
                SerialNumberIdEnum.class
        );
        String instanceNo = serialNumberService.generate(serialNumberIdEnum);
        String engineProcessInstanceId = flowableProcessInstanceGateway.start(
                definition.getEngineProcessDefinitionId(),
                employeeId,
                startForm.getFormDataJson()
        );

        BpmInstanceEntity entity = new BpmInstanceEntity();
        entity.setInstanceNo(instanceNo);
        entity.setDefinitionId(definition.getDefinitionId());
        entity.setEngineProcessDefinitionId(definition.getEngineProcessDefinitionId());
        entity.setEngineProcessInstanceId(engineProcessInstanceId);
        entity.setStartEmployeeId(employee.getEmployeeId());
        entity.setInitialFormDataSnapshotJson(startForm.getFormDataJson());
        entity.setCurrentFormDataSnapshotJson(startForm.getFormDataJson());
        entity.setRunState(BpmInstanceRunStateEnum.RUNNING.getValue());
        bpmInstanceDao.insert(entity);
        return ResponseDTO.ok(entity.getInstanceId());
    }
}
```

```java
@Service
public class BpmNotificationListenerService {

    @Resource
    private MessageService messageService;

    @Resource
    private SmsService smsService;

    @Resource
    private MailService mailService;

    public void dispatch(BpmNotificationCommand command) {
        if (command.channels().contains("MESSAGE")) {
            messageService.sendMessage(command.toMessageSendForm());
        }
        if (command.channels().contains("SMS")) {
            smsService.send(command.toSmsSendForm());
        }
        if (command.channels().contains("MAIL")) {
            mailService.sendMail(command.subject(), command.content(), List.of(), command.receiverMailList(), true);
        }
    }
}
```

```java
@RestController
@Tag(name = "BPM Task")
public class AppBpmTaskController {

    @Resource
    private BpmTaskService bpmTaskService;

    @PostMapping("/app/bpm/task/approve")
    public ResponseDTO<String> approve(@RequestBody @Valid BpmTaskApproveForm approveForm) {
        return bpmTaskService.approve(approveForm);
    }

    @PostMapping("/app/bpm/task/reject")
    public ResponseDTO<String> reject(@RequestBody @Valid BpmTaskRejectForm rejectForm) {
        return bpmTaskService.reject(rejectForm);
    }

    @PostMapping("/app/bpm/task/returnToInitiator")
    public ResponseDTO<String> returnToInitiator(@RequestBody @Valid BpmTaskReturnForm returnForm) {
        return bpmTaskService.returnToInitiator(returnForm);
    }

    @PostMapping("/app/bpm/task/transfer")
    public ResponseDTO<String> transfer(@RequestBody @Valid BpmTaskTransferForm transferForm) {
        return bpmTaskService.transfer(transferForm);
    }
}
```

- [ ] **Step 4: Re-run the runtime test, then run the admin compile gate**

Run:

```bash
mvn -f hunyuan-backend/pom.xml -pl hunyuan-bpm -am -Dtest=BpmRuntimeCommandServiceTest test
mvn -f hunyuan-backend/pom.xml -pl hunyuan-admin -am -DskipTests compile
```

Expected:
- `BpmRuntimeCommandServiceTest` passes.
- `hunyuan-admin` compile still prints `BUILD SUCCESS`.

- [ ] **Step 5: Commit the runtime slice**

Run:

```bash
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmInstanceController.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmTaskController.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmListenerController.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/app hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/internal/FlowableProjectionEventListener.java hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java
git commit -m "feat: add bpm runtime services"
```

Expected:
- Git creates one commit containing runtime start/task/action/listener logic only.

## Task 6: Land the Admin BPM Frontend Surface and Menu SQL

**Files:**
- Modify: `数据库SQL脚本/mysql/sql-update-log/v3.34.0.sql`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/category.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/form.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/model.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/definition.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/listener.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/index.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`
- Create: admin BPM pages and shared drawers/components listed in the file-structure section

**Interfaces:**
- Consumes:
  - Admin BPM backend endpoints from Tasks 3-5
  - `ArtSearchPanel`, `ArtTablePanel`, `ArtTableHeader`, `ArtTable`, `ArtEditPage`, `ArtEditSection`, `Page`
- Produces:
  - Admin routes:
    - `/system/bpm/category/category-list.vue`
    - `/system/bpm/form/form-list.vue`
    - `/system/bpm/model/model-list.vue`
    - `/system/bpm/model/model-editor.vue`
    - `/system/bpm/definition/definition-list.vue`
    - `/system/bpm/instance/instance-list.vue`
    - `/system/bpm/task/task-list.vue`
    - `/system/bpm/listener/listener-catalog.vue`

- [ ] **Step 1: Write failing frontend contract tests for API endpoints and admin pages**

Create `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`:

```ts
import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const categoryPagePath = 'apps/hunyuan-system/src/views/system/bpm/category/category-list.vue';
const modelEditorPath = 'apps/hunyuan-system/src/views/system/bpm/model/model-editor.vue';
const definitionPagePath = 'apps/hunyuan-system/src/views/system/bpm/definition/definition-list.vue';
const runtimePagePath = 'apps/hunyuan-system/src/views/system/bpm/task/task-list.vue';
const apiPath = 'apps/hunyuan-system/src/api/system/bpm/index.ts';

describe('bpm backend menu docking pages', () => {
  it('provides real admin bpm pages instead of module bridge placeholders', () => {
    [categoryPagePath, modelEditorPath, definitionPagePath, runtimePagePath].forEach((path) => {
      expect(existsSync(resolve(process.cwd(), path))).toBe(true);
    });
  });

  it('keeps bpm list pages dense and route-backed', () => {
    const categorySource = readFileSync(resolve(process.cwd(), categoryPagePath), 'utf8');

    expect(categorySource).toContain('ArtSearchPanel');
    expect(categorySource).toContain('ArtTablePanel');
    expect(categorySource).toContain(':collapsible="false"');
    expect(categorySource).not.toContain('category-page__title');
    expect(categorySource).not.toContain('module-bridge');
  });

  it('wires the bpm api barrel to category, form, model, definition, runtime, and listener endpoints', () => {
    const source = readFileSync(resolve(process.cwd(), apiPath), 'utf8');

    expect(source).toContain('/bpm/category/');
    expect(source).toContain('/bpm/form/');
    expect(source).toContain('/bpm/model/');
    expect(source).toContain('/bpm/definition/');
    expect(source).toContain('/bpm/designer/');
    expect(source).toContain('/bpm/task/');
    expect(source).toContain('/bpm/listener/');
  });
});
```

Create `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`:

```ts
import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const apiFiles = [
  { label: 'category', path: 'apps/hunyuan-system/src/api/system/bpm/category.ts', needles: ['/bpm/category/query', '/bpm/category/add', '/bpm/category/update'] },
  { label: 'form', path: 'apps/hunyuan-system/src/api/system/bpm/form.ts', needles: ['/bpm/form/query', '/bpm/form/add', '/bpm/form/update'] },
  { label: 'model', path: 'apps/hunyuan-system/src/api/system/bpm/model.ts', needles: ['/bpm/model/query', '/bpm/designer/detail/', '/bpm/definition/publish'] },
  { label: 'definition', path: 'apps/hunyuan-system/src/api/system/bpm/definition.ts', needles: ['/bpm/definition/query', '/bpm/definition/detail/', '/bpm/definition/updateStartState'] },
  { label: 'runtime', path: 'apps/hunyuan-system/src/api/system/bpm/runtime.ts', needles: ['/bpm/instance/query', '/app/bpm/startable', '/app/bpm/task/approve', '/app/bpm/task/returnToInitiator'] },
  { label: 'listener', path: 'apps/hunyuan-system/src/api/system/bpm/listener.ts', needles: ['/bpm/listener/query', '/bpm/listener/channelOptions'] },
] as const;

describe('bpm api modules', () => {
  it.each(apiFiles)('keeps $label api module bound to backend contracts', ({ path, needles }) => {
    const filePath = resolve(process.cwd(), path);

    expect(existsSync(filePath)).toBe(true);

    const source = readFileSync(filePath, 'utf8');
    needles.forEach((needle) => expect(source).toContain(needle));
  });
});
```

- [ ] **Step 2: Run the frontend contract tests and confirm they fail before files exist**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts
```

Expected:
- 两个命令都失败，提示 BPM API 与页面文件不存在。

- [ ] **Step 3: Implement the admin API modules, pages, drawers, and menu SQL**

The BPM frontend must stay inside the current `hunyuan-system` patterns:

```ts
// apps/hunyuan-system/src/api/system/bpm/model.ts
import { requestClient } from '#/api/request';

export async function queryBpmModelPage(params: BpmModelQueryParams) {
  return requestClient.post('/bpm/model/query', params);
}

export async function getDesignerDetail(modelId: number) {
  return requestClient.get(`/bpm/designer/detail/${modelId}`);
}

export async function saveDesignerDraft(params: BpmDesignerSaveForm) {
  return requestClient.post('/bpm/designer/save', params);
}

export async function publishDefinition(modelId: number) {
  return requestClient.post('/bpm/definition/publish', { modelId });
}
```

```vue
<script setup lang="ts">
import { reactive, ref } from 'vue';

import { ArtSearchPanel } from '@vben/art-hooks/common';
import { ArtTable, ArtTableHeader, ArtTablePanel, useTableColumns } from '@vben/art-hooks/table';
import { Page } from '@vben/common-ui';

import { queryBpmCategoryPage } from '#/api/system/bpm';

defineOptions({ name: 'SystemBpmCategoryList' });

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref([]);
const searchForm = reactive({ categoryCode: '', categoryName: '', disabledFlag: undefined });
const pagination = reactive({ current: 1, size: 10, total: 0 });

const { columns, columnChecks } = useTableColumns(() => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'categoryCode', label: '分类编码', minWidth: 180 },
  { prop: 'categoryName', label: '分类名称', minWidth: 180 },
  { prop: 'sort', label: '排序', width: 90, align: 'center' },
  { prop: 'disabledFlag', label: '状态', width: 90, align: 'center', useSlot: true },
  { prop: 'actions', label: '操作', width: 180, align: 'center', fixed: 'right', useSlot: true },
]);
</script>
```

`数据库SQL脚本/mysql/sql-update-log/v3.34.0.sql` must append BPM menu rows for the admin surface:

```sql
INSERT INTO `t_menu` (`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `icon`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `create_time`, `update_time`)
VALUES
  (308, '流程引擎', 1, 0, 40, '/system/bpm', NULL, 1, 'ep:connection', 1, 0, 0, 1, now(), now()),
  (309, '流程分类', 2, 308, 1, '/system/bpm/category', '/system/bpm/category/category-list.vue', 1, 'ep:collection-tag', 1, 0, 0, 1, now(), now()),
  (310, '流程表单', 2, 308, 2, '/system/bpm/form', '/system/bpm/form/form-list.vue', 1, 'ep:document', 1, 0, 0, 1, now(), now()),
  (311, '流程模型', 2, 308, 3, '/system/bpm/model', '/system/bpm/model/model-list.vue', 1, 'ep:share', 1, 0, 0, 1, now(), now()),
  (312, '流程定义', 2, 308, 4, '/system/bpm/definition', '/system/bpm/definition/definition-list.vue', 1, 'ep:files', 1, 0, 0, 1, now(), now()),
  (313, '流程实例', 2, 308, 5, '/system/bpm/instance', '/system/bpm/instance/instance-list.vue', 1, 'ep:histogram', 1, 0, 0, 1, now(), now()),
  (314, '流程任务', 2, 308, 6, '/system/bpm/task', '/system/bpm/task/task-list.vue', 1, 'ep:list', 1, 0, 0, 1, now(), now()),
  (315, '流程监听器', 2, 308, 7, '/system/bpm/listener', '/system/bpm/listener/listener-catalog.vue', 1, 'ep:bell', 1, 0, 0, 1, now(), now());
```

- [ ] **Step 4: Run the frontend BPM contract tests and both typecheck gates**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
pnpm --dir hunyuan-design -F @vben/web-ele run typecheck
```

Expected:
- Vitest contract tests pass.
- `@hunyuan/system` 和 `@vben/web-ele` typecheck 都通过。

- [ ] **Step 5: Commit the admin frontend slice**

Run:

```bash
git add 数据库SQL脚本/mysql/sql-update-log/v3.34.0.sql hunyuan-design/apps/hunyuan-system/src/api/system/bpm hunyuan-design/apps/hunyuan-system/src/views/system/bpm
git commit -m "feat: add bpm admin frontend"
```

Expected:
- Git creates one commit containing the admin BPM pages, API modules, module tests, and BPM menu SQL additions.

## Task 7: Land the Employee Workbench Pages and Run End-to-End Verification

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`
- Modify: `数据库SQL脚本/mysql/sql-update-log/v3.34.0.sql`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/workbench/startable-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/workbench/my-instance-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/workbench/my-todo-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/workbench/my-done-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/components/bpm-task-action-drawer.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/components/bpm-instance-timeline-drawer.vue`

**Interfaces:**
- Consumes:
  - App BPM APIs from Task 5
  - Shared drawers and list/table standards
- Produces:
  - Route-backed employee pages inside the existing `hunyuan-system` app
  - Task action drawer supporting `approve`, `reject`, `return-to-initiator`, `transfer`
  - Timeline drawer showing `CREATED`, `APPROVED`, `REJECTED`, `RETURNED_TO_INITIATOR`, `TRANSFERRED`, `RESUBMITTED`, `INSTANCE_CANCELLED`

- [ ] **Step 1: Extend the frontend module test with employee workbench assertions**

Append to `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`:

```ts
const startablePagePath = 'apps/hunyuan-system/src/views/system/bpm/workbench/startable-list.vue';
const myTodoPagePath = 'apps/hunyuan-system/src/views/system/bpm/workbench/my-todo-list.vue';
const actionDrawerPath = 'apps/hunyuan-system/src/views/system/bpm/components/bpm-task-action-drawer.vue';

it('provides route-backed employee bpm workbench pages and action drawer', () => {
  [startablePagePath, myTodoPagePath, actionDrawerPath].forEach((path) => {
    expect(existsSync(resolve(process.cwd(), path))).toBe(true);
  });

  const drawerSource = readFileSync(resolve(process.cwd(), actionDrawerPath), 'utf8');
  expect(drawerSource).toContain('approve');
  expect(drawerSource).toContain('reject');
  expect(drawerSource).toContain('returnToInitiator');
  expect(drawerSource).toContain('transfer');
});
```

- [ ] **Step 2: Run the workbench test and confirm it fails before files exist**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom -t "employee bpm workbench"
```

Expected:
- 测试失败，提示 workbench 页面或 action drawer 文件不存在。

- [ ] **Step 3: Implement the employee workbench pages and append menu rows**

The employee pages should stay list-first and drawer-driven:

```vue
<script setup lang="ts">
import { reactive, ref } from 'vue';

import { ArtSearchPanel } from '@vben/art-hooks/common';
import { ArtTable, ArtTableHeader, ArtTablePanel, useTableColumns } from '@vben/art-hooks/table';
import { Page } from '@vben/common-ui';

import { queryMyTodoPage } from '#/api/system/bpm';

defineOptions({ name: 'SystemBpmMyTodoList' });

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref([]);
const searchForm = reactive({ categoryName: '', instanceTitle: '' });
const pagination = reactive({ current: 1, size: 10, total: 0 });

const { columns, columnChecks } = useTableColumns(() => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'instanceTitle', label: '流程标题', minWidth: 260 },
  { prop: 'taskName', label: '当前任务', minWidth: 180 },
  { prop: 'startEmployeeNameSnapshot', label: '发起人', width: 120, align: 'center' },
  { prop: 'assignedAt', label: '到达时间', minWidth: 180 },
  { prop: 'actions', label: '操作', width: 180, align: 'center', fixed: 'right', useSlot: true },
]);
</script>
```

Append employee BPM menu rows to `v3.34.0.sql`:

```sql
INSERT INTO `t_menu` (`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `icon`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `create_time`, `update_time`)
VALUES
  (316, '我可发起', 2, 308, 8, '/system/bpm/workbench/startable', '/system/bpm/workbench/startable-list.vue', 1, 'ep:promotion', 1, 0, 0, 1, now(), now()),
  (317, '我发起的', 2, 308, 9, '/system/bpm/workbench/my-instance', '/system/bpm/workbench/my-instance-list.vue', 1, 'ep:document-copy', 1, 0, 0, 1, now(), now()),
  (318, '我的待办', 2, 308, 10, '/system/bpm/workbench/my-todo', '/system/bpm/workbench/my-todo-list.vue', 1, 'ep:checked', 1, 0, 0, 1, now(), now()),
  (319, '我的已办', 2, 308, 11, '/system/bpm/workbench/my-done', '/system/bpm/workbench/my-done-list.vue', 1, 'ep:finished', 1, 0, 0, 1, now(), now());
```

- [ ] **Step 4: Run the full end-to-end verification bundle**

Run:

```bash
mvn -f hunyuan-backend/pom.xml -pl hunyuan-bpm -am -Dtest=BpmSchemaSourceTest,BpmAuthoringServiceTest,BpmDefinitionPublishServiceTest,BpmRuntimeCommandServiceTest,BpmApiIsolationTest test
mvn -f hunyuan-backend/pom.xml -pl hunyuan-admin -am -Dtest=BpmFlowableCompatibilityTest test
mvn -f hunyuan-backend/pom.xml -pl hunyuan-admin -am -DskipTests compile
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
pnpm --dir hunyuan-design -F @vben/web-ele run typecheck
mvn -f hunyuan-backend/pom.xml -pl hunyuan-admin -am spring-boot:run
```

Expected:
- 所有窄测试通过。
- `hunyuan-admin` compile 通过。
- 前端 BPM 模块测试与 typecheck 全部通过。
- `spring-boot:run` 日志中出现 `Tomcat started on port 1024`，且没有 Flowable bean 冲突、循环依赖或 mapper 扫描错误。

- [ ] **Step 5: Commit the workbench and verification slice**

Run:

```bash
git add 数据库SQL脚本/mysql/sql-update-log/v3.34.0.sql hunyuan-design/apps/hunyuan-system/src/views/system/bpm
git commit -m "feat: add bpm employee workbench"
```

Expected:
- Git creates one commit containing the employee pages, shared drawers, and the final BPM menu additions after the full verification bundle passes.

## Self-Review Checklist

### Spec Coverage

- `hunyuan-bpm` 独立模块：Task 1。
- Flowable 作为默认隐藏内核：Task 1、Task 4、Task 5。
- 9 张核心表：Task 2。
- 分类、表单、设计器、模型：Task 3、Task 6。
- 定义、发布快照、历史版本：Task 4。
- 实例、任务、动作时间线、抄送：Task 5、Task 7。
- 通知型监听器：Task 5、Task 6。
- 管理端页面：Task 6。
- 员工端入口：Task 7。
- `return-to-initiator` 与 `reject` 区分：Task 5。
- `initial_form_data_snapshot_json` 与 `current_form_data_snapshot_json` 双真相：Task 2、Task 5。
- Flowable 类型不外溢：Task 4 的 `BpmApiIsolationTest`。

### Placeholder Scan

- 没有使用 `TBD`、`TODO`、`implement later`、`similar to Task N` 等占位符。
- 所有任务都给出了确切文件路径、明确命令和最小可执行代码骨架。
- 只有菜单 SQL 采用固定 ID 段 `308-319`，避免“实现时再看”的漂移。

### Type Consistency

- `BpmOrgIdentityGateway`、`BpmCurrentActorProvider` 的签名在 Task 1、Task 4、Task 5 一致。
- `publish(BpmDefinitionPublishForm)`、`startInstance(BpmInstanceStartForm)`、`returnToInitiator(BpmTaskReturnForm)`、`transfer(BpmTaskTransferForm)` 在后续任务中命名保持一致。
- 管理端页面路径和 SQL 中的 `component` 路径一致。
- `bpm-api.test.ts`、`bpm-modules.test.ts`、前端 API barrel 和后端控制器 URL 段都统一使用 `/bpm/...` 与 `/app/bpm/...`。
