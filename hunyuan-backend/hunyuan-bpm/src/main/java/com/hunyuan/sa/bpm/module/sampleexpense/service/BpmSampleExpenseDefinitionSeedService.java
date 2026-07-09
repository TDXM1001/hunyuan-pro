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

    /**
     * 构造最小单节点审批草稿，后续统一交给发布服务生成定义和 Flowable 部署。
     */
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
