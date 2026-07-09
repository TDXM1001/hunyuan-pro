# BPM Sample Expense Definition Seed Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a controlled, idempotent seed path that prepares `sample_expense_apply` through Hunyuan BPM's existing publish flow so P2 live acceptance can create a real Flowable-backed sample expense instance.

**Architecture:** The seed lives in `sampleexpense` beside the existing sample business API. It reuses `BpmCategoryDao`, `BpmFormDao`, `BpmModelDao`, `BpmDefinitionDao`, and `BpmDefinitionService.publish`; it does not insert `t_bpm_definition` directly and does not touch Flowable tables. The frontend only gains a small API wrapper and contract pin for the existing sample endpoint family.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, JUnit 5, Mockito, AssertJ, Vue/TypeScript, Vitest, pnpm.

## Global Constraints

- 输出中文、UTF-8。
- 当前分支实现，不创建新分支或 worktree。
- 不新增依赖。
- 按 TDD 执行：生产代码前先写失败测试，并确认失败原因正确。
- 生产发布必须通过 `BpmDefinitionService.publish`，不能直接写 `t_bpm_definition`、`t_bpm_definition_node` 或 Flowable 表。
- seed 只允许显式接口触发，不做应用启动自动初始化。
- seed 只处理样板编码：`bpm_sample`、`sample_expense_form`、`sample_expense_apply`。
- seed 不修改非样板编码数据，不删除历史定义。
- Runtime 证据、截图、MCP 输出继续写入 `G:\code-mcp\playwright-mcp-temp\runtime`，不提交仓库。

---

## File Structure

- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/sampleexpense/service/BpmSampleExpenseDefinitionSeedService.java`
  - One responsibility: prepare the sample BPM category, form, model draft, and publish the model through `BpmDefinitionService`.
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/sampleexpense/BpmSampleExpenseDefinitionSeedServiceTest.java`
  - Unit tests for idempotency, missing-definition publishing, model draft content, and publish failure propagation.
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmSampleExpenseController.java`
  - Add the controlled `POST /bpm/sample/expense/prepareDefinition` endpoint.
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/sample-expense.ts`
  - Add `prepareBpmSampleExpenseDefinition()`.
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`
  - Pin the new frontend API function and backend path.

---

### Task 1: 后端 Seed Service 红绿闭环

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/sampleexpense/BpmSampleExpenseDefinitionSeedServiceTest.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/sampleexpense/service/BpmSampleExpenseDefinitionSeedService.java`

**Interfaces:**
- Consumes:
  - `BpmDefinitionDao.selectCurrentByDefinitionKey(String definitionKey)`
  - `BpmCategoryDao.selectOne(BpmCategoryEntity entity)`
  - `BpmFormDao.selectOne(BpmFormEntity entity)`
  - `BpmModelDao.selectOne(BpmModelEntity entity)`
  - `BpmDefinitionService.publish(BpmDefinitionPublishForm publishForm)`
- Produces:
  - `public ResponseDTO<Long> prepare()`
  - Constants used by tests and later tasks:
    - `DEFINITION_KEY = "sample_expense_apply"`
    - `CATEGORY_CODE = "bpm_sample"`
    - `FORM_KEY = "sample_expense_form"`

- [ ] **Step 1: Write the failing service test**

Create `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/sampleexpense/BpmSampleExpenseDefinitionSeedServiceTest.java`:

