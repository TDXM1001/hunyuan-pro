package com.hunyuan.sa.bpm.module.definition.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import com.hunyuan.sa.bpm.engine.compiler.CompiledDefinitionArtifact;
import com.hunyuan.sa.bpm.engine.compiler.CompiledNodeSnapshot;
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
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionDiffVO;
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionDetailVO;
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionValidationReportVO;
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionVO;
import com.hunyuan.sa.bpm.module.form.dao.BpmFormDao;
import com.hunyuan.sa.bpm.module.form.domain.entity.BpmFormEntity;
import com.hunyuan.sa.bpm.module.model.dao.BpmModelDao;
import com.hunyuan.sa.bpm.module.model.domain.entity.BpmModelEntity;
import org.apache.commons.lang3.StringUtils;
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

        JSONArray nodes = simpleModelObject == null ? null : simpleModelObject.getJSONArray("nodes");
        if (nodes != null) {
            for (int i = 0; i < nodes.size(); i++) {
                JSONObject nodeObject = nodes.getJSONObject(i);
                if (nodeObject == null || !"userTask".equals(nodeObject.getString("type"))) {
                    continue;
                }
                String resolverType = firstNonBlank(
                        nodeObject.getString("candidateResolverType"),
                        nodeObject.getString("resolverType")
                );
                if (StringUtils.isBlank(resolverType)) {
                    addFinding(
                            report,
                            "BLOCKING",
                            "USER_TASK_CANDIDATE_EMPTY",
                            "审批节点缺少处理人规则",
                            nodeObject.getString("nodeKey"),
                            "candidateResolverType"
                    );
                }
            }
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

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Long> publish(BpmDefinitionPublishForm publishForm) {
        BpmModelEntity modelEntity = bpmModelDao.selectById(publishForm.getModelId());
        if (modelEntity == null || Boolean.TRUE.equals(modelEntity.getDeletedFlag())) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        ResponseDTO<BpmDefinitionValidationReportVO> reportResponse = validateForPublish(publishForm.getModelId());
        if (!Boolean.TRUE.equals(reportResponse.getOk())) {
            return ResponseDTO.userErrorParam(reportResponse.getMsg());
        }
        if (!Boolean.TRUE.equals(reportResponse.getData().getPass())) {
            return ResponseDTO.userErrorParam("流程发布校验未通过");
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

        CompiledDefinitionArtifact artifact = simpleModelBpmnCompiler.compile(
                modelEntity.getModelKey(),
                modelEntity.getModelName(),
                modelEntity.getSimpleModelJson(),
                modelEntity.getStartRuleJson(),
                modelEntity.getVariableMappingJson()
        );
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

        BpmDefinitionEntity currentDefinition = bpmDefinitionDao.selectCurrentByDefinitionKey(modelEntity.getModelKey());
        if (currentDefinition != null && currentDefinition.getDefinitionId() != null
                && !currentDefinition.getDefinitionId().equals(definitionEntity.getDefinitionId())) {
            BpmDefinitionEntity historicalDefinition = new BpmDefinitionEntity();
            historicalDefinition.setDefinitionId(currentDefinition.getDefinitionId());
            historicalDefinition.setLifecycleState(BpmDefinitionLifecycleStateEnum.HISTORICAL.getValue());
            bpmDefinitionDao.updateById(historicalDefinition);
        }

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
        updateModelEntity.setHasUnpublishedChanges(Boolean.FALSE);
        bpmModelDao.updateById(updateModelEntity);

        return ResponseDTO.ok(definitionEntity.getDefinitionId());
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

    private void finishReport(BpmDefinitionValidationReportVO report) {
        int blockingCount = (int) report.getFindings().stream()
                .filter(item -> "BLOCKING".equals(item.getLevel()))
                .count();
        report.setBlockingCount(blockingCount);
        report.setWarningCount(report.getFindings().size() - blockingCount);
        report.setPass(blockingCount == 0);
    }

    private String firstNonBlank(String firstValue, String secondValue) {
        if (StringUtils.isNotBlank(firstValue)) {
            return firstValue;
        }
        return secondValue;
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
