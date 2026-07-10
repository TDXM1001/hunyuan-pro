package com.hunyuan.sa.bpm.module.definition.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartPageUtil;
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
import com.hunyuan.sa.bpm.module.definition.domain.form.BpmDefinitionQueryForm;
import com.hunyuan.sa.bpm.module.definition.domain.form.BpmDefinitionStartScopeSaveForm;
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionDiffVO;
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionDetailVO;
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionValidationReportVO;
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionVO;
import com.hunyuan.sa.bpm.module.form.dao.BpmFormDao;
import com.hunyuan.sa.bpm.module.form.domain.entity.BpmFormEntity;
import com.hunyuan.sa.bpm.module.model.dao.BpmModelDao;
import com.hunyuan.sa.bpm.module.model.domain.entity.BpmModelEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 流程定义服务。
 */
@Service
public class BpmDefinitionService {

    @Resource
    private BpmModelDao bpmModelDao;

    @Resource
    private BpmCategoryDao bpmCategoryDao;

    @Resource
    private BpmFormDao bpmFormDao;

    @Resource
    private BpmDefinitionDao bpmDefinitionDao;

    @Resource
    private BpmDefinitionNodeDao bpmDefinitionNodeDao;

    @Resource
    private SimpleModelValidator simpleModelValidator;

    @Resource
    private BpmSimpleModelPublishValidator bpmSimpleModelPublishValidator;

    @Resource
    private BpmCandidatePrecheckService bpmCandidatePrecheckService;

    @Resource
    private SimpleModelBpmnCompiler simpleModelBpmnCompiler;

    @Resource
    private FlowableProcessDefinitionGateway flowableProcessDefinitionGateway;

    @Resource
    private BpmCurrentActorProvider bpmCurrentActorProvider;

    @Resource
    private BpmOrgIdentityGateway bpmOrgIdentityGateway;

    public ResponseDTO<PageResult<BpmDefinitionVO>> query(BpmDefinitionQueryForm queryForm) {
        Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
        List<BpmDefinitionVO> list = bpmDefinitionDao.queryPage(page, queryForm);
        return ResponseDTO.ok(SmartPageUtil.convert2PageResult(page, list));
    }