```java
package com.hunyuan.sa.bpm.sampleexpense;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.common.enumeration.BpmDefinitionLifecycleStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmDefinitionStartStateEnum;
import com.hunyuan.sa.bpm.module.category.dao.BpmCategoryDao;
import com.hunyuan.sa.bpm.module.category.domain.entity.BpmCategoryEntity;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionEntity;
import com.hunyuan.sa.bpm.module.definition.domain.form.BpmDefinitionPublishForm;
import com.hunyuan.sa.bpm.module.definition.service.BpmDefinitionService;
import com.hunyuan.sa.bpm.module.form.dao.BpmFormDao;
import com.hunyuan.sa.bpm.module.form.domain.entity.BpmFormEntity;
import com.hunyuan.sa.bpm.module.model.dao.BpmModelDao;
import com.hunyuan.sa.bpm.module.model.domain.entity.BpmModelEntity;
import com.hunyuan.sa.bpm.module.sampleexpense.service.BpmSampleExpenseDefinitionSeedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmSampleExpenseDefinitionSeedServiceTest {

    private BpmSampleExpenseDefinitionSeedService service;

    private BpmDefinitionDao definitionDao;

    private BpmCategoryDao categoryDao;

    private BpmFormDao formDao;

    private BpmModelDao modelDao;

    private BpmDefinitionService definitionService;

    @BeforeEach
    void setUp() {
        service = new BpmSampleExpenseDefinitionSeedService();
        definitionDao = Mockito.mock(BpmDefinitionDao.class);
        categoryDao = Mockito.mock(BpmCategoryDao.class);
        formDao = Mockito.mock(BpmFormDao.class);
        modelDao = Mockito.mock(BpmModelDao.class);
        definitionService = Mockito.mock(BpmDefinitionService.class);

        setField(service, "bpmDefinitionDao", definitionDao);
        setField(service, "bpmCategoryDao", categoryDao);
        setField(service, "bpmFormDao", formDao);
        setField(service, "bpmModelDao", modelDao);
        setField(service, "bpmDefinitionService", definitionService);
    }

    @Test
    void prepareShouldReturnCurrentStartableDefinitionWithoutPublishingAgain() {
        BpmDefinitionEntity currentDefinition = new BpmDefinitionEntity();
        currentDefinition.setDefinitionId(77L);
        currentDefinition.setLifecycleState(BpmDefinitionLifecycleStateEnum.CURRENT.getValue());
        currentDefinition.setStartState(BpmDefinitionStartStateEnum.STARTABLE.getValue());
        when(definitionDao.selectCurrentByDefinitionKey("sample_expense_apply")).thenReturn(currentDefinition);

        ResponseDTO<Long> response = service.prepare();

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).isEqualTo(77L);
        verify(categoryDao, never()).insert(any(BpmCategoryEntity.class));
        verify(formDao, never()).insert(any(BpmFormEntity.class));
        verify(modelDao, never()).insert(any(BpmModelEntity.class));
        verify(definitionService, never()).publish(any(BpmDefinitionPublishForm.class));
    }

    @Test
    void prepareShouldCreateSampleModelAndPublishWhenDefinitionMissing() {
        when(definitionDao.selectCurrentByDefinitionKey("sample_expense_apply")).thenReturn(null);
        when(categoryDao.selectOne(any(BpmCategoryEntity.class))).thenReturn(null);
        when(formDao.selectOne(any(BpmFormEntity.class))).thenReturn(null);
        when(modelDao.selectOne(any(BpmModelEntity.class))).thenReturn(null);
        when(categoryDao.insert(any(BpmCategoryEntity.class))).thenAnswer(invocation -> {
            BpmCategoryEntity entity = invocation.getArgument(0);
            entity.setCategoryId(10L);
            return 1;
        });
        when(formDao.insert(any(BpmFormEntity.class))).thenAnswer(invocation -> {
            BpmFormEntity entity = invocation.getArgument(0);
            entity.setFormId(20L);
            return 1;
        });
        when(modelDao.insert(any(BpmModelEntity.class))).thenAnswer(invocation -> {
            BpmModelEntity entity = invocation.getArgument(0);
            entity.setModelId(30L);
            return 1;
        });
        when(definitionService.publish(any(BpmDefinitionPublishForm.class))).thenReturn(ResponseDTO.ok(40L));

        ResponseDTO<Long> response = service.prepare();

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).isEqualTo(40L);

        ArgumentCaptor<BpmCategoryEntity> categoryCaptor = ArgumentCaptor.forClass(BpmCategoryEntity.class);
        verify(categoryDao).insert(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getCategoryCode()).isEqualTo("bpm_sample");
        assertThat(categoryCaptor.getValue().getCategoryName()).isEqualTo("BPM验收样板");
        assertThat(categoryCaptor.getValue().getDisabledFlag()).isFalse();
        assertThat(categoryCaptor.getValue().getDeletedFlag()).isFalse();

        ArgumentCaptor<BpmFormEntity> formCaptor = ArgumentCaptor.forClass(BpmFormEntity.class);
        verify(formDao).insert(formCaptor.capture());
        assertThat(formCaptor.getValue().getFormKey()).isEqualTo("sample_expense_form");
        assertThat(formCaptor.getValue().getSchemaJson()).contains("\"expenseId\"");
        assertThat(formCaptor.getValue().getSchemaJson()).contains("\"amount\"");

        ArgumentCaptor<BpmModelEntity> modelCaptor = ArgumentCaptor.forClass(BpmModelEntity.class);
        verify(modelDao).insert(modelCaptor.capture());
        assertThat(modelCaptor.getValue().getModelKey()).isEqualTo("sample_expense_apply");
        assertThat(modelCaptor.getValue().getModelName()).isEqualTo("样板费用申请");
        assertThat(modelCaptor.getValue().getCategoryId()).isEqualTo(10L);
        assertThat(modelCaptor.getValue().getFormId()).isEqualTo(20L);
        assertThat(modelCaptor.getValue().getSimpleModelJson()).contains("\"nodeKey\":\"sample_approve\"");
        assertThat(modelCaptor.getValue().getSimpleModelJson()).contains("\"candidateResolverType\":\"EMPLOYEE\"");
        assertThat(modelCaptor.getValue().getSimpleModelJson()).contains("\"employeeId\":1");
        assertThat(modelCaptor.getValue().getStartRuleJson()).isEqualTo("{\"allowAll\":true}");
        assertThat(modelCaptor.getValue().getHasUnpublishedChanges()).isTrue();

        ArgumentCaptor<BpmDefinitionPublishForm> publishCaptor = ArgumentCaptor.forClass(BpmDefinitionPublishForm.class);
        verify(definitionService).publish(publishCaptor.capture());
        assertThat(publishCaptor.getValue().getModelId()).isEqualTo(30L);
    }

    @Test
    void prepareShouldReuseExistingSampleRelationAndUpdateModelDraftBeforePublishing() {
        BpmCategoryEntity category = new BpmCategoryEntity();
        category.setCategoryId(10L);
        category.setCategoryCode("bpm_sample");
        BpmFormEntity form = new BpmFormEntity();
        form.setFormId(20L);
        form.setFormKey("sample_expense_form");
        BpmModelEntity model = new BpmModelEntity();
        model.setModelId(30L);
        model.setModelKey("sample_expense_apply");

        when(definitionDao.selectCurrentByDefinitionKey("sample_expense_apply")).thenReturn(null);
        when(categoryDao.selectOne(any(BpmCategoryEntity.class))).thenReturn(category);
        when(formDao.selectOne(any(BpmFormEntity.class))).thenReturn(form);
        when(modelDao.selectOne(any(BpmModelEntity.class))).thenReturn(model);
        when(definitionService.publish(any(BpmDefinitionPublishForm.class))).thenReturn(ResponseDTO.ok(41L));

        ResponseDTO<Long> response = service.prepare();

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).isEqualTo(41L);
        verify(categoryDao, never()).insert(any(BpmCategoryEntity.class));
        verify(formDao, never()).insert(any(BpmFormEntity.class));
        verify(modelDao, never()).insert(any(BpmModelEntity.class));

        ArgumentCaptor<BpmModelEntity> modelUpdateCaptor = ArgumentCaptor.forClass(BpmModelEntity.class);
        verify(modelDao).updateById(modelUpdateCaptor.capture());
        assertThat(modelUpdateCaptor.getValue().getModelId()).isEqualTo(30L);
        assertThat(modelUpdateCaptor.getValue().getCategoryId()).isEqualTo(10L);
        assertThat(modelUpdateCaptor.getValue().getFormId()).isEqualTo(20L);
        assertThat(modelUpdateCaptor.getValue().getSimpleModelJson()).contains("\"employeeId\":1");
        assertThat(modelUpdateCaptor.getValue().getHasUnpublishedChanges()).isTrue();
    }

    @Test
    void prepareShouldReturnPublishFailureMessage() {
        BpmCategoryEntity category = new BpmCategoryEntity();
        category.setCategoryId(10L);
        BpmFormEntity form = new BpmFormEntity();
        form.setFormId(20L);
        BpmModelEntity model = new BpmModelEntity();
        model.setModelId(30L);

        when(definitionDao.selectCurrentByDefinitionKey("sample_expense_apply")).thenReturn(null);
        when(categoryDao.selectOne(any(BpmCategoryEntity.class))).thenReturn(category);
        when(formDao.selectOne(any(BpmFormEntity.class))).thenReturn(form);
        when(modelDao.selectOne(any(BpmModelEntity.class))).thenReturn(model);
        when(definitionService.publish(any(BpmDefinitionPublishForm.class)))
                .thenReturn(ResponseDTO.userErrorParam("流程发布校验未通过"));

        ResponseDTO<Long> response = service.prepare();

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("流程发布校验未通过");
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("设置测试字段失败: " + fieldName, ex);
        }
    }
}
```

