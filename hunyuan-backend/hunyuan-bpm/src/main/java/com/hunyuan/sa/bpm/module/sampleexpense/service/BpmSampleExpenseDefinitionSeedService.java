package com.hunyuan.sa.bpm.module.sampleexpense.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.common.enumeration.BpmDefinitionLifecycleStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmDefinitionStartStateEnum;
import com.hunyuan.sa.bpm.engine.ast.HumanTaskNode;
import com.hunyuan.sa.bpm.engine.ast.ProcessAst;
import com.hunyuan.sa.bpm.engine.ast.ProcessNode;
import com.hunyuan.sa.bpm.engine.ast.ProcessNodeType;
import com.hunyuan.sa.bpm.engine.compiler.ProcessAstParser;
import com.hunyuan.sa.bpm.engine.compiler.ProcessAstWalker;
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
import org.springframework.util.StringUtils;

/**
 * BPM 样板费用申请流程定义初始化服务。
 */
@Service
public class BpmSampleExpenseDefinitionSeedService {

    private static final String DEFINITION_KEY = "sample_expense_apply";

    private static final String CATEGORY_CODE = "bpm_sample";

    private static final String FORM_KEY = "sample_expense_form";

    private static final String APPROVE_NODE_KEY = "sample_finance_review";

    private static final String ARCHIVE_NODE_KEY = "sample_archive_review";

    private static final String NOTIFICATION_CHANNEL_MESSAGE = "MESSAGE";

    private static final String SIMPLE_MODEL_JSON = """
            {"schemaVersion":2,"settings":{"maxBranchDepth":3},"nodes":[
              {"nodeKey":"sample_amount_route","type":"EXCLUSIVE_BRANCH","name":"申请金额路由","branches":[
                {"branchKey":"small_amount","name":"小额费用","condition":{"sourceType":"FORM_FIELD","fieldKey":"requestedAmount","valueType":"NUMBER","operator":"LTE","compareValue":5000},"nodes":[
                  {"nodeKey":"sample_finance_review","type":"USER_TASK","name":"小额财务核定","approvalMode":"single","candidateResolverType":"EMPLOYEE","employeeId":1,"fieldPermissions":[{"fieldKey":"approvedAmount","permission":"EDITABLE","required":true}],"listeners":[{"channel":"MESSAGE"}]}
                ]},
                {"branchKey":"large_amount","name":"大额费用","condition":{"sourceType":"FORM_FIELD","fieldKey":"requestedAmount","valueType":"NUMBER","operator":"GT","compareValue":5000},"nodes":[
                  {"nodeKey":"sample_large_parallel","type":"PARALLEL_BRANCH","name":"大额费用独立复核","branches":[
                    {"branchKey":"large_finance","name":"财务复核","nodes":[
                      {"nodeKey":"sample_large_finance_review","type":"USER_TASK","name":"大额财务核定","approvalMode":"single","candidateResolverType":"EMPLOYEE","employeeId":1,"fieldPermissions":[{"fieldKey":"approvedAmount","permission":"READONLY","required":false}],"listeners":[{"channel":"MESSAGE"}]}
                    ]},
                    {"branchKey":"large_risk","name":"风险复核","nodes":[
                      {"nodeKey":"sample_risk_review","type":"USER_TASK","name":"风险复核","approvalMode":"single","candidateResolverType":"EMPLOYEE","employeeId":1,"fieldPermissions":[{"fieldKey":"approvedAmount","permission":"READONLY","required":false}],"listeners":[]}
                    ]}
                  ]}
                ]},
                {"branchKey":"manual_check","name":"人工核验","isDefault":true,"nodes":[
                  {"nodeKey":"sample_manual_handle","type":"HANDLE_TASK","name":"人工核验办理","candidateResolverType":"EMPLOYEE","employeeId":1,"fieldPermissions":[{"fieldKey":"approvedAmount","permission":"READONLY","required":false}],"listeners":[]}
                ]}
              ]},
              {"nodeKey":"sample_post_route","type":"INCLUSIVE_BRANCH","name":"汇合后通知","branches":[
                {"branchKey":"finance_copy","name":"高额财务抄送","condition":{"sourceType":"FORM_FIELD","fieldKey":"requestedAmount","valueType":"NUMBER","operator":"GTE","compareValue":10000},"nodes":[
                  {"nodeKey":"sample_finance_copy","type":"COPY_TASK","name":"财务抄送","candidateResolverType":"EMPLOYEE","employeeIds":[1]}
                ]},
                {"branchKey":"archive_confirm","name":"归档确认","condition":{"sourceType":"FORM_FIELD","fieldKey":"requestedAmount","valueType":"NUMBER","operator":"GTE","compareValue":0},"nodes":[
                  {"nodeKey":"sample_archive_review","type":"HANDLE_TASK","name":"归档确认","candidateResolverType":"EMPLOYEE","employeeId":1,"fieldPermissions":[{"fieldKey":"approvedAmount","permission":"READONLY","required":false}],"listeners":[]}
                ]},
                {"branchKey":"missing_amount_archive","name":"缺值归档确认","isDefault":true,"nodes":[
                  {"nodeKey":"sample_missing_amount_archive","type":"HANDLE_TASK","name":"缺值归档确认","candidateResolverType":"EMPLOYEE","employeeId":1,"fieldPermissions":[{"fieldKey":"approvedAmount","permission":"READONLY","required":false}],"listeners":[]}
                ]}
              ]}
            ]}
            """;

