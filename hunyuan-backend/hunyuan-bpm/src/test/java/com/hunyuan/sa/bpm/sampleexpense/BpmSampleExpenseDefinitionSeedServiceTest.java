package com.hunyuan.sa.bpm.sampleexpense;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
        currentDefinition.setFormSchemaSnapshotJson("{\"fields\":[{\"field\":\"approvedAmount\"}]}");
        currentDefinition.setCompiledBpmnXml("${execution.getVariable('route_sample_post_route_archive_confirm') == true}");
        currentDefinition.setSimpleModelSnapshotJson("""
                {"schemaVersion":2,"nodes":[
                  {"nodeKey":"sample_amount_route","type":"EXCLUSIVE_BRANCH","branches":[
                    {"branchKey":"small","nodes":[{"nodeKey":"sample_finance_review","type":"USER_TASK","fieldPermissions":[{"fieldKey":"approvedAmount","permission":"EDITABLE","required":true}],"listeners":[{"channel":"MESSAGE"}]}]},
                    {"branchKey":"default","isDefault":true,"nodes":[]}
                  ]},
                  {"nodeKey":"sample_post_route","type":"INCLUSIVE_BRANCH","branches":[
                    {"branchKey":"archive","nodes":[{"nodeKey":"sample_archive_review","type":"HANDLE_TASK","fieldPermissions":[{"fieldKey":"approvedAmount","permission":"READONLY","required":false}]}]},
                    {"branchKey":"default","isDefault":true,"nodes":[]}
                  ]}
                ]}
                """);
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
    void prepareShouldPublishNewVersionWhenCurrentDefinitionUsesUnsafeRouteExpression() {
        BpmDefinitionEntity currentDefinition = new BpmDefinitionEntity();
        currentDefinition.setDefinitionId(77L);
        currentDefinition.setLifecycleState(BpmDefinitionLifecycleStateEnum.CURRENT.getValue());
        currentDefinition.setStartState(BpmDefinitionStartStateEnum.STARTABLE.getValue());
        currentDefinition.setFormSchemaSnapshotJson("{\"fields\":[{\"field\":\"approvedAmount\"}]}");
        currentDefinition.setCompiledBpmnXml("${route_sample_post_route_archive_confirm == true}");
        currentDefinition.setSimpleModelSnapshotJson("""
                {"schemaVersion":2,"nodes":[
                  {"nodeKey":"sample_amount_route","type":"EXCLUSIVE_BRANCH","branches":[
                    {"branchKey":"small","nodes":[{"nodeKey":"sample_finance_review","type":"USER_TASK","fieldPermissions":[{"fieldKey":"approvedAmount","permission":"EDITABLE","required":true}],"listeners":[{"channel":"MESSAGE"}]}]},
                    {"branchKey":"default","isDefault":true,"nodes":[]}
                  ]},
                  {"nodeKey":"sample_post_route","type":"INCLUSIVE_BRANCH","branches":[
                    {"branchKey":"archive","nodes":[{"nodeKey":"sample_archive_review","type":"HANDLE_TASK","fieldPermissions":[{"fieldKey":"approvedAmount","permission":"READONLY","required":false}]}]},
                    {"branchKey":"default","isDefault":true,"nodes":[]}
                  ]}
                ]}
                """);
        BpmCategoryEntity category = new BpmCategoryEntity();
        category.setCategoryId(10L);
        BpmFormEntity form = new BpmFormEntity();
        form.setFormId(20L);
        BpmModelEntity model = new BpmModelEntity();
        model.setModelId(30L);

        when(definitionDao.selectCurrentByDefinitionKey("sample_expense_apply")).thenReturn(currentDefinition);
        when(categoryDao.selectOne(any(BpmCategoryEntity.class))).thenReturn(category);
        when(formDao.selectOne(any(BpmFormEntity.class))).thenReturn(form);
        when(modelDao.selectOne(any(BpmModelEntity.class))).thenReturn(model);
        when(definitionService.publish(any(BpmDefinitionPublishForm.class))).thenReturn(ResponseDTO.ok(88L));

        ResponseDTO<Long> response = service.prepare();

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).isEqualTo(88L);
        verify(definitionService).publish(any(BpmDefinitionPublishForm.class));
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
        assertThat(formCaptor.getValue().getSchemaJson()).contains("\"requestedAmount\"");
        assertThat(formCaptor.getValue().getSchemaJson()).contains("\"approvedAmount\"");

        ArgumentCaptor<BpmModelEntity> modelCaptor = ArgumentCaptor.forClass(BpmModelEntity.class);
        verify(modelDao).insert(modelCaptor.capture());
        JSONObject simpleModel = JSON.parseObject(modelCaptor.getValue().getSimpleModelJson());
        assertThat(modelCaptor.getValue().getModelKey()).isEqualTo("sample_expense_apply");
        assertThat(modelCaptor.getValue().getModelName()).isEqualTo("样板费用申请");
        assertThat(modelCaptor.getValue().getCategoryId()).isEqualTo(10L);
        assertThat(modelCaptor.getValue().getFormId()).isEqualTo(20L);
        assertThat(simpleModel.getInteger("schemaVersion")).isEqualTo(2);
        assertThat(modelCaptor.getValue().getSimpleModelJson()).contains("\"nodeKey\":\"sample_amount_route\"");
        assertThat(modelCaptor.getValue().getSimpleModelJson()).contains("\"nodeKey\":\"sample_large_parallel\"");
        assertThat(modelCaptor.getValue().getSimpleModelJson()).contains("\"nodeKey\":\"sample_post_route\"");
        assertThat(modelCaptor.getValue().getSimpleModelJson()).contains("\"type\":\"EXCLUSIVE_BRANCH\"");
        assertThat(modelCaptor.getValue().getSimpleModelJson()).contains("\"type\":\"PARALLEL_BRANCH\"");
        assertThat(modelCaptor.getValue().getSimpleModelJson()).contains("\"type\":\"INCLUSIVE_BRANCH\"");
        assertThat(modelCaptor.getValue().getSimpleModelJson()).contains("\"type\":\"HANDLE_TASK\"");
        assertThat(modelCaptor.getValue().getSimpleModelJson()).contains("\"type\":\"COPY_TASK\"");
        assertThat(modelCaptor.getValue().getSimpleModelJson()).contains("\"operator\":\"LTE\",\"compareValue\":5000");
        assertThat(modelCaptor.getValue().getSimpleModelJson()).contains("\"operator\":\"GT\",\"compareValue\":5000");
        assertThat(modelCaptor.getValue().getSimpleModelJson()).contains("\"operator\":\"GTE\",\"compareValue\":10000");
        assertThat(modelCaptor.getValue().getSimpleModelJson()).contains("\"isDefault\":true");
        assertThat(modelCaptor.getValue().getSimpleModelJson()).contains("\"nodeKey\":\"sample_finance_review\"");
        assertThat(modelCaptor.getValue().getSimpleModelJson()).contains("\"nodeKey\":\"sample_archive_review\"");
        assertThat(modelCaptor.getValue().getSimpleModelJson()).contains("\"fieldKey\":\"approvedAmount\"");
        assertThat(modelCaptor.getValue().getSimpleModelJson()).contains("\"permission\":\"EDITABLE\"");
        assertThat(modelCaptor.getValue().getSimpleModelJson()).contains("\"permission\":\"READONLY\"");
        assertThat(modelCaptor.getValue().getSimpleModelJson()).contains("\"candidateResolverType\":\"EMPLOYEE\"");
        assertThat(modelCaptor.getValue().getSimpleModelJson()).contains("\"employeeId\":1");
        assertThat(modelCaptor.getValue().getSimpleModelJson()).contains("\"listeners\":[{\"channel\":\"MESSAGE\"}]");
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
        assertThat(modelUpdateCaptor.getValue().getSimpleModelJson()).contains("\"listeners\":[{\"channel\":\"MESSAGE\"}]");
        assertThat(modelUpdateCaptor.getValue().getHasUnpublishedChanges()).isTrue();
    }

    @Test
    void prepareShouldPublishNewVersionWhenCurrentDefinitionMissesNotificationListener() {
        BpmDefinitionEntity currentDefinition = new BpmDefinitionEntity();
        currentDefinition.setDefinitionId(77L);
        currentDefinition.setLifecycleState(BpmDefinitionLifecycleStateEnum.CURRENT.getValue());
        currentDefinition.setStartState(BpmDefinitionStartStateEnum.STARTABLE.getValue());
        currentDefinition.setSimpleModelSnapshotJson("{\"nodes\":[{\"nodeKey\":\"sample_approve\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeId\":1}]}");
        BpmCategoryEntity category = new BpmCategoryEntity();
        category.setCategoryId(10L);
        BpmFormEntity form = new BpmFormEntity();
        form.setFormId(20L);
        BpmModelEntity model = new BpmModelEntity();
        model.setModelId(30L);

        when(definitionDao.selectCurrentByDefinitionKey("sample_expense_apply")).thenReturn(currentDefinition);
        when(categoryDao.selectOne(any(BpmCategoryEntity.class))).thenReturn(category);
        when(formDao.selectOne(any(BpmFormEntity.class))).thenReturn(form);
        when(modelDao.selectOne(any(BpmModelEntity.class))).thenReturn(model);
        when(definitionService.publish(any(BpmDefinitionPublishForm.class))).thenReturn(ResponseDTO.ok(88L));

        ResponseDTO<Long> response = service.prepare();

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).isEqualTo(88L);
        ArgumentCaptor<BpmModelEntity> modelUpdateCaptor = ArgumentCaptor.forClass(BpmModelEntity.class);
        verify(modelDao).updateById(modelUpdateCaptor.capture());
        assertThat(modelUpdateCaptor.getValue().getSimpleModelJson()).contains("\"listeners\":[{\"channel\":\"MESSAGE\"}]");
        verify(definitionService).publish(any(BpmDefinitionPublishForm.class));
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