- [ ] **Step 2: Run the focused test to verify RED**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmSampleExpenseDefinitionSeedServiceTest' test
```

Expected: FAIL during test compilation because `BpmSampleExpenseDefinitionSeedService` does not exist.

- [ ] **Step 3: Create the minimal seed service**

Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/sampleexpense/service/BpmSampleExpenseDefinitionSeedService.java`:

```java
package com.hunyuan.sa.bpm.module.sampleexpense.service;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.common.enumeration.BpmDefinitionLifecycleStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmDefinitionStartStateEnum;
import com.hunyuan.sa.bpm.module.category.dao.BpmCategoryDao;
import com.hunyuan.sa.bpm.module.category.domain.entity.BpmCategoryEntity;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionEntity;
import com.hunyuan.sa.bpm.module.definition.domain.form.BpmDefinitionPublishForm;
import com.hunyuan.sa.bpm.module.definition.service.BpmDefinitionService;
import com.hunyuan.sa.bpm.module.form.dao.BpmFormDao;
import com.hunyuan.sa.bpm.module.form.domain.entity.BpmFormEntity;
import com.hunyuan.sa.bpm.module.model.dao.BpmModelDao;
import com.hunyuan.sa.bpm.module.model.domain.entity.BpmModelEntity;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BPM 样板费用申请流程定义初始化服务。
 */
@Service
public class BpmSampleExpenseDefinitionSeedService {

    private static final String DEFINITION_KEY = "sample_expense_apply";

    private static final String CATEGORY_CODE = "bpm_sample";

    private static final String FORM_KEY = "sample_expense_form";

    private static final String SIMPLE_MODEL_JSON = "{\"nodes\":[{\"nodeKey\":\"sample_approve\",\"type\":\"userTask\",\"name\":\"样板审批\",\"approvalMode\":\"single\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeId\":1}]}";

    private static final String START_RULE_JSON = "{\"allowAll\":true}";

    private static final String FORM_SCHEMA_JSON = "{\"fields\":[{\"field\":\"expenseId\",\"label\":\"样板费用申请ID\",\"type\":\"number\"},{\"field\":\"amount\",\"label\":\"申请金额\",\"type\":\"number\"}]}";

    private static final String FORM_LAYOUT_JSON = "{\"grid\":12}";

    @Resource
    private BpmDefinitionDao bpmDefinitionDao;

    @Resource
    private BpmCategoryDao bpmCategoryDao;

    @Resource
    private BpmFormDao bpmFormDao;

    @Resource
    private BpmModelDao bpmModelDao;

    @Resource
    private BpmDefinitionService bpmDefinitionService;

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Long> prepare() {
        BpmDefinitionEntity currentDefinition = bpmDefinitionDao.selectCurrentByDefinitionKey(DEFINITION_KEY);
        if (isCurrentStartable(currentDefinition)) {
            return ResponseDTO.ok(currentDefinition.getDefinitionId());
        }

        BpmCategoryEntity category = ensureCategory();
        BpmFormEntity form = ensureForm();
        BpmModelEntity model = ensureModel(category.getCategoryId(), form.getFormId());

        BpmDefinitionPublishForm publishForm = new BpmDefinitionPublishForm();
        publishForm.setModelId(model.getModelId());
        ResponseDTO<Long> publishResponse = bpmDefinitionService.publish(publishForm);
        if (!Boolean.TRUE.equals(publishResponse.getOk())) {
            return ResponseDTO.userErrorParam(publishResponse.getMsg());
        }
        return publishResponse;
    }

    private boolean isCurrentStartable(BpmDefinitionEntity definition) {
        return definition != null
                && BpmDefinitionLifecycleStateEnum.CURRENT.getValue().equals(definition.getLifecycleState())
                && BpmDefinitionStartStateEnum.STARTABLE.getValue().equals(definition.getStartState());
    }

    private BpmCategoryEntity ensureCategory() {
        BpmCategoryEntity query = new BpmCategoryEntity();
        query.setCategoryCode(CATEGORY_CODE);
        query.setDeletedFlag(Boolean.FALSE);
        BpmCategoryEntity existing = bpmCategoryDao.selectOne(query);
        if (existing != null) {
            return existing;
        }

        BpmCategoryEntity entity = new BpmCategoryEntity();
        entity.setCategoryCode(CATEGORY_CODE);
        entity.setCategoryName("BPM验收样板");
        entity.setIcon("ep:connection");
        entity.setSort(0);
        entity.setDisabledFlag(Boolean.FALSE);
        entity.setDeletedFlag(Boolean.FALSE);
        entity.setRemark("BPM 样板费用申请验收流程分类");
        bpmCategoryDao.insert(entity);
        return entity;
    }

    private BpmFormEntity ensureForm() {
        BpmFormEntity query = new BpmFormEntity();
        query.setFormKey(FORM_KEY);
        query.setDeletedFlag(Boolean.FALSE);
        BpmFormEntity existing = bpmFormDao.selectOne(query);
        if (existing != null) {
            return existing;
        }

        BpmFormEntity entity = new BpmFormEntity();
        entity.setFormKey(FORM_KEY);
        entity.setFormName("样板费用申请表单");
        entity.setSchemaJson(FORM_SCHEMA_JSON);
        entity.setLayoutJson(FORM_LAYOUT_JSON);
        entity.setDisabledFlag(Boolean.FALSE);
        entity.setDeletedFlag(Boolean.FALSE);
        entity.setRemark("BPM 样板费用申请验收表单");
        bpmFormDao.insert(entity);
        return entity;
    }

    private BpmModelEntity ensureModel(Long categoryId, Long formId) {
        BpmModelEntity query = new BpmModelEntity();
        query.setModelKey(DEFINITION_KEY);
        query.setDeletedFlag(Boolean.FALSE);
        BpmModelEntity existing = bpmModelDao.selectOne(query);
        if (existing != null) {
            BpmModelEntity update = buildModelDraft(categoryId, formId);
            update.setModelId(existing.getModelId());
            bpmModelDao.updateById(update);
            existing.setCategoryId(categoryId);
            existing.setFormId(formId);
            existing.setSimpleModelJson(SIMPLE_MODEL_JSON);
            existing.setStartRuleJson(START_RULE_JSON);
            existing.setHasUnpublishedChanges(Boolean.TRUE);
            return existing;
        }

        BpmModelEntity entity = buildModelDraft(categoryId, formId);
        entity.setModelKey(DEFINITION_KEY);
        entity.setDeletedFlag(Boolean.FALSE);
        bpmModelDao.insert(entity);
        return entity;
    }

    private BpmModelEntity buildModelDraft(Long categoryId, Long formId) {
        BpmModelEntity entity = new BpmModelEntity();
        entity.setModelName("样板费用申请");
        entity.setCategoryId(categoryId);
        entity.setFormType(1);
        entity.setFormId(formId);
        entity.setVisibleFlag(Boolean.TRUE);
        entity.setSort(0);
        entity.setDescription("BPM 样板费用申请验收流程");
        entity.setSimpleModelJson(SIMPLE_MODEL_JSON);
        entity.setStartRuleJson(START_RULE_JSON);
        entity.setTitleRuleJson("{\"template\":\"样板费用申请\"}");
        entity.setSummaryRuleJson("{\"fields\":[\"amount\"]}");
        entity.setVariableMappingJson("{\"expenseId\":\"form.expenseId\",\"amount\":\"form.amount\"}");
        entity.setHasUnpublishedChanges(Boolean.TRUE);
        return entity;
    }
}
```

