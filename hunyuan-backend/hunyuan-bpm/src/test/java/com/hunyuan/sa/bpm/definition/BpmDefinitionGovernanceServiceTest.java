package com.hunyuan.sa.bpm.definition;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.engine.compiler.BpmSimpleModelPublishValidator;
import com.hunyuan.sa.bpm.engine.compiler.BpmCandidatePrecheckService;
import com.hunyuan.sa.bpm.common.enumeration.BpmDefinitionStartStateEnum;
import com.hunyuan.sa.bpm.engine.compiler.SimpleModelValidator;
import com.hunyuan.sa.bpm.module.category.dao.BpmCategoryDao;
import com.hunyuan.sa.bpm.module.category.domain.entity.BpmCategoryEntity;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionEntity;
import com.hunyuan.sa.bpm.module.definition.domain.form.BpmDefinitionStartScopeSaveForm;
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionDiffVO;
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionValidationReportVO;
import com.hunyuan.sa.bpm.module.form.dao.BpmFormDao;
import com.hunyuan.sa.bpm.module.form.domain.entity.BpmFormEntity;
import com.hunyuan.sa.bpm.module.definition.service.BpmDefinitionService;
import com.hunyuan.sa.bpm.module.model.dao.BpmModelDao;
import com.hunyuan.sa.bpm.module.model.domain.entity.BpmModelEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmDefinitionGovernanceServiceTest {

    private BpmDefinitionService bpmDefinitionService;

    private BpmModelDao bpmModelDao;

    private BpmDefinitionDao bpmDefinitionDao;

    private BpmCategoryDao bpmCategoryDao;

    private BpmFormDao bpmFormDao;

    private BpmOrgIdentityGateway identityGateway;

    @BeforeEach
    void setUp() {
        bpmDefinitionService = new BpmDefinitionService();
        bpmModelDao = Mockito.mock(BpmModelDao.class);
        bpmDefinitionDao = Mockito.mock(BpmDefinitionDao.class);
        bpmCategoryDao = Mockito.mock(BpmCategoryDao.class);
        bpmFormDao = Mockito.mock(BpmFormDao.class);
        identityGateway = Mockito.mock(BpmOrgIdentityGateway.class);

        setField(bpmDefinitionService, "bpmModelDao", bpmModelDao);
        setField(bpmDefinitionService, "bpmDefinitionDao", bpmDefinitionDao);
        setField(bpmDefinitionService, "bpmCategoryDao", bpmCategoryDao);
        setField(bpmDefinitionService, "bpmFormDao", bpmFormDao);
        setField(bpmDefinitionService, "simpleModelValidator", new SimpleModelValidator());
        setField(bpmDefinitionService, "bpmSimpleModelPublishValidator", new BpmSimpleModelPublishValidator());
        BpmCandidatePrecheckService candidatePrecheckService = new BpmCandidatePrecheckService();
        setField(candidatePrecheckService, "bpmOrgIdentityGateway", identityGateway);
        setField(bpmDefinitionService, "bpmCandidatePrecheckService", candidatePrecheckService);
    }

    @Test
    void validateForPublishShouldReturnBlockingFindingWhenUserTaskHasNoResolver() {
        BpmModelEntity model = new BpmModelEntity();
        model.setModelId(10L);
        model.setModelKey("expense_apply");
        model.setModelName("费用申请");
        model.setStartRuleJson("{\"scope\":\"ALL\"}");
        model.setSimpleModelJson("{\"nodes\":[{\"nodeKey\":\"approve\",\"type\":\"userTask\",\"name\":\"主管审批\",\"approvalMode\":\"single\"}]}");
        prepareValidPublishDependencies(model, buildForm(9L, "{\"fields\":[]}"));
        when(bpmModelDao.selectById(10L)).thenReturn(model);

        ResponseDTO<BpmDefinitionValidationReportVO> response = bpmDefinitionService.validateForPublish(10L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getPass()).isFalse();
        assertThat(response.getData().getBlockingCount()).isEqualTo(1);
        assertThat(response.getData().getFindings())
                .anyMatch(item -> "USER_TASK_CANDIDATE_EMPTY".equals(item.getCode()));
    }

    @Test
    void validateForPublishShouldReturnBlockingFindingWhenSimpleModelJsonIsInvalid() {
        BpmModelEntity model = new BpmModelEntity();
        model.setModelId(12L);
        model.setModelKey("expense_apply");
        model.setModelName("费用申请");
        model.setStartRuleJson("{\"scope\":\"ALL\"}");
        model.setSimpleModelJson("{\"nodes\":[");
        when(bpmModelDao.selectById(12L)).thenReturn(model);

        ResponseDTO<BpmDefinitionValidationReportVO> response = bpmDefinitionService.validateForPublish(12L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getPass()).isFalse();
        assertThat(response.getData().getFindings())
                .anyMatch(item -> "SIMPLE_MODEL_JSON_INVALID".equals(item.getCode()));
    }

    @Test
    void validateForPublishShouldBlockWhenEmployeeSelectFieldTypeMismatch() {
        BpmModelEntity model = new BpmModelEntity();
        model.setModelId(13L);
        model.setFormId(9L);
        model.setModelKey("expense_apply");
        model.setModelName("费用申请");
        model.setStartRuleJson("{\"scope\":\"ALL\"}");
        model.setSimpleModelJson("{\"nodes\":[{\"nodeKey\":\"task_selected\",\"type\":\"userTask\",\"name\":\"发起时选择审批\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}]}");

        BpmFormEntity form = buildForm(
                9L,
                "{\"fields\":[{\"field\":\"approverEmployeeId\",\"type\":\"input\"}]}"
        );

        prepareValidPublishDependencies(model, form);
        when(bpmModelDao.selectById(13L)).thenReturn(model);

        ResponseDTO<BpmDefinitionValidationReportVO> response = bpmDefinitionService.validateForPublish(13L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getPass()).isFalse();
        assertThat(response.getData().getFindings())
                .anyMatch(item -> item.getMessage().contains("员工单选"));
    }

    @Test
    void validateForPublishShouldExposeCandidateChecksForSupportedResolvers() throws Exception {
        BpmModelEntity model = new BpmModelEntity();
        model.setModelId(14L);
        model.setFormId(10L);
        model.setModelKey("expense_apply");
        model.setModelName("费用申请");
        model.setStartRuleJson("{\"scope\":\"ALL\"}");
        model.setSimpleModelJson("{\"nodes\":["
                + "{\"nodeKey\":\"task_employee\",\"type\":\"userTask\",\"name\":\"指定员工\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeId\":301},"
                + "{\"nodeKey\":\"task_role\",\"type\":\"userTask\",\"name\":\"角色审批\",\"candidateResolverType\":\"ROLE\",\"roleId\":9},"
                + "{\"nodeKey\":\"task_manager\",\"type\":\"userTask\",\"name\":\"部门主管审批\",\"candidateResolverType\":\"DEPARTMENT_MANAGER\"},"
                + "{\"nodeKey\":\"task_start_employee\",\"type\":\"userTask\",\"name\":\"发起人本人\",\"candidateResolverType\":\"START_EMPLOYEE\"},"
                + "{\"nodeKey\":\"task_start_manager\",\"type\":\"userTask\",\"name\":\"发起人部门主管\",\"candidateResolverType\":\"START_DEPARTMENT_MANAGER\"},"
                + "{\"nodeKey\":\"task_selected\",\"type\":\"userTask\",\"name\":\"发起时自选审批\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}"
                + "]}");

        BpmFormEntity form = buildForm(
                10L,
                "{\"fields\":[{\"field\":\"approverEmployeeId\",\"type\":\"employeeSelect\"}]}"
        );

        prepareValidPublishDependencies(model, form);
        when(bpmModelDao.selectById(14L)).thenReturn(model);
        when(identityGateway.listEmployeeIdsByRoleId(9L)).thenReturn(List.of(301L));
        when(identityGateway.resolveDepartmentManagerEmployeeId(7L)).thenReturn(302L);

        ResponseDTO<BpmDefinitionValidationReportVO> response = bpmDefinitionService.validateForPublish(14L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getPass()).isTrue();

        Method getCandidateChecks = response.getData().getClass().getMethod("getCandidateChecks");
        List<?> candidateChecks = (List<?>) getCandidateChecks.invoke(response.getData());
        assertThat(candidateChecks).hasSize(6);

        Object employeeSelectCheck = candidateChecks.get(5);
        Method getResolverLabel = employeeSelectCheck.getClass().getMethod("getCandidateResolverLabel");
        Method getStatus = employeeSelectCheck.getClass().getMethod("getStatus");
        Method getRequiredConfig = employeeSelectCheck.getClass().getMethod("getRequiredConfig");
        Method getRequiresRuntimeFormData = employeeSelectCheck.getClass().getMethod("getRequiresRuntimeFormData");

        assertThat(getResolverLabel.invoke(employeeSelectCheck)).isEqualTo("发起时自选审批人");
        assertThat(getStatus.invoke(employeeSelectCheck)).isEqualTo("RUNTIME_REQUIRED");
        assertThat(String.valueOf(getRequiredConfig.invoke(employeeSelectCheck))).contains("approverEmployeeId");
        assertThat(getRequiresRuntimeFormData.invoke(employeeSelectCheck)).isEqualTo(Boolean.TRUE);
    }

    @Test
    void validateForPublishShouldBlockUnsupportedApprovalMode() {
        BpmModelEntity model = new BpmModelEntity();
        model.setModelId(15L);
        model.setModelKey("expense_apply");
        model.setModelName("费用申请");
        model.setStartRuleJson("{\"scope\":\"ALL\"}");
        model.setSimpleModelJson("{\"nodes\":[{\"nodeKey\":\"approve\",\"type\":\"userTask\",\"name\":\"主管审批\",\"approvalMode\":\"parallel\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeId\":301}]}");
        prepareValidPublishDependencies(model, buildForm(11L, "{\"fields\":[]}"));
        when(bpmModelDao.selectById(15L)).thenReturn(model);

        ResponseDTO<BpmDefinitionValidationReportVO> response = bpmDefinitionService.validateForPublish(15L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getPass()).isFalse();
        assertThat(response.getData().getFindings())
                .anyMatch(item -> item.getMessage().contains("当前只支持单人审批、顺序审批或并行全员会签"));
    }

    @Test
    void validateForPublishShouldBlockInvalidStartRuleJson() {
        BpmModelEntity model = new BpmModelEntity();
        model.setModelId(16L);
        model.setModelKey("expense_apply");
        model.setModelName("费用申请");
        model.setStartRuleJson("{");
        model.setSimpleModelJson("{\"nodes\":[{\"nodeKey\":\"approve\",\"type\":\"userTask\",\"name\":\"主管审批\",\"approvalMode\":\"single\",\"candidateResolverType\":\"START_EMPLOYEE\"}]}");
        prepareValidPublishDependencies(model, buildForm(12L, "{\"fields\":[]}"));
        when(bpmModelDao.selectById(16L)).thenReturn(model);

        ResponseDTO<BpmDefinitionValidationReportVO> response = bpmDefinitionService.validateForPublish(16L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getPass()).isFalse();
        assertThat(response.getData().getFindings())
                .anyMatch(item -> item.getMessage().contains("发起规则 JSON 不合法"));
    }

    @Test
    void validateForPublishShouldBlockMissingCategory() {
        BpmModelEntity model = new BpmModelEntity();
        model.setModelId(17L);
        model.setModelKey("expense_apply");
        model.setModelName("费用申请");
        model.setCategoryId(7L);
        model.setFormId(13L);
        model.setStartRuleJson("{\"scope\":\"ALL\"}");
        model.setSimpleModelJson("{\"nodes\":[{\"nodeKey\":\"approve\",\"type\":\"userTask\",\"name\":\"主管审批\",\"approvalMode\":\"single\",\"candidateResolverType\":\"START_EMPLOYEE\"}]}");
        when(bpmModelDao.selectById(17L)).thenReturn(model);
        when(bpmFormDao.selectById(13L)).thenReturn(buildForm(13L, "{\"fields\":[]}"));

        ResponseDTO<BpmDefinitionValidationReportVO> response = bpmDefinitionService.validateForPublish(17L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getPass()).isFalse();
        assertThat(response.getData().getFindings())
                .anyMatch(item -> "CATEGORY_NOT_FOUND".equals(item.getCode()));
    }

    @Test
    void validateForPublishShouldBlockMissingForm() {
        BpmModelEntity model = new BpmModelEntity();
        model.setModelId(18L);
        model.setModelKey("expense_apply");
        model.setModelName("费用申请");
        model.setCategoryId(7L);
        model.setFormId(14L);
        model.setStartRuleJson("{\"scope\":\"ALL\"}");
        model.setSimpleModelJson("{\"nodes\":[{\"nodeKey\":\"approve\",\"type\":\"userTask\",\"name\":\"主管审批\",\"approvalMode\":\"single\",\"candidateResolverType\":\"START_EMPLOYEE\"}]}");
        when(bpmModelDao.selectById(18L)).thenReturn(model);
        when(bpmCategoryDao.selectById(7L)).thenReturn(buildCategory(7L));

        ResponseDTO<BpmDefinitionValidationReportVO> response = bpmDefinitionService.validateForPublish(18L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getPass()).isFalse();
        assertThat(response.getData().getFindings())
                .anyMatch(item -> "FORM_NOT_FOUND".equals(item.getCode()));
    }

    @Test
    void previewPublishDiffShouldReportChangedSimpleModelAndStartRule() {
        BpmModelEntity model = new BpmModelEntity();
        model.setModelId(11L);
        model.setModelKey("expense_apply");
        model.setSimpleModelJson("{\"nodes\":[{\"nodeKey\":\"approve2\"}]}");
        model.setStartRuleJson("{\"scope\":\"ROLE\"}");
        model.setPublishedDefinitionId(21L);

        BpmDefinitionEntity previous = new BpmDefinitionEntity();
        previous.setDefinitionId(21L);
        previous.setDefinitionVersion(3);
        previous.setSimpleModelSnapshotJson("{\"nodes\":[{\"nodeKey\":\"approve1\"}]}");
        previous.setStartRuleSnapshotJson("{\"scope\":\"ALL\"}");

        when(bpmModelDao.selectById(11L)).thenReturn(model);
        when(bpmDefinitionDao.selectById(21L)).thenReturn(previous);

        ResponseDTO<BpmDefinitionDiffVO> response = bpmDefinitionService.previewPublishDiff(11L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getPreviousDefinitionId()).isEqualTo(21L);
        assertThat(response.getData().getPreviousVersion()).isEqualTo(3);
        assertThat(response.getData().getChangedItems())
                .contains("流程节点设计已变化", "发起规则已变化");
    }

    @Test
    void saveStartScopeAndStartStateShouldUpdateDefinitionGovernanceFields() {
        BpmDefinitionEntity definition = new BpmDefinitionEntity();
        definition.setDefinitionId(21L);
        when(bpmDefinitionDao.selectById(21L)).thenReturn(definition);

        BpmDefinitionStartScopeSaveForm form = new BpmDefinitionStartScopeSaveForm();
        form.setDefinitionId(21L);
        form.setStartScopeJson("{\"type\":\"EMPLOYEE\",\"employeeIds\":[100]}");

        ResponseDTO<String> saveResponse = bpmDefinitionService.saveStartScope(form);
        ResponseDTO<String> suspendResponse = bpmDefinitionService.suspendStart(21L);
        ResponseDTO<String> enableResponse = bpmDefinitionService.enableStart(21L);

        assertThat(saveResponse.getOk()).isTrue();
        assertThat(suspendResponse.getOk()).isTrue();
        assertThat(enableResponse.getOk()).isTrue();

        ArgumentCaptor<BpmDefinitionEntity> updateCaptor = ArgumentCaptor.forClass(BpmDefinitionEntity.class);
        verify(bpmDefinitionDao, Mockito.times(3)).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getAllValues().get(0).getStartScopeJson())
                .isEqualTo("{\"type\":\"EMPLOYEE\",\"employeeIds\":[100]}");
        assertThat(updateCaptor.getAllValues().get(1).getStartState())
                .isEqualTo(BpmDefinitionStartStateEnum.SUSPENDED.getValue());
        assertThat(updateCaptor.getAllValues().get(2).getStartState())
                .isEqualTo(BpmDefinitionStartStateEnum.STARTABLE.getValue());
    }

    private void prepareValidPublishDependencies(BpmModelEntity model, BpmFormEntity form) {
        model.setCategoryId(7L);
        model.setFormId(form.getFormId());
        when(bpmCategoryDao.selectById(7L)).thenReturn(buildCategory(7L));
        when(bpmFormDao.selectById(form.getFormId())).thenReturn(form);
    }

    private BpmCategoryEntity buildCategory(Long categoryId) {
        BpmCategoryEntity category = new BpmCategoryEntity();
        category.setCategoryId(categoryId);
        category.setCategoryName("通用流程");
        return category;
    }

    private BpmFormEntity buildForm(Long formId, String schemaJson) {
        BpmFormEntity form = new BpmFormEntity();
        form.setFormId(formId);
        form.setFormName("通用表单");
        form.setSchemaJson(schemaJson);
        return form;
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
