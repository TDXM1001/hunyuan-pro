package com.hunyuan.sa.bpm.definition;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.common.enumeration.BpmDefinitionLifecycleStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmDefinitionStartStateEnum;
import com.hunyuan.sa.bpm.engine.compiler.BpmCandidatePrecheckService;
import com.hunyuan.sa.bpm.engine.compiler.CompiledDefinitionArtifact;
import com.hunyuan.sa.bpm.engine.compiler.CompiledNodeSnapshot;
import com.hunyuan.sa.bpm.engine.compiler.BpmSimpleModelPublishValidator;
import com.hunyuan.sa.bpm.engine.compiler.SimpleModelBpmnCompiler;
import com.hunyuan.sa.bpm.engine.compiler.SimpleModelValidator;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessDefinitionGateway;
import com.hunyuan.sa.bpm.module.category.dao.BpmCategoryDao;
import com.hunyuan.sa.bpm.module.category.domain.entity.BpmCategoryEntity;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionDao;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.definition.domain.form.BpmDefinitionPublishForm;
import com.hunyuan.sa.bpm.module.definition.service.BpmDefinitionService;
import com.hunyuan.sa.bpm.module.form.dao.BpmFormDao;
import com.hunyuan.sa.bpm.module.form.domain.entity.BpmFormEntity;
import com.hunyuan.sa.bpm.module.model.dao.BpmModelDao;
import com.hunyuan.sa.bpm.module.model.domain.entity.BpmModelEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmDefinitionPublishServiceTest {

    private BpmDefinitionService bpmDefinitionService;

    private BpmModelDao bpmModelDao;

    private BpmDefinitionDao bpmDefinitionDao;

    private BpmDefinitionNodeDao bpmDefinitionNodeDao;

    @BeforeEach
    void setUp() {
        bpmDefinitionService = new BpmDefinitionService();
        bpmModelDao = Mockito.mock(BpmModelDao.class);
        bpmDefinitionDao = Mockito.mock(BpmDefinitionDao.class);
        bpmDefinitionNodeDao = Mockito.mock(BpmDefinitionNodeDao.class);

        setField(bpmDefinitionService, "bpmModelDao", bpmModelDao);
        setField(bpmDefinitionService, "bpmCategoryDao", Mockito.mock(BpmCategoryDao.class));
        setField(bpmDefinitionService, "bpmFormDao", Mockito.mock(BpmFormDao.class));
        setField(bpmDefinitionService, "bpmDefinitionDao", bpmDefinitionDao);
        setField(bpmDefinitionService, "bpmDefinitionNodeDao", bpmDefinitionNodeDao);
        setField(bpmDefinitionService, "simpleModelValidator", Mockito.mock(SimpleModelValidator.class));
        setField(bpmDefinitionService, "bpmSimpleModelPublishValidator", new BpmSimpleModelPublishValidator());
        BpmOrgIdentityGateway identityGateway = Mockito.mock(BpmOrgIdentityGateway.class);
        BpmCandidatePrecheckService candidatePrecheckService = new BpmCandidatePrecheckService();
        setField(candidatePrecheckService, "bpmOrgIdentityGateway", identityGateway);
        setField(bpmDefinitionService, "bpmCandidatePrecheckService", candidatePrecheckService);
        setField(bpmDefinitionService, "simpleModelBpmnCompiler", Mockito.mock(SimpleModelBpmnCompiler.class));
        setField(bpmDefinitionService, "flowableProcessDefinitionGateway", Mockito.mock(FlowableProcessDefinitionGateway.class));
        setField(bpmDefinitionService, "bpmCurrentActorProvider", Mockito.mock(BpmCurrentActorProvider.class));
        setField(bpmDefinitionService, "bpmOrgIdentityGateway", identityGateway);
        when(bpmModelDao.update(any(BpmModelEntity.class), any(Wrapper.class))).thenReturn(1);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void publishShouldCreateNewImmutableDefinitionAndHistoricalizeOldVersion() {
        BpmModelEntity modelEntity = buildModelEntity();
        BpmCategoryEntity categoryEntity = buildCategoryEntity();
        BpmFormEntity formEntity = buildFormEntity();

        when(bpmModelDao.selectById(1L)).thenReturn(modelEntity);
        when(categoryDao().selectById(7L)).thenReturn(categoryEntity);
        when(formDao().selectById(9L)).thenReturn(formEntity);
        when(validator().validate(any(), any())).thenReturn(ResponseDTO.ok());
        when(compiler().compile(any(), any(), any(), any(), any())).thenReturn(buildArtifact());
        when(gateway().deploy(any(), any(), any())).thenReturn("leave:2:1001");
        when(currentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);
        when(identityGateway().requireEmployee(100L)).thenReturn(new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null));
        when(bpmDefinitionDao.selectMaxVersionByDefinitionKey("leave")).thenReturn(1);
        when(bpmDefinitionDao.insert(any(BpmDefinitionEntity.class))).thenAnswer(invocation -> {
            BpmDefinitionEntity entity = invocation.getArgument(0);
            entity.setDefinitionId(12L);
            return 1;
        });

        BpmDefinitionPublishForm publishForm = new BpmDefinitionPublishForm();
        publishForm.setModelId(1L);

        ResponseDTO<Long> response = bpmDefinitionService.publish(publishForm);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).isEqualTo(12L);

        ArgumentCaptor<BpmDefinitionEntity> insertCaptor = ArgumentCaptor.forClass(BpmDefinitionEntity.class);
        verify(bpmDefinitionDao).insert(insertCaptor.capture());
        BpmDefinitionEntity insertedDefinition = insertCaptor.getValue();
        assertThat(insertedDefinition.getDefinitionVersion()).isEqualTo(2);
        assertThat(insertedDefinition.getLifecycleState()).isEqualTo(BpmDefinitionLifecycleStateEnum.CURRENT.getValue());
        assertThat(insertedDefinition.getStartState()).isEqualTo(BpmDefinitionStartStateEnum.STARTABLE.getValue());
        assertThat(insertedDefinition.getEngineProcessDefinitionId()).isEqualTo("leave:2:1001");
        assertThat(insertedDefinition.getPublishedByNameSnapshot()).isEqualTo("张三");

        ArgumentCaptor<BpmDefinitionEntity> historicalCaptor = ArgumentCaptor.forClass(BpmDefinitionEntity.class);
        ArgumentCaptor<Wrapper> historicalWrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(bpmDefinitionDao).update(historicalCaptor.capture(), historicalWrapperCaptor.capture());
        assertThat(historicalCaptor.getValue().getLifecycleState()).isEqualTo(BpmDefinitionLifecycleStateEnum.HISTORICAL.getValue());
        assertThat(historicalWrapperCaptor.getValue().getSqlSegment())
                .contains("definition_key", "lifecycle_state", "definition_id", "<>");

        InOrder publishOrder = inOrder(compiler(), bpmModelDao, gateway());
        publishOrder.verify(compiler()).compile(any(), any(), any(), any(), any());
        publishOrder.verify(bpmModelDao).update(any(BpmModelEntity.class), any(Wrapper.class));
        publishOrder.verify(gateway()).deploy(any(), any(), any());
        verify(bpmModelDao, times(1)).selectById(1L);

        ArgumentCaptor<BpmModelEntity> claimCaptor = ArgumentCaptor.forClass(BpmModelEntity.class);
        ArgumentCaptor<Wrapper> claimWrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(bpmModelDao).update(claimCaptor.capture(), claimWrapperCaptor.capture());
        assertThat(claimCaptor.getValue().getHasUnpublishedChanges()).isFalse();
        assertThat(claimWrapperCaptor.getValue().getSqlSegment())
                .contains(
                        "model_id",
                        "has_unpublished_changes",
                        "update_time",
                        "model_key",
                        "model_name",
                        "category_id",
                        "form_type",
                        "form_id",
                        "simple_model_json",
                        "start_rule_json",
                        "manager_scope_json",
                        "title_rule_json",
                        "summary_rule_json",
                        "variable_mapping_json",
                        "instance_no_rule_id"
                );
        assertThat(((AbstractWrapper<?, ?, ?>) claimWrapperCaptor.getValue()).getParamNameValuePairs().values())
                .contains(
                        modelEntity.getModelId(),
                        Boolean.TRUE,
                        modelEntity.getUpdateTime(),
                        modelEntity.getModelKey(),
                        modelEntity.getModelName(),
                        modelEntity.getCategoryId(),
                        modelEntity.getFormType(),
                        modelEntity.getFormId(),
                        modelEntity.getSimpleModelJson(),
                        modelEntity.getStartRuleJson(),
                        modelEntity.getManagerScopeJson(),
                        modelEntity.getTitleRuleJson(),
                        modelEntity.getSummaryRuleJson(),
                        modelEntity.getVariableMappingJson(),
                        modelEntity.getInstanceNoRuleId()
                );

        InOrder definitionUpdateOrder = inOrder(bpmDefinitionDao);
        definitionUpdateOrder.verify(bpmDefinitionDao).insert(any(BpmDefinitionEntity.class));
        definitionUpdateOrder.verify(bpmDefinitionDao).update(any(BpmDefinitionEntity.class), any(Wrapper.class));

        ArgumentCaptor<BpmModelEntity> modelUpdateCaptor = ArgumentCaptor.forClass(BpmModelEntity.class);
        verify(bpmModelDao).updateById(modelUpdateCaptor.capture());
        assertThat(modelUpdateCaptor.getValue().getModelId()).isEqualTo(1L);
        assertThat(modelUpdateCaptor.getValue().getPublishedDefinitionId()).isEqualTo(12L);
        assertThat(modelUpdateCaptor.getValue().getHasUnpublishedChanges()).isNull();
    }

    @Test
    void publishShouldKeepFormAndListenerSnapshotsInDefinitionProjection() {
        BpmModelEntity modelEntity = buildModelEntity();
        BpmCategoryEntity categoryEntity = buildCategoryEntity();
        BpmFormEntity formEntity = buildFormEntity();

        when(bpmModelDao.selectById(1L)).thenReturn(modelEntity);
        when(categoryDao().selectById(7L)).thenReturn(categoryEntity);
        when(formDao().selectById(9L)).thenReturn(formEntity);
        when(validator().validate(any(), any())).thenReturn(ResponseDTO.ok());
        when(compiler().compile(any(), any(), any(), any(), any())).thenReturn(buildArtifact());
        when(gateway().deploy(any(), any(), any())).thenReturn("leave:1:1000");
        when(currentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);
        when(identityGateway().requireEmployee(100L)).thenReturn(new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null));
        when(bpmDefinitionDao.selectCurrentByDefinitionKey("leave")).thenReturn(null);
        when(bpmDefinitionDao.selectMaxVersionByDefinitionKey("leave")).thenReturn(null);
        when(bpmDefinitionDao.insert(any(BpmDefinitionEntity.class))).thenAnswer(invocation -> {
            BpmDefinitionEntity entity = invocation.getArgument(0);
            entity.setDefinitionId(21L);
            return 1;
        });

        BpmDefinitionPublishForm publishForm = new BpmDefinitionPublishForm();
        publishForm.setModelId(1L);

        ResponseDTO<Long> response = bpmDefinitionService.publish(publishForm);

        assertThat(response.getOk()).isTrue();

        ArgumentCaptor<BpmDefinitionEntity> definitionCaptor = ArgumentCaptor.forClass(BpmDefinitionEntity.class);
        verify(bpmDefinitionDao).insert(definitionCaptor.capture());
        assertThat(definitionCaptor.getValue().getFormSchemaSnapshotJson()).contains("\"fields\"");

        ArgumentCaptor<BpmDefinitionNodeEntity> nodeCaptor = ArgumentCaptor.forClass(BpmDefinitionNodeEntity.class);
        verify(bpmDefinitionNodeDao).insert(nodeCaptor.capture());
        assertThat(nodeCaptor.getValue().getCompiledNodeSnapshotJson()).contains("\"listeners\"");
    }

    @Test
    void publishShouldRejectEmployeeSelectFieldMismatchBeforeCompileAndDeploy() {
        BpmModelEntity modelEntity = buildModelEntity();
        modelEntity.setSimpleModelJson("{\"nodes\":[{\"nodeKey\":\"task_selected\",\"type\":\"userTask\",\"name\":\"发起时选择审批\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}]}");
        BpmCategoryEntity categoryEntity = buildCategoryEntity();
        BpmFormEntity formEntity = buildFormEntity();
        formEntity.setSchemaJson("{\"fields\":[{\"field\":\"approverEmployeeId\",\"type\":\"input\"}]}");

        when(bpmModelDao.selectById(1L)).thenReturn(modelEntity);
        when(categoryDao().selectById(7L)).thenReturn(categoryEntity);
        when(formDao().selectById(9L)).thenReturn(formEntity);
        when(validator().validate(any(), any())).thenReturn(ResponseDTO.ok());

        BpmDefinitionPublishForm publishForm = new BpmDefinitionPublishForm();
        publishForm.setModelId(1L);

        ResponseDTO<Long> response = bpmDefinitionService.publish(publishForm);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("发起时选择审批").contains("员工单选");
        verify(compiler(), never()).compile(any(), any(), any(), any(), any());
        verify(gateway(), never()).deploy(any(), any(), any());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void publishShouldNotDeployWhenSnapshotClaimFails() {
        BpmModelEntity modelEntity = buildModelEntity();
        BpmCategoryEntity categoryEntity = buildCategoryEntity();
        BpmFormEntity formEntity = buildFormEntity();

        when(bpmModelDao.selectById(1L)).thenReturn(modelEntity);
        when(categoryDao().selectById(7L)).thenReturn(categoryEntity);
        when(formDao().selectById(9L)).thenReturn(formEntity);
        when(validator().validate(any(), any())).thenReturn(ResponseDTO.ok());
        when(compiler().compile(any(), any(), any(), any(), any())).thenReturn(buildArtifact());
        when(bpmModelDao.update(any(BpmModelEntity.class), any(Wrapper.class))).thenReturn(0);
        when(gateway().deploy(any(), any(), any())).thenReturn("leave:1:1000");
        when(bpmDefinitionDao.selectMaxVersionByDefinitionKey("leave")).thenReturn(null);
        when(currentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);
        when(identityGateway().requireEmployee(100L)).thenReturn(new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null));
        when(bpmDefinitionDao.insert(any(BpmDefinitionEntity.class))).thenAnswer(invocation -> {
            BpmDefinitionEntity entity = invocation.getArgument(0);
            entity.setDefinitionId(22L);
            return 1;
        });

        BpmDefinitionPublishForm publishForm = new BpmDefinitionPublishForm();
        publishForm.setModelId(1L);

        ResponseDTO<Long> response = bpmDefinitionService.publish(publishForm);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("模型已发生变更，请刷新后重新发布");
        verify(gateway(), never()).deploy(any(), any(), any());
        verify(bpmDefinitionDao, never()).insert(any(BpmDefinitionEntity.class));
    }

    private BpmModelEntity buildModelEntity() {
        BpmModelEntity entity = new BpmModelEntity();
        entity.setModelId(1L);
        entity.setModelKey("leave");
        entity.setModelName("请假流程");
        entity.setCategoryId(7L);
        entity.setFormType(1);
        entity.setFormId(9L);
        entity.setSimpleModelJson("{\"nodes\":[{\"nodeKey\":\"approve\",\"type\":\"userTask\",\"name\":\"部门审批\",\"approvalMode\":\"single\",\"candidateResolverType\":\"DEPARTMENT_MANAGER\",\"listeners\":[{\"channel\":\"MESSAGE\"}]}]}");
        entity.setStartRuleJson("{\"allowAll\":true}");
        entity.setVariableMappingJson("{\"days\":\"form.days\"}");
        entity.setManagerScopeJson("{\"scope\":\"department\"}");
        entity.setTitleRuleJson("{\"template\":\"请假流程\"}");
        entity.setSummaryRuleJson("{\"fields\":[\"days\"]}");
        entity.setInstanceNoRuleId(3);
        entity.setHasUnpublishedChanges(Boolean.TRUE);
        entity.setUpdateTime(LocalDateTime.of(2026, 7, 10, 10, 30));
        return entity;
    }

    private BpmCategoryEntity buildCategoryEntity() {
        BpmCategoryEntity entity = new BpmCategoryEntity();
        entity.setCategoryId(7L);
        entity.setCategoryName("人事流程");
        return entity;
    }

    private BpmFormEntity buildFormEntity() {
        BpmFormEntity entity = new BpmFormEntity();
        entity.setFormId(9L);
        entity.setFormName("请假表单");
        entity.setSchemaJson("{\"fields\":[{\"field\":\"days\"}]}");
        entity.setLayoutJson("{\"grid\":12}");
        return entity;
    }

    private CompiledDefinitionArtifact buildArtifact() {
        return new CompiledDefinitionArtifact(
                "<definitions><process id=\"leave\" name=\"请假流程\"/></definitions>",
                List.of(new CompiledNodeSnapshot(
                        "approve",
                        "userTask",
                        "部门审批",
                        1,
                        "{\"listeners\":[{\"channel\":\"MESSAGE\"}]}",
                        "{\"listeners\":[{\"channel\":\"MESSAGE\"}],\"candidateResolverType\":\"DEPARTMENT_MANAGER\"}"
                ))
        );
    }

    @SuppressWarnings("unchecked")
    private BpmCategoryDao categoryDao() {
        return (BpmCategoryDao) getFieldValue("bpmCategoryDao");
    }

    @SuppressWarnings("unchecked")
    private BpmFormDao formDao() {
        return (BpmFormDao) getFieldValue("bpmFormDao");
    }

    @SuppressWarnings("unchecked")
    private SimpleModelValidator validator() {
        return (SimpleModelValidator) getFieldValue("simpleModelValidator");
    }

    @SuppressWarnings("unchecked")
    private SimpleModelBpmnCompiler compiler() {
        return (SimpleModelBpmnCompiler) getFieldValue("simpleModelBpmnCompiler");
    }

    @SuppressWarnings("unchecked")
    private FlowableProcessDefinitionGateway gateway() {
        return (FlowableProcessDefinitionGateway) getFieldValue("flowableProcessDefinitionGateway");
    }

    @SuppressWarnings("unchecked")
    private BpmCurrentActorProvider currentActorProvider() {
        return (BpmCurrentActorProvider) getFieldValue("bpmCurrentActorProvider");
    }

    @SuppressWarnings("unchecked")
    private BpmOrgIdentityGateway identityGateway() {
        return (BpmOrgIdentityGateway) getFieldValue("bpmOrgIdentityGateway");
    }

    private Object getFieldValue(String fieldName) {
        try {
            Field field = bpmDefinitionService.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(bpmDefinitionService);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("读取测试字段失败: " + fieldName, ex);
        }
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