- [ ] **Step 4: Run the focused test to verify GREEN**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmSampleExpenseDefinitionSeedServiceTest' test
```

Expected: PASS, 4 tests, 0 failures.

- [ ] **Step 5: Commit Task 1**

Run:

```powershell
git add hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/sampleexpense/BpmSampleExpenseDefinitionSeedServiceTest.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/sampleexpense/service/BpmSampleExpenseDefinitionSeedService.java
git commit -m "feat: 增加 BPM 样板定义初始化服务"
```

---

### Task 2: 管理端接口与前端 API 合同

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmSampleExpenseController.java`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/sample-expense.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`

**Interfaces:**
- Consumes:
  - `BpmSampleExpenseDefinitionSeedService.prepare(): ResponseDTO<Long>`
- Produces:
  - Backend endpoint: `POST /bpm/sample/expense/prepareDefinition`
  - Frontend function: `prepareBpmSampleExpenseDefinition(): Promise<number>`

- [ ] **Step 1: Write the failing frontend API contract test**

Modify the `sampleExpense` needles in `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts` to include the new function and path:

```ts
  {
    label: 'sampleExpense',
    needles: [
      'createBpmSampleExpense',
      '/bpm/sample/expense/create',
      'startBpmSampleExpense',
      '/bpm/sample/expense/start/',
      'getBpmSampleExpenseDetail',
      '/bpm/sample/expense/detail/',
      'markNextBpmSampleExpenseCallbackFailed',
      '/bpm/sample/expense/markNextCallbackFailed/',
      'prepareBpmSampleExpenseDefinition',
      '/bpm/sample/expense/prepareDefinition',
      'BpmSampleExpenseVO',
    ],
    path: 'apps/hunyuan-system/src/api/system/bpm/sample-expense.ts',
  },