    public ResponseDTO<BpmDefinitionDetailVO> getDetail(Long definitionId) {
        BpmDefinitionDetailVO detail = bpmDefinitionDao.queryDetail(definitionId);
        if (detail == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        return ResponseDTO.ok(detail);
    }

    public ResponseDTO<BpmDefinitionValidationReportVO> validateForPublish(Long modelId) {
        BpmModelEntity modelEntity = bpmModelDao.selectById(modelId);
        if (modelEntity == null || Boolean.TRUE.equals(modelEntity.getDeletedFlag())) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        return validateForPublish(modelEntity);
    }

    private ResponseDTO<BpmDefinitionValidationReportVO> validateForPublish(BpmModelEntity modelEntity) {
        BpmDefinitionValidationReportVO report = new BpmDefinitionValidationReportVO();
        report.setPass(Boolean.TRUE);
        report.setBlockingCount(0);
        report.setWarningCount(0);

        JSONObject simpleModelObject;
        try {
            simpleModelObject = JSON.parseObject(modelEntity.getSimpleModelJson());
        } catch (Exception ex) {
            addFinding(report, "BLOCKING", "SIMPLE_MODEL_JSON_INVALID", "设计器草稿 JSON 不合法", null, "simpleModelJson");
            finishReport(report);
            return ResponseDTO.ok(report);
        }
        if (simpleModelObject == null) {
            addFinding(report, "BLOCKING", "SIMPLE_MODEL_JSON_INVALID", "设计器草稿 JSON 不合法", null, "simpleModelJson");
            finishReport(report);
            return ResponseDTO.ok(report);
        }

        ResponseDTO<String> simpleModelValidationResponse = simpleModelValidator.validate(
                modelEntity.getSimpleModelJson(),
                modelEntity.getStartRuleJson()
        );
        if (!Boolean.TRUE.equals(simpleModelValidationResponse.getOk())) {
            String validationMessage = simpleModelValidationResponse.getMsg();
            addFinding(
                    report,
                    "BLOCKING",
                    "SIMPLE_MODEL_VALIDATION_FAILED",
                    validationMessage,
                    null,
                    validationMessage != null && validationMessage.contains("发起规则")
                            ? "startRuleJson"
                            : "simpleModelJson"
            );
        }

        BpmCategoryEntity categoryEntity = modelEntity.getCategoryId() == null
                ? null
                : bpmCategoryDao.selectById(modelEntity.getCategoryId());
        if (categoryEntity == null || Boolean.TRUE.equals(categoryEntity.getDeletedFlag())) {
            addFinding(
                    report,
                    "BLOCKING",
                    "CATEGORY_NOT_FOUND",
                    "流程分类不存在",
                    null,
                    "categoryId"
            );
        }

        BpmFormEntity formEntity = modelEntity.getFormId() == null
                ? null
                : bpmFormDao.selectById(modelEntity.getFormId());
        String formSchemaJson = null;
        if (formEntity == null || Boolean.TRUE.equals(formEntity.getDeletedFlag())) {
            addFinding(
                    report,
                    "BLOCKING",
                    "FORM_NOT_FOUND",
                    "流程表单不存在",
                    null,
                    "formId"
            );
        } else {
            formSchemaJson = formEntity.getSchemaJson();
            ResponseDTO<String> publishConsistencyResponse = bpmSimpleModelPublishValidator.validate(
                    modelEntity.getSimpleModelJson(),
                    formSchemaJson
            );
            if (!Boolean.TRUE.equals(publishConsistencyResponse.getOk())) {
                addFindingIfMessageAbsent(
                        report,
                        "BLOCKING",
                        "FORM_SCHEMA_CONSISTENCY_INVALID",
                        publishConsistencyResponse.getMsg(),
                        null,
                        "formSchemaJson"
                );
            }
        }

        List<BpmDefinitionValidationReportVO.CandidateCheck> candidateChecks = bpmCandidatePrecheckService.precheck(
                JSON.toJSONString(simpleModelObject),
                formSchemaJson
        );
        report.getCandidateChecks().addAll(candidateChecks);
        for (BpmDefinitionValidationReportVO.CandidateCheck candidateCheck : candidateChecks) {
            if (!"BLOCKING".equals(candidateCheck.getStatus())) {
                continue;
            }
            addFindingIfMessageAbsent(
                    report,
                    "BLOCKING",
                    candidateCheck.getCode(),
                    candidateCheck.getMessage(),
                    candidateCheck.getNodeKey(),
                    candidateCheck.getField()
            );
        }

        finishReport(report);
        return ResponseDTO.ok(report);
    }

    public ResponseDTO<BpmDefinitionDiffVO> previewPublishDiff(Long modelId) {
        BpmModelEntity modelEntity = bpmModelDao.selectById(modelId);
        if (modelEntity == null || Boolean.TRUE.equals(modelEntity.getDeletedFlag())) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        BpmDefinitionDiffVO diff = new BpmDefinitionDiffVO();
        diff.setModelId(modelId);
        diff.getChangedItems().add("发布后将生成新的不可变流程定义版本");

        if (modelEntity.getPublishedDefinitionId() != null) {
            BpmDefinitionEntity previousDefinition = bpmDefinitionDao.selectById(modelEntity.getPublishedDefinitionId());
            if (previousDefinition != null) {
                diff.setPreviousDefinitionId(previousDefinition.getDefinitionId());
                diff.setPreviousVersion(previousDefinition.getDefinitionVersion());
                if (!Objects.equals(previousDefinition.getSimpleModelSnapshotJson(), modelEntity.getSimpleModelJson())) {
                    diff.getChangedItems().add("流程节点设计已变化");
                }
                if (!Objects.equals(previousDefinition.getStartRuleSnapshotJson(), modelEntity.getStartRuleJson())) {
                    diff.getChangedItems().add("发起规则已变化");
                }
            }
        }

        return ResponseDTO.ok(diff);
    }

    public ResponseDTO<String> saveStartScope(BpmDefinitionStartScopeSaveForm form) {
        BpmDefinitionEntity entity = bpmDefinitionDao.selectById(form.getDefinitionId());
        if (entity == null) {
            return ResponseDTO.userErrorParam("流程定义不存在");
        }
        BpmDefinitionEntity updateEntity = new BpmDefinitionEntity();
        updateEntity.setDefinitionId(form.getDefinitionId());
        updateEntity.setStartScopeJson(form.getStartScopeJson());
        bpmDefinitionDao.updateById(updateEntity);
        return ResponseDTO.ok();
    }

    public ResponseDTO<String> suspendStart(Long definitionId) {
        return updateStartState(definitionId, BpmDefinitionStartStateEnum.SUSPENDED.getValue());
    }

    public ResponseDTO<String> enableStart(Long definitionId) {
        return updateStartState(definitionId, BpmDefinitionStartStateEnum.STARTABLE.getValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Long> publish(BpmDefinitionPublishForm publishForm) {
        BpmModelEntity modelEntity = bpmModelDao.selectById(publishForm.getModelId());
        if (modelEntity == null || Boolean.TRUE.equals(modelEntity.getDeletedFlag())) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        ResponseDTO<BpmDefinitionValidationReportVO> reportResponse = validateForPublish(modelEntity);
        if (!Boolean.TRUE.equals(reportResponse.getOk())) {
            return ResponseDTO.userErrorParam(reportResponse.getMsg());
        }
        if (!Boolean.TRUE.equals(reportResponse.getData().getPass())) {
            String errorMessage = reportResponse.getData().getFindings().stream()
                    .filter(item -> "BLOCKING".equals(item.getLevel()))
                    .map(BpmDefinitionValidationReportVO.Finding::getMessage)
                    .findFirst()
                    .orElse("流程发布校验未通过");
            return ResponseDTO.userErrorParam(errorMessage);
        }

        ResponseDTO<String> validateResponse = simpleModelValidator.validate(
                modelEntity.getSimpleModelJson(),
                modelEntity.getStartRuleJson()
        );
        if (!Boolean.TRUE.equals(validateResponse.getOk())) {
            return ResponseDTO.userErrorParam(validateResponse.getMsg());
        }

        BpmCategoryEntity categoryEntity = bpmCategoryDao.selectById(modelEntity.getCategoryId());
        if (categoryEntity == null || Boolean.TRUE.equals(categoryEntity.getDeletedFlag())) {
            return ResponseDTO.userErrorParam("流程分类不存在");
        }
        BpmFormEntity formEntity = bpmFormDao.selectById(modelEntity.getFormId());
        if (formEntity == null || Boolean.TRUE.equals(formEntity.getDeletedFlag())) {
            return ResponseDTO.userErrorParam("流程表单不存在");
        }
        ResponseDTO<String> publishConsistencyResponse = bpmSimpleModelPublishValidator.validate(
                modelEntity.getSimpleModelJson(),
                formEntity.getSchemaJson()
        );
        if (!Boolean.TRUE.equals(publishConsistencyResponse.getOk())) {
            return ResponseDTO.userErrorParam(publishConsistencyResponse.getMsg());
        }

        CompiledDefinitionArtifact artifact = simpleModelBpmnCompiler.compile(
                modelEntity.getModelKey(),
                modelEntity.getModelName(),
                modelEntity.getSimpleModelJson(),
                modelEntity.getStartRuleJson(),
                modelEntity.getVariableMappingJson()
        );
        BpmModelEntity claimModelEntity = new BpmModelEntity();
        claimModelEntity.setHasUnpublishedChanges(Boolean.FALSE);
        int claimedCount = bpmModelDao.update(
                claimModelEntity,
                buildPublishClaimWrapper(modelEntity)
        );
        if (claimedCount == 0) {
            return ResponseDTO.userErrorParam("模型已发生变更，请刷新后重新发布");
        }
        String engineProcessDefinitionId = flowableProcessDefinitionGateway.deploy(
                modelEntity.getModelKey(),
                modelEntity.getModelName(),
                artifact.compiledBpmnXml()
        );

        Integer currentMaxVersion = bpmDefinitionDao.selectMaxVersionByDefinitionKey(modelEntity.getModelKey());
        int newVersion = currentMaxVersion == null ? 1 : currentMaxVersion + 1;

        Long currentEmployeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        BpmEmployeeSnapshot employeeSnapshot = bpmOrgIdentityGateway.requireEmployee(currentEmployeeId);

        BpmDefinitionEntity definitionEntity = buildDefinitionEntity(
                modelEntity,
                categoryEntity,
                formEntity,
                artifact,
                engineProcessDefinitionId,
                newVersion,
                currentEmployeeId,
                employeeSnapshot.actualName()
        );
        bpmDefinitionDao.insert(definitionEntity);

        historicalizeOtherCurrentDefinitions(modelEntity.getModelKey(), definitionEntity.getDefinitionId());

        for (CompiledNodeSnapshot nodeSnapshot : artifact.nodeSnapshots()) {
            BpmDefinitionNodeEntity nodeEntity = new BpmDefinitionNodeEntity();
            nodeEntity.setDefinitionId(definitionEntity.getDefinitionId());
            nodeEntity.setNodeKey(nodeSnapshot.nodeKey());
            nodeEntity.setNodeType(nodeSnapshot.nodeType());
            nodeEntity.setNodeNameSnapshot(nodeSnapshot.nodeNameSnapshot());
            nodeEntity.setSortOrder(nodeSnapshot.sortOrder());
            nodeEntity.setAuthoredRuleSnapshotJson(nodeSnapshot.authoredRuleSnapshotJson());
            nodeEntity.setCompiledNodeSnapshotJson(nodeSnapshot.compiledNodeSnapshotJson());
            bpmDefinitionNodeDao.insert(nodeEntity);
        }

        BpmModelEntity updateModelEntity = new BpmModelEntity();
        updateModelEntity.setModelId(modelEntity.getModelId());
        updateModelEntity.setPublishedDefinitionId(definitionEntity.getDefinitionId());
        bpmModelDao.updateById(updateModelEntity);

        return ResponseDTO.ok(definitionEntity.getDefinitionId());
    }

    private UpdateWrapper<BpmModelEntity> buildPublishClaimWrapper(BpmModelEntity modelEntity) {
        UpdateWrapper<BpmModelEntity> wrapper = Wrappers.<BpmModelEntity>update()
                .eq("model_id", modelEntity.getModelId())
                .eq("has_unpublished_changes", Boolean.TRUE)
                .eq("update_time", modelEntity.getUpdateTime());
        addNullSafeSnapshotCondition(wrapper, "model_key", modelEntity.getModelKey());
        addNullSafeSnapshotCondition(wrapper, "model_name", modelEntity.getModelName());
        addNullSafeSnapshotCondition(wrapper, "category_id", modelEntity.getCategoryId());
        addNullSafeSnapshotCondition(wrapper, "form_type", modelEntity.getFormType());
        addNullSafeSnapshotCondition(wrapper, "form_id", modelEntity.getFormId());
        addNullSafeSnapshotCondition(wrapper, "simple_model_json", modelEntity.getSimpleModelJson());
        addNullSafeSnapshotCondition(wrapper, "start_rule_json", modelEntity.getStartRuleJson());
        addNullSafeSnapshotCondition(wrapper, "manager_scope_json", modelEntity.getManagerScopeJson());
        addNullSafeSnapshotCondition(wrapper, "title_rule_json", modelEntity.getTitleRuleJson());
        addNullSafeSnapshotCondition(wrapper, "summary_rule_json", modelEntity.getSummaryRuleJson());
        addNullSafeSnapshotCondition(wrapper, "variable_mapping_json", modelEntity.getVariableMappingJson());
        addNullSafeSnapshotCondition(wrapper, "instance_no_rule_id", modelEntity.getInstanceNoRuleId());
        return wrapper;
    }

    private void addNullSafeSnapshotCondition(
            UpdateWrapper<BpmModelEntity> wrapper,
            String column,
            Object value
    ) {
        if (value == null) {
            wrapper.isNull(column);
            return;
        }
        wrapper.eq(column, value);
    }

    private void addFinding(
            BpmDefinitionValidationReportVO report,
            String level,
            String code,
            String message,
            String nodeKey,
            String field
    ) {
        BpmDefinitionValidationReportVO.Finding finding = new BpmDefinitionValidationReportVO.Finding();
        finding.setLevel(level);
        finding.setCode(code);
        finding.setMessage(message);
        finding.setNodeKey(nodeKey);
        finding.setField(field);
        report.getFindings().add(finding);
    }

    private void addFindingIfMessageAbsent(
            BpmDefinitionValidationReportVO report,
            String level,
            String code,
            String message,
            String nodeKey,
            String field
    ) {
        boolean messageExists = report.getFindings().stream()
                .anyMatch(item -> Objects.equals(item.getMessage(), message));
        if (!messageExists) {
            addFinding(report, level, code, message, nodeKey, field);
        }
    }

    private void finishReport(BpmDefinitionValidationReportVO report) {
        int blockingCount = (int) report.getFindings().stream()
                .filter(item -> "BLOCKING".equals(item.getLevel()))
                .count();
        report.setBlockingCount(blockingCount);
        report.setWarningCount(report.getFindings().size() - blockingCount);
        report.setPass(blockingCount == 0);
    }

    private ResponseDTO<String> updateStartState(Long definitionId, Integer startState) {
        BpmDefinitionEntity entity = bpmDefinitionDao.selectById(definitionId);
        if (entity == null) {
            return ResponseDTO.userErrorParam("流程定义不存在");
        }
        BpmDefinitionEntity updateEntity = new BpmDefinitionEntity();
        updateEntity.setDefinitionId(definitionId);
        updateEntity.setStartState(startState);
        bpmDefinitionDao.updateById(updateEntity);
        return ResponseDTO.ok();
    }

    private void historicalizeOtherCurrentDefinitions(String definitionKey, Long currentDefinitionId) {
        BpmDefinitionEntity historicalDefinition = new BpmDefinitionEntity();
        historicalDefinition.setLifecycleState(BpmDefinitionLifecycleStateEnum.HISTORICAL.getValue());
        bpmDefinitionDao.update(
                historicalDefinition,
                Wrappers.<BpmDefinitionEntity>update()
                        .eq("definition_key", definitionKey)
                        .eq("lifecycle_state", BpmDefinitionLifecycleStateEnum.CURRENT.getValue())
                        .ne("definition_id", currentDefinitionId)
        );
    }

    private BpmDefinitionEntity buildDefinitionEntity(
            BpmModelEntity modelEntity,
            BpmCategoryEntity categoryEntity,
            BpmFormEntity formEntity,
            CompiledDefinitionArtifact artifact,
            String engineProcessDefinitionId,
            Integer definitionVersion,
            Long publishedByEmployeeId,
            String publishedByName
    ) {
        BpmDefinitionEntity entity = new BpmDefinitionEntity();
        entity.setModelId(modelEntity.getModelId());
        entity.setDefinitionKey(modelEntity.getModelKey());
        entity.setDefinitionName(modelEntity.getModelName());
        entity.setDefinitionVersion(definitionVersion);
        entity.setCategoryIdSnapshot(categoryEntity.getCategoryId());
        entity.setCategoryNameSnapshot(categoryEntity.getCategoryName());
        entity.setFormTypeSnapshot(modelEntity.getFormType());
        entity.setFormIdSnapshot(formEntity.getFormId());
        entity.setFormNameSnapshot(formEntity.getFormName());
        entity.setFormSchemaSnapshotJson(formEntity.getSchemaJson());
        entity.setSimpleModelSnapshotJson(modelEntity.getSimpleModelJson());
        entity.setCompiledBpmnXml(artifact.compiledBpmnXml());
        entity.setStartRuleSnapshotJson(modelEntity.getStartRuleJson());
        entity.setManagerScopeSnapshotJson(modelEntity.getManagerScopeJson());
        entity.setTitleRuleSnapshotJson(modelEntity.getTitleRuleJson());
        entity.setSummaryRuleSnapshotJson(modelEntity.getSummaryRuleJson());
        entity.setVariableMappingSnapshotJson(modelEntity.getVariableMappingJson());
        entity.setInstanceNoRuleIdSnapshot(modelEntity.getInstanceNoRuleId());
        entity.setLifecycleState(BpmDefinitionLifecycleStateEnum.CURRENT.getValue());
        entity.setStartState(BpmDefinitionStartStateEnum.STARTABLE.getValue());
        entity.setEngineProcessDefinitionId(engineProcessDefinitionId);
        entity.setPublishedByEmployeeId(publishedByEmployeeId);
        entity.setPublishedByNameSnapshot(publishedByName);
        entity.setPublishedAt(LocalDateTime.now());
        return entity;
    }
}
