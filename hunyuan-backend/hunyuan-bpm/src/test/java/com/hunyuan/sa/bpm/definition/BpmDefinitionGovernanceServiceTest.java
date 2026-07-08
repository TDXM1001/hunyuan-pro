package com.hunyuan.sa.bpm.definition;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.common.enumeration.BpmDefinitionStartStateEnum;
import com.hunyuan.sa.bpm.engine.compiler.SimpleModelValidator;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionEntity;
import com.hunyuan.sa.bpm.module.definition.domain.form.BpmDefinitionStartScopeSaveForm;
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionDiffVO;
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionValidationReportVO;
import com.hunyuan.sa.bpm.module.definition.service.BpmDefinitionService;
import com.hunyuan.sa.bpm.module.model.dao.BpmModelDao;
import com.hunyuan.sa.bpm.module.model.domain.entity.BpmModelEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmDefinitionGovernanceServiceTest {

    private BpmDefinitionService bpmDefinitionService;

    private BpmModelDao bpmModelDao;

    private BpmDefinitionDao bpmDefinitionDao;

    @BeforeEach
    void setUp() {
        bpmDefinitionService = new BpmDefinitionService();
        bpmModelDao = Mockito.mock(BpmModelDao.class);
        bpmDefinitionDao = Mockito.mock(BpmDefinitionDao.class);

        setField(bpmDefinitionService, "bpmModelDao", bpmModelDao);
        setField(bpmDefinitionService, "bpmDefinitionDao", bpmDefinitionDao);
        setField(bpmDefinitionService, "simpleModelValidator", new SimpleModelValidator());
    }

    @Test
    void validateForPublishShouldReturnBlockingFindingWhenUserTaskHasNoResolver() {
        BpmModelEntity model = new BpmModelEntity();
        model.setModelId(10L);
        model.setModelKey("expense_apply");
        model.setModelName("费用申请");
        model.setStartRuleJson("{\"scope\":\"ALL\"}");
        model.setSimpleModelJson("{\"nodes\":[{\"nodeKey\":\"approve\",\"type\":\"userTask\",\"name\":\"主管审批\",\"approvalMode\":\"single\"}]}");
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