```

- [ ] **Step 2: Run frontend contract test to verify RED**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts --dom
```

Expected: FAIL because `sample-expense.ts` does not yet contain `prepareBpmSampleExpenseDefinition` or `/bpm/sample/expense/prepareDefinition`.

- [ ] **Step 3: Add backend controller endpoint**

Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmSampleExpenseController.java`:

```java
import com.hunyuan.sa.bpm.module.sampleexpense.service.BpmSampleExpenseDefinitionSeedService;
```

Add the resource field below `bpmSampleExpenseService`:

```java
    @Resource
    private BpmSampleExpenseDefinitionSeedService bpmSampleExpenseDefinitionSeedService;
```

Add this endpoint before the existing `create` method:

```java
    @Operation(summary = "准备 BPM 样板费用申请流程定义")
    @PostMapping("/bpm/sample/expense/prepareDefinition")
    @SaCheckPermission("bpm:integration:update")
    public ResponseDTO<Long> prepareDefinition() {
        return bpmSampleExpenseDefinitionSeedService.prepare();
    }
```

- [ ] **Step 4: Add frontend API function**

Modify `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/sample-expense.ts` and add this function after the interfaces:

```ts
export async function prepareBpmSampleExpenseDefinition() {
  return requestClient.post<number>('/bpm/sample/expense/prepareDefinition');
}
```

- [ ] **Step 5: Run frontend contract test to verify GREEN**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts --dom
```