    private static final String START_RULE_JSON = "{\"allowAll\":true}";

    private static final String FORM_SCHEMA_JSON = "{\"fields\":[{\"field\":\"expenseId\",\"label\":\"样板费用申请ID\",\"type\":\"number\"},{\"field\":\"requestedAmount\",\"label\":\"申请金额\",\"type\":\"number\"},{\"field\":\"approvedAmount\",\"label\":\"核定金额\",\"type\":\"number\"}]}";

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
        if (isCurrentStartable(currentDefinition) && hasApprovalDataGovernance(currentDefinition)) {
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

    /**
     * 旧样板定义可能已经可发起，但缺少 v2 嵌套路由或数据治理配置；此时需要发布一次新版。
     */
    private boolean hasApprovalDataGovernance(BpmDefinitionEntity definition) {
        if (definition == null || !StringUtils.hasText(definition.getSimpleModelSnapshotJson())) {
            return false;
        }
        if (!StringUtils.hasText(definition.getFormSchemaSnapshotJson())
                || !definition.getFormSchemaSnapshotJson().contains("\"field\":\"approvedAmount\"")) {
            return false;
        }
        if (!StringUtils.hasText(definition.getCompiledBpmnXml())
                || !definition.getCompiledBpmnXml().contains("execution.getVariable('route_")) {
            return false;
        }
        try {
            ProcessAst processAst = new ProcessAstParser().parse(definition.getSimpleModelSnapshotJson());
            if (processAst.schemaVersion() < 2) {
                return false;
            }
            boolean financeReady = false;
            boolean archiveReady = false;
            for (ProcessNode processNode : new ProcessAstWalker().walk(processAst)) {
                if (!(processNode instanceof HumanTaskNode humanTaskNode)) {
                    continue;
                }
                JSONArray permissions = configurationArray(humanTaskNode, "fieldPermissions");
                if (APPROVE_NODE_KEY.equals(humanTaskNode.nodeKey())
                        && humanTaskNode.type() == ProcessNodeType.USER_TASK) {
                    boolean editable = hasPermission(permissions, "approvedAmount", "EDITABLE", true);
                    JSONArray listeners = configurationArray(humanTaskNode, "listeners");
                    boolean messageListener = false;
                    if (listeners != null) {
                        for (int listenerIndex = 0; listenerIndex < listeners.size(); listenerIndex++) {
                            JSONObject listenerObject = listeners.getJSONObject(listenerIndex);
                            if (listenerObject != null
                                    && NOTIFICATION_CHANNEL_MESSAGE.equals(listenerObject.getString("channel"))) {
                                messageListener = true;
                                break;
                            }
                        }
                    }
                    financeReady = editable && messageListener;
                } else if (ARCHIVE_NODE_KEY.equals(humanTaskNode.nodeKey())
                        && humanTaskNode.type() == ProcessNodeType.HANDLE_TASK) {
                    archiveReady = hasPermission(permissions, "approvedAmount", "READONLY", false);
                }
            }
            return financeReady && archiveReady;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private JSONArray configurationArray(HumanTaskNode node, String key) {
        Object value = node.configuration().get(key);
        return value instanceof JSONArray array ? array : null;
    }

    private boolean hasPermission(JSONArray permissions, String fieldKey, String mode, boolean required) {
        if (permissions == null) {
            return false;
        }
        for (int index = 0; index < permissions.size(); index++) {
            JSONObject permission = permissions.getJSONObject(index);
            if (permission != null
                    && fieldKey.equals(permission.getString("fieldKey"))
                    && mode.equals(permission.getString("permission"))
                    && required == permission.getBooleanValue("required")) {
                return true;
            }
        }
        return false;
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
            BpmFormEntity update = new BpmFormEntity();
            update.setFormId(existing.getFormId());
            update.setFormName("样板费用申请表单");
            update.setSchemaJson(FORM_SCHEMA_JSON);
            update.setLayoutJson(FORM_LAYOUT_JSON);
            update.setDisabledFlag(Boolean.FALSE);
            update.setDeletedFlag(Boolean.FALSE);
            bpmFormDao.updateById(update);
            existing.setSchemaJson(FORM_SCHEMA_JSON);
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
     * 构造 M1 多路径样板草稿，后续统一交给发布服务生成定义和 Flowable 部署。
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
        entity.setSummaryRuleJson("{\"fields\":[\"requestedAmount\",\"approvedAmount\"]}");
        entity.setVariableMappingJson("{\"expenseId\":\"form.expenseId\",\"requestedAmount\":\"form.requestedAmount\",\"approvedAmount\":\"form.approvedAmount\"}");
        entity.setHasUnpublishedChanges(Boolean.TRUE);
        return entity;
    }
}