Expected: PASS, `bpm-api.test.ts` all tests pass.

- [ ] **Step 6: Run focused backend seed test after controller wiring**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmSampleExpenseDefinitionSeedServiceTest' test
```

Expected: PASS, 4 tests, 0 failures.

- [ ] **Step 7: Commit Task 2**

Run:

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmSampleExpenseController.java hunyuan-design/apps/hunyuan-system/src/api/system/bpm/sample-expense.ts hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts
git commit -m "feat: 接入 BPM 样板定义初始化接口"
```

---

### Task 3: 全量门禁与活体验收前置更新

**Files:**
- Modify: `docs/superpowers/specs/2026-07-09-bpm-p2-live-acceptance.md`
  - Only after live acceptance is rerun; update with new evidence and final status.
- Runtime evidence output only:
  - `G:\code-mcp\playwright-mcp-temp\runtime\hunyuan-p2-live-acceptance-<timestamp>.json`

**Interfaces:**
- Consumes:
  - `POST /bpm/sample/expense/prepareDefinition`
  - existing sample expense API
  - existing callback retry/query API
  - existing instance trace API
- Produces:
  - Fresh gate output
  - Updated acceptance record if live run is no longer blocked

- [ ] **Step 1: Run backend focused gate**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmSampleExpenseDefinitionSeedServiceTest,BpmSampleExpenseServiceTest,BpmSampleExpenseCallbackHandlerTest,BpmBusinessCallbackExecutorTest' test
```

Expected: PASS; all listed tests have 0 failures and 0 errors.

- [ ] **Step 2: Run backend module gate**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test
```

Expected: PASS; no failures and no errors.

- [ ] **Step 3: Run frontend BPM contract and module gate**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: PASS; `bpm-api.test.ts` and `bpm-modules.test.ts` pass.

- [ ] **Step 4: Run frontend typecheck gate**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS; `vue-tsc --noEmit --skipLibCheck` exits 0.

- [ ] **Step 5: Run Flowable boundary gate**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin -Dtest=BpmFlowableCompatibilityTest test
```

Expected: PASS; compatibility test exits 0.

- [ ] **Step 6: Rerun live acceptance with seed preflight**

Use the existing P2 live acceptance flow, but insert this step after admin login and before querying definitions:

```http
POST /api/bpm/sample/expense/prepareDefinition
Authorization: Bearer <admin-token>
```

Expected response:

```json
{
  "ok": true,
  "data": 123
}
```

Then query definitions:

```http
POST /api/bpm/definition/query
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "definitionKey": "sample_expense_apply",
  "pageNum": 1,
  "pageSize": 10
}
```

Expected: `total >= 1`, and at least one row has `definitionKey = "sample_expense_apply"` and start state is可发起.

Continue the existing live acceptance:

- `POST /api/bpm/sample/expense/create`
- `POST /api/bpm/sample/expense/markNextCallbackFailed/{expenseId}`
- `POST /api/bpm/sample/expense/start/{expenseId}`
- approve the generated task through existing app/admin flow
- `POST /api/bpm/integration/callback/query`
- `POST /api/bpm/integration/callback/retry/{callbackRecordId}`
- `GET /api/bpm/sample/expense/detail/{expenseId}`
- `GET /api/bpm/instance/trace/{instanceId}`

Runtime evidence must stay under `G:\code-mcp\playwright-mcp-temp\runtime`.

- [ ] **Step 7: Update acceptance record after live rerun**

If live acceptance completes, modify `docs/superpowers/specs/2026-07-09-bpm-p2-live-acceptance.md`:

```markdown
## 结论

P2 收官活体验收通过。

本轮通过 `POST /bpm/sample/expense/prepareDefinition` 显式准备 `sample_expense_apply` 流程定义，定义由现有 `BpmDefinitionService.publish` 发布，Hunyuan 定义快照与 Flowable 部署保持一致。随后完成样板费用申请创建、失败注入、发起、审批、失败回调查询、手动重试、样板详情查询和实例 trace 可靠性检查。
```

Add the new runtime evidence file paths in the environment/evidence section, including the file that records the seed response and the final live acceptance response.

If live acceptance still blocks, keep the conclusion as blocked and replace the old blocker with the new concrete blocker, including exact endpoint, response, and timestamp.

- [ ] **Step 8: Commit Task 3**

If only code/tests changed and live acceptance is not rerun in this task, commit code gates:

```powershell
git status --short
git add hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/sampleexpense/BpmSampleExpenseDefinitionSeedServiceTest.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/sampleexpense/service/BpmSampleExpenseDefinitionSeedService.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmSampleExpenseController.java hunyuan-design/apps/hunyuan-system/src/api/system/bpm/sample-expense.ts hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts
git commit -m "test: 验证 BPM 样板定义初始化闭环"
```

If live acceptance record is updated, commit docs separately:

```powershell
git add docs/superpowers/specs/2026-07-09-bpm-p2-live-acceptance.md
git commit -m "docs: 更新 BPM P2 收官活体验收记录"
```

---

## Self-Review

**Spec coverage:**

- Explicit seed trigger is covered by Task 2.
- Idempotent current-definition return is covered by Task 1 test `prepareShouldReturnCurrentStartableDefinitionWithoutPublishingAgain`.
- Missing-definition category/form/model creation and publish are covered by Task 1 test `prepareShouldCreateSampleModelAndPublishWhenDefinitionMissing`.
- Existing sample relation reuse is covered by Task 1 test `prepareShouldReuseExistingSampleRelationAndUpdateModelDraftBeforePublishing`.
- Publish failure propagation is covered by Task 1 test `prepareShouldReturnPublishFailureMessage`.
- Frontend API contract is covered by Task 2.
- Live acceptance preflight and evidence update are covered by Task 3.

**Marker scan:**

- The plan contains no unfinished markers or unnamed edge handling steps.

**Type consistency:**

- Service signature is consistently `ResponseDTO<Long> prepare()`.
- Backend endpoint is consistently `POST /bpm/sample/expense/prepareDefinition`.
- Frontend function is consistently `prepareBpmSampleExpenseDefinition()`.
- Definition key is consistently `sample_expense_apply`.
