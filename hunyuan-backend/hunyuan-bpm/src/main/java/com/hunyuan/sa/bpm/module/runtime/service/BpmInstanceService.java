package com.hunyuan.sa.bpm.module.runtime.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartEnumUtil;
import com.hunyuan.sa.base.common.util.SmartPageUtil;
import com.hunyuan.sa.base.module.support.serialnumber.constant.SerialNumberIdEnum;
import com.hunyuan.sa.base.module.support.serialnumber.service.SerialNumberService;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessStartCommand;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.WorkingDataMutationCommand;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.WorkingDataMutationResult;
import com.hunyuan.sa.bpm.module.approvaldata.service.BpmApprovalDataMutationService;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.ApprovalRuntimeBinding;
import com.hunyuan.sa.bpm.module.approvaldata.service.BpmApprovalRuntimeDataService;
import com.hunyuan.sa.bpm.module.candidate.domain.model.StartDecision;
import com.hunyuan.sa.bpm.module.candidate.domain.model.StartVisibilityEvaluationContext;
import com.hunyuan.sa.bpm.module.candidate.service.StartVisibilityPolicyEvaluator;
import com.hunyuan.sa.bpm.common.enumeration.BpmApprovalGroupCloseReasonEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmDefinitionLifecycleStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmDefinitionStartStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceResultStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceRunStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskResultEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmFormDataChangeSourceEnum;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionDao;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmFormDataChangeDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmFormDataChangeEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskActionLogEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmAdminInstanceCancelForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceCancelForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceQueryForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceResubmitForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceStartForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceDetailVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmApprovalGroupSummaryVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmRuntimeStartDraftVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmStartableDefinitionVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskVO;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 流程实例服务。
 */
@Service
public class BpmInstanceService {

    private static final long CURRENT_TENANT_ID = 1L;

    @Resource
    private BpmDefinitionDao bpmDefinitionDao;

    @Resource
    private GraphDefinitionVersionDao graphDefinitionVersionDao;

    @Resource
    private BpmInstanceDao bpmInstanceDao;

    @Resource
    private BpmTaskDao bpmTaskDao;

    @Resource
    private BpmDefinitionNodeDao bpmDefinitionNodeDao;

    @Resource
    private BpmTaskActionLogDao bpmTaskActionLogDao;

    @Resource
    private FlowableProcessInstanceGateway flowableProcessInstanceGateway;

    @Resource
    private BpmCurrentActorProvider bpmCurrentActorProvider;

    @Resource
    private BpmOrgIdentityGateway bpmOrgIdentityGateway;

    @Resource
    private StartVisibilityPolicyEvaluator startVisibilityPolicyEvaluator;

    @Resource
    private SerialNumberService serialNumberService;

    @Resource
    private BpmTaskAssignmentResolver bpmTaskAssignmentResolver;

    @Resource
    private BpmTaskProjectionService bpmTaskProjectionService;

    @Resource
    private BpmApprovalStageInstanceProjectionService bpmApprovalStageInstanceProjectionService;

    @Resource
    private BpmApprovalGroupService bpmApprovalGroupService;

    @Resource
    private BpmRuntimeFormDataValidator bpmRuntimeFormDataValidator;

    @Resource
    private BpmFormDataChangeDao bpmFormDataChangeDao;

    @Resource
    private BpmTimeEventService bpmTimeEventService;

    @Resource
    private BpmExternalWaitService bpmExternalWaitService;

    @Resource
    private BpmApprovalRuntimeDataService bpmApprovalRuntimeDataService;

    @Resource
    private BpmApprovalDataMutationService bpmApprovalDataMutationService;

    public ResponseDTO<PageResult<BpmInstanceVO>> queryAdminPage(BpmInstanceQueryForm queryForm) {
        Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
        List<BpmInstanceVO> list = bpmInstanceDao.queryPage(page, queryForm);
        return ResponseDTO.ok(SmartPageUtil.convert2PageResult(page, list));
    }

    public ResponseDTO<PageResult<BpmInstanceVO>> queryMyInstancePage(BpmInstanceQueryForm queryForm) {
        queryForm.setStartEmployeeId(bpmCurrentActorProvider.requireCurrentEmployeeId());
        return queryAdminPage(queryForm);
    }

    public ResponseDTO<List<BpmStartableDefinitionVO>> queryStartableDefinitions() {
        Long employeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        BpmEmployeeSnapshot employeeSnapshot = bpmOrgIdentityGateway.requireEmployee(employeeId);
        List<BpmStartableDefinitionVO> list = new ArrayList<>();
        bpmDefinitionDao.queryStartableList(employeeId).stream()
                .filter(definition -> canEmployeeStart(definition.getStartScopeJson(), employeeId))
                .forEach(definition -> {
                    definition.setDefinitionSource("LEGACY");
                    list.add(definition);
                });
        graphDefinitionVersionDao.selectActiveStartableList().stream()
                .filter(graphVersion -> canEmployeeStartGraph(graphVersion, employeeSnapshot))
                .map(this::toGraphStartableDefinition)
                .forEach(list::add);
        list.sort(Comparator
                .comparing(BpmStartableDefinitionVO::getDefinitionName, Comparator.nullsLast(String::compareTo))
                .thenComparing(BpmStartableDefinitionVO::getDefinitionSource, Comparator.nullsLast(String::compareTo))
                .thenComparing(BpmStartableDefinitionVO::getDefinitionVersion, Comparator.nullsLast(Comparator.reverseOrder())));
        return ResponseDTO.ok(list);
    }

    public ResponseDTO<BpmRuntimeStartDraftVO> getStartDraft(Long definitionId) {
        BpmDefinitionEntity definitionEntity = bpmDefinitionDao.selectById(definitionId);
        ResponseDTO<BpmRuntimeStartDraftVO> validationResponse = validateCurrentStartableDefinition(definitionEntity);
        if (validationResponse != null) {
            return validationResponse;
        }
        return ResponseDTO.ok(buildStartDraft(
                definitionEntity,
                definitionEntity.getDefinitionName(),
                null,
                "{}",
                null,
                null
        ));
    }

    public ResponseDTO<BpmRuntimeStartDraftVO> getGraphStartDraft(Long graphDefinitionVersionId) {
        GraphDefinitionVersionEntity graphVersion = graphDefinitionVersionDao.selectById(graphDefinitionVersionId);
        if (graphVersion == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        if (!"ACTIVE".equals(graphVersion.getLifecycleState())) {
            return ResponseDTO.userErrorParam("Graph 定义版本已下线，无法发起");
        }
        if (graphVersion.getCategoryIdSnapshot() == null || StringUtils.isBlank(graphVersion.getCategoryNameSnapshot())) {
            return ResponseDTO.userErrorParam("Graph 定义版本缺少分类快照，无法发起");
        }
        BpmEmployeeSnapshot employeeSnapshot = bpmOrgIdentityGateway.requireEmployee(
                bpmCurrentActorProvider.requireCurrentEmployeeId()
        );
        GraphStartVisibilityPolicy visibilityPolicy = readGraphStartVisibilityPolicy(graphVersion);
        StartDecision decision = startVisibilityPolicyEvaluator.evaluateStart(
                visibilityPolicy.schemaVersion(),
                visibilityPolicy.canonicalPayload(),
                new StartVisibilityEvaluationContext(CURRENT_TENANT_ID, employeeSnapshot, java.util.Set.of(), false)
        );
        if (!decision.allowed()) {
            return ResponseDTO.userErrorParam("当前 Graph 定义版本不在可发起范围内");
        }
        return ResponseDTO.ok(buildGraphStartDraft(graphVersion));
    }

    public ResponseDTO<BpmInstanceDetailVO> getDetail(Long instanceId) {
        BpmInstanceEntity instance = bpmInstanceDao.selectById(instanceId);
        if (instance == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        BpmInstanceDetailVO detail = new BpmInstanceDetailVO();
        detail.setInstanceId(instance.getInstanceId());
        detail.setInstanceNo(instance.getInstanceNo());
        detail.setTitle(instance.getTitle());
        detail.setSummary(instance.getSummary());
        detail.setRunState(instance.getRunState());
        detail.setResultState(instance.getResultState());
        detail.setStartEmployeeNameSnapshot(instance.getStartEmployeeNameSnapshot());
        detail.setStartDepartmentNameSnapshot(instance.getStartDepartmentNameSnapshot());
        detail.setCurrentFormDataSnapshotJson(instance.getCurrentFormDataSnapshotJson());
        detail.setCurrentNodeSummaryJson(instance.getCurrentNodeSummaryJson());
        detail.setStartedAt(instance.getStartedAt());
        detail.setFinishedAt(instance.getFinishedAt());
        List<BpmTaskVO> currentTasks = bpmTaskDao.queryCurrentTasksByInstanceId(instanceId);
        attachApprovalGroupSummaries(currentTasks);
        detail.setCurrentTasks(currentTasks);
        detail.setActionLogs(bpmTaskActionLogDao.queryByInstanceId(instanceId));
        detail.setApprovalGroups(bpmApprovalGroupService.listDetailsByInstanceId(instanceId));
        return ResponseDTO.ok(detail);
    }

    private void attachApprovalGroupSummaries(List<BpmTaskVO> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        List<Long> approvalGroupIds = tasks.stream()
                .map(BpmTaskVO::getApprovalGroupId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (approvalGroupIds.isEmpty()) {
            return;
        }
        Map<Long, BpmApprovalGroupSummaryVO> summaryMap =
                bpmApprovalGroupService.mapSummariesById(approvalGroupIds);
        tasks.stream()
                .filter(task -> task.getApprovalGroupId() != null)
                .forEach(task -> task.setApprovalGroup(summaryMap.get(task.getApprovalGroupId())));
    }

    public ResponseDTO<BpmRuntimeStartDraftVO> getResubmitDraft(Long instanceId) {
        BpmInstanceEntity instanceEntity = bpmInstanceDao.selectById(instanceId);
        if (instanceEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        Long employeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        if (!employeeId.equals(instanceEntity.getStartEmployeeId())) {
            return ResponseDTO.userErrorParam("只能重新提交自己发起的流程");
        }
        if (!BpmInstanceRunStateEnum.WAIT_RESUBMIT.equalsValue(instanceEntity.getRunState())) {
            return ResponseDTO.userErrorParam("当前流程实例状态不支持重新提交");
        }

        BpmDefinitionEntity definitionEntity = bpmDefinitionDao.selectById(instanceEntity.getDefinitionId());
        ResponseDTO<BpmRuntimeStartDraftVO> validationResponse = validateCurrentStartableDefinition(definitionEntity);
        if (validationResponse != null) {
            return validationResponse;
        }

        return ResponseDTO.ok(buildStartDraft(
                definitionEntity,
                instanceEntity.getTitle(),
                instanceEntity.getSummary(),
                StringUtils.defaultIfBlank(instanceEntity.getCurrentFormDataSnapshotJson(), "{}"),
                instanceEntity.getInstanceId(),
                instanceEntity.getFormDataVersion() == null ? 1L : instanceEntity.getFormDataVersion()
        ));
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> cancelMyInstance(BpmInstanceCancelForm cancelForm) {
        BpmInstanceEntity instanceEntity = bpmInstanceDao.selectById(cancelForm.getInstanceId());
        if (instanceEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        Long employeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        if (!employeeId.equals(instanceEntity.getStartEmployeeId())) {
            return ResponseDTO.userErrorParam("只能取消自己发起的流程");
        }
        if (!BpmInstanceRunStateEnum.RUNNING.equalsValue(instanceEntity.getRunState())
                && !BpmInstanceRunStateEnum.WAIT_RESUBMIT.equalsValue(instanceEntity.getRunState())) {
            return ResponseDTO.userErrorParam("当前流程实例状态不支持取消");
        }

        BpmEmployeeSnapshot employeeSnapshot = bpmOrgIdentityGateway.requireEmployee(employeeId);
        if (BpmInstanceRunStateEnum.RUNNING.equalsValue(instanceEntity.getRunState())) {
            flowableProcessInstanceGateway.cancel(instanceEntity.getEngineProcessInstanceId(), cancelForm.getCancelReason());
        }

        LocalDateTime now = LocalDateTime.now();
        bpmApprovalGroupService.closePendingGroupsForInstance(
                instanceEntity.getInstanceId(),
                BpmApprovalGroupCloseReasonEnum.INSTANCE_CANCELLED,
                BpmTaskResultEnum.INSTANCE_CANCELLED,
                now
        );
        closePendingTasks(instanceEntity.getInstanceId(), now);
        bpmTimeEventService.cancelPendingForInstance(instanceEntity.getInstanceId());
        bpmExternalWaitService.cancelPendingForInstance(instanceEntity.getInstanceId());

        BpmInstanceEntity updateInstanceEntity = new BpmInstanceEntity();
        updateInstanceEntity.setInstanceId(instanceEntity.getInstanceId());
        updateInstanceEntity.setRunState(BpmInstanceRunStateEnum.CANCELLED.getValue());
        updateInstanceEntity.setResultState(BpmInstanceResultStateEnum.CANCELLED_BY_START_USER.getValue());
        updateInstanceEntity.setActiveTaskCount(0);
        updateInstanceEntity.setCurrentNodeSummaryJson(null);
        updateInstanceEntity.setCancelByEmployeeId(employeeSnapshot.employeeId());
        updateInstanceEntity.setCancelByNameSnapshot(employeeSnapshot.actualName());
        updateInstanceEntity.setCancelReason(cancelForm.getCancelReason());
        updateInstanceEntity.setLastActionAt(now);
        updateInstanceEntity.setFinishedAt(now);
        updateInstanceEntity.setCancelledAt(now);
        bpmInstanceDao.updateById(updateInstanceEntity);

        bpmTaskActionLogDao.insert(buildInstanceActionLog(instanceEntity, employeeSnapshot, cancelForm.getCancelReason(), now));
        return ResponseDTO.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> adminCancel(BpmAdminInstanceCancelForm cancelForm) {
        BpmInstanceEntity instanceEntity = bpmInstanceDao.selectById(cancelForm.getInstanceId());
        if (instanceEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        if (!BpmInstanceRunStateEnum.RUNNING.equalsValue(instanceEntity.getRunState())
                && !BpmInstanceRunStateEnum.WAIT_RESUBMIT.equalsValue(instanceEntity.getRunState())) {
            return ResponseDTO.userErrorParam("当前流程实例状态不支持管理员取消");
        }

        Long employeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        BpmEmployeeSnapshot actorSnapshot = bpmOrgIdentityGateway.requireEmployee(employeeId);
        if (BpmInstanceRunStateEnum.RUNNING.equalsValue(instanceEntity.getRunState())) {
            flowableProcessInstanceGateway.cancel(instanceEntity.getEngineProcessInstanceId(), cancelForm.getCancelReason());
        }

        LocalDateTime now = LocalDateTime.now();
        bpmApprovalGroupService.closePendingGroupsForInstance(
                instanceEntity.getInstanceId(),
                BpmApprovalGroupCloseReasonEnum.INSTANCE_CANCELLED,
                BpmTaskResultEnum.INSTANCE_CANCELLED,
                now
        );
        closePendingTasks(instanceEntity.getInstanceId(), now);
        bpmTimeEventService.cancelPendingForInstance(instanceEntity.getInstanceId());
        bpmExternalWaitService.cancelPendingForInstance(instanceEntity.getInstanceId());

        BpmInstanceEntity updateInstanceEntity = new BpmInstanceEntity();
        updateInstanceEntity.setInstanceId(instanceEntity.getInstanceId());
        updateInstanceEntity.setRunState(BpmInstanceRunStateEnum.CANCELLED.getValue());
        updateInstanceEntity.setResultState(BpmInstanceResultStateEnum.CANCELLED_BY_ADMIN.getValue());
        updateInstanceEntity.setActiveTaskCount(0);
        updateInstanceEntity.setCurrentNodeSummaryJson(null);
        updateInstanceEntity.setCancelByEmployeeId(actorSnapshot.employeeId());
        updateInstanceEntity.setCancelByNameSnapshot(actorSnapshot.actualName());
        updateInstanceEntity.setCancelReason(cancelForm.getCancelReason());
        updateInstanceEntity.setLastActionAt(now);
        updateInstanceEntity.setFinishedAt(now);
        updateInstanceEntity.setCancelledAt(now);
        bpmInstanceDao.updateById(updateInstanceEntity);

        bpmTaskActionLogDao.insert(buildAdminInstanceCancelActionLog(
                instanceEntity,
                actorSnapshot,
                cancelForm.getCancelReason(),
                now
        ));
        return ResponseDTO.ok();
    }

    public ResponseDTO<String> resyncProjection(Long instanceId) {
        BpmInstanceEntity instanceEntity = bpmInstanceDao.selectById(instanceId);
        if (instanceEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        bpmTaskProjectionService.syncActiveTasksForInstance(instanceId);
        if (instanceEntity.getGraphDefinitionVersionId() != null
                && BpmInstanceRunStateEnum.RUNNING.equalsValue(instanceEntity.getRunState())) {
            bpmApprovalStageInstanceProjectionService.reconcileApprovedCompletion(
                    instanceId,
                    instanceEntity.getEngineProcessInstanceId()
            );
        }
        return ResponseDTO.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Long> resubmitMyInstance(BpmInstanceResubmitForm resubmitForm) {
        BpmInstanceEntity instanceEntity = bpmInstanceDao.selectByIdForUpdate(resubmitForm.getInstanceId());
        if (instanceEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        Long employeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        if (!employeeId.equals(instanceEntity.getStartEmployeeId())) {
            return ResponseDTO.userErrorParam("只能重新提交自己发起的流程");
        }
        if (!BpmInstanceRunStateEnum.WAIT_RESUBMIT.equalsValue(instanceEntity.getRunState())) {
            return ResponseDTO.userErrorParam("当前流程实例状态不支持重新提交");
        }
        long currentVersion = instanceEntity.getFormDataVersion() == null ? 1L : instanceEntity.getFormDataVersion();
        if (!Objects.equals(currentVersion, resubmitForm.getFormDataVersion())) {
            return ResponseDTO.userErrorParam("FORM_DATA_VERSION_CONFLICT：审批数据已变化，请刷新后重新确认");
        }

        BpmDefinitionEntity definitionEntity = bpmDefinitionDao.selectById(instanceEntity.getDefinitionId());
        ResponseDTO<Long> validationResponse = validateCurrentStartableDefinition(definitionEntity);
        if (validationResponse != null) {
            return validationResponse;
        }

        if (StringUtils.isNotBlank(definitionEntity.getFormSchemaSnapshotJson())) {
            ResponseDTO<String> formDataResponse = bpmRuntimeFormDataValidator.validateFullData(
                    definitionEntity.getFormSchemaSnapshotJson(),
                    resubmitForm.getFormDataJson()
            );
            if (!Boolean.TRUE.equals(formDataResponse.getOk())) {
                return ResponseDTO.userErrorParam(formDataResponse.getMsg());
            }
        }

        JSONObject beforeData = JSON.parseObject(
                StringUtils.defaultIfBlank(instanceEntity.getCurrentFormDataSnapshotJson(), "{}")
        );
        JSONObject afterData = JSON.parseObject(resubmitForm.getFormDataJson());
        JSONArray changedFields = new JSONArray();
        JSONObject beforeValues = new JSONObject(true);
        JSONObject afterValues = new JSONObject(true);
        java.util.LinkedHashSet<String> comparedFields = new java.util.LinkedHashSet<>();
        comparedFields.addAll(beforeData.keySet());
        comparedFields.addAll(afterData.keySet());
        for (String fieldKey : comparedFields) {
            if (Objects.equals(beforeData.get(fieldKey), afterData.get(fieldKey))) {
                continue;
            }
            changedFields.add(fieldKey);
            beforeValues.put(fieldKey, beforeData.get(fieldKey));
            afterValues.put(fieldKey, afterData.get(fieldKey));
        }
        long afterVersion = changedFields.isEmpty() ? currentVersion : currentVersion + 1;

        BpmEmployeeSnapshot employeeSnapshot = bpmOrgIdentityGateway.requireEmployee(employeeId);
        String nextFormDataJson = resubmitForm.getFormDataJson();
        Long nextProcessWorkingDataId = instanceEntity.getProcessWorkingDataId();
        if (instanceEntity.getApprovalSubjectSnapshotId() != null) {
            JSONObject patch = new JSONObject(true);
            for (Object rawField : changedFields) {
                String field = String.valueOf(rawField);
                patch.put(field, afterData.get(field));
            }
            WorkingDataMutationResult mutation;
            try {
                mutation = bpmApprovalDataMutationService.update(new WorkingDataMutationCommand(
                        instanceEntity.getApprovalSubjectSnapshotId(),
                        null,
                        currentVersion,
                        JSON.toJSONString(patch),
                        "发起人退回重提",
                        employeeSnapshot.employeeId(),
                        employeeSnapshot.actualName(),
                        "RESUBMIT",
                        resubmitForm.getSummary(),
                        "[]"
                ));
            } catch (IllegalArgumentException | IllegalStateException ex) {
                return ResponseDTO.userErrorParam(ex.getMessage());
            }
            nextFormDataJson = mutation.dataJson();
            nextProcessWorkingDataId = mutation.processWorkingDataId();
            afterVersion = mutation.dataVersion();
        }
        List<BpmDefinitionNodeEntity> definitionNodes = bpmDefinitionNodeDao.selectList(
                Wrappers.<BpmDefinitionNodeEntity>lambdaQuery()
                        .eq(BpmDefinitionNodeEntity::getDefinitionId, definitionEntity.getDefinitionId())
                        .orderByAsc(BpmDefinitionNodeEntity::getSortOrder, BpmDefinitionNodeEntity::getDefinitionNodeId)
        );

        Map<String, Object> runtimeAssignmentVariables;
        try {
            runtimeAssignmentVariables = bpmTaskAssignmentResolver.resolve(
                    definitionNodes,
                    new BpmTaskAssignmentContext(employeeSnapshot, nextFormDataJson)
            );
        } catch (IllegalArgumentException ex) {
            return ResponseDTO.userErrorParam(ex.getMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        BpmInstanceEntity updateInstanceEntity = new BpmInstanceEntity();
        updateInstanceEntity.setInstanceId(instanceEntity.getInstanceId());
        updateInstanceEntity.setEngineProcessDefinitionId(definitionEntity.getEngineProcessDefinitionId());
        updateInstanceEntity.setDefinitionKeySnapshot(definitionEntity.getDefinitionKey());
        updateInstanceEntity.setDefinitionVersionSnapshot(definitionEntity.getDefinitionVersion());
        updateInstanceEntity.setCategoryIdSnapshot(definitionEntity.getCategoryIdSnapshot());
        updateInstanceEntity.setCategoryNameSnapshot(definitionEntity.getCategoryNameSnapshot());
        updateInstanceEntity.setTitle(StringUtils.isBlank(resubmitForm.getTitle()) ? definitionEntity.getDefinitionName() : resubmitForm.getTitle());
        updateInstanceEntity.setSummary(resubmitForm.getSummary());
        updateInstanceEntity.setCurrentFormDataSnapshotJson(nextFormDataJson);
        updateInstanceEntity.setFormDataVersion(afterVersion);
        updateInstanceEntity.setProcessWorkingDataId(nextProcessWorkingDataId);
        updateInstanceEntity.setRunState(BpmInstanceRunStateEnum.RUNNING.getValue());
        updateInstanceEntity.setResultState(null);
        updateInstanceEntity.setActiveTaskCount(0);
        updateInstanceEntity.setCurrentNodeSummaryJson(null);
        updateInstanceEntity.setLastActionAt(now);
        bpmInstanceDao.updateById(updateInstanceEntity);

        if (instanceEntity.getApprovalSubjectSnapshotId() == null && !changedFields.isEmpty()) {
            BpmFormDataChangeEntity change = new BpmFormDataChangeEntity();
            change.setInstanceId(instanceEntity.getInstanceId());
            change.setChangeSource(BpmFormDataChangeSourceEnum.INSTANCE_RESUBMITTED.name());
            change.setActorEmployeeId(employeeSnapshot.employeeId());
            change.setActorNameSnapshot(employeeSnapshot.actualName());
            change.setBeforeVersion(currentVersion);
            change.setAfterVersion(afterVersion);
            change.setChangedFieldsJson(JSON.toJSONString(changedFields));
            change.setBeforeValuesJson(JSON.toJSONString(beforeValues));
            change.setAfterValuesJson(JSON.toJSONString(afterValues));
            bpmFormDataChangeDao.insert(change);
        }

        String engineProcessInstanceId = flowableProcessInstanceGateway.start(
                definitionEntity.getEngineProcessDefinitionId(),
                instanceEntity.getInstanceId(),
                employeeId,
                nextFormDataJson,
                runtimeAssignmentVariables
        );
        BpmInstanceEntity engineUpdate = new BpmInstanceEntity();
        engineUpdate.setInstanceId(instanceEntity.getInstanceId());
        engineUpdate.setEngineProcessInstanceId(engineProcessInstanceId);
        bpmInstanceDao.updateById(engineUpdate);

        bpmTaskActionLogDao.insert(buildResubmitActionLog(instanceEntity, employeeSnapshot, now));
        bpmTaskProjectionService.syncActiveTasksForInstance(instanceEntity.getInstanceId());
        return ResponseDTO.ok(instanceEntity.getInstanceId());
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Long> startInstance(BpmInstanceStartForm startForm) {
        if (startForm == null || !startForm.hasExactlyOneDefinitionSource()) {
            return ResponseDTO.userErrorParam("流程定义来源只能选择一种");
        }
        Long employeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        if (startForm.getGraphDefinitionVersionId() != null) {
            return startGraphInstance(startForm, employeeId);
        }
        BpmDefinitionEntity definitionEntity = bpmDefinitionDao.selectById(startForm.getDefinitionId());
        return startInstanceWithDefinition(startForm, definitionEntity, employeeId);
    }

    private ResponseDTO<Long> startGraphInstance(BpmInstanceStartForm startForm, Long employeeId) {
        if (StringUtils.isBlank(startForm.getFormDataJson())) {
            return ResponseDTO.userErrorParam("表单数据不能为空");
        }
        GraphDefinitionVersionEntity graphVersion = graphDefinitionVersionDao.selectById(startForm.getGraphDefinitionVersionId());
        if (graphVersion == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        if (!"ACTIVE".equals(graphVersion.getLifecycleState())) {
            return ResponseDTO.userErrorParam("Graph 定义版本已下线，无法发起");
        }
        if (StringUtils.isBlank(graphVersion.getEngineProcessDefinitionId())) {
            throw new IllegalStateException("Graph 定义版本缺少 Flowable 流程定义ID");
        }
        if (graphVersion.getCategoryIdSnapshot() == null || StringUtils.isBlank(graphVersion.getCategoryNameSnapshot())) {
            return ResponseDTO.userErrorParam("Graph 定义版本缺少分类快照，无法发起");
        }

        BpmEmployeeSnapshot employeeSnapshot = bpmOrgIdentityGateway.requireEmployee(employeeId);
        GraphStartVisibilityPolicy visibilityPolicy = readGraphStartVisibilityPolicy(graphVersion);
        StartDecision startDecision = startVisibilityPolicyEvaluator.evaluateStart(
                visibilityPolicy.schemaVersion(),
                visibilityPolicy.canonicalPayload(),
                new StartVisibilityEvaluationContext(CURRENT_TENANT_ID, employeeSnapshot, java.util.Set.of(), false)
        );
        if (!startDecision.allowed()) {
            return ResponseDTO.userErrorParam("当前 Graph 定义版本不在可发起范围内");
        }

        ApprovalRuntimeBinding approvalBinding;
        try {
            approvalBinding = bpmApprovalRuntimeDataService.prepareForStart(
                    startForm.getApprovalSubjectSnapshotId(), graphVersion
            );
        } catch (IllegalArgumentException ex) {
            return ResponseDTO.userErrorParam(ex.getMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        BpmInstanceEntity entity = new BpmInstanceEntity();
        entity.setInstanceNo(serialNumberService.generate(SerialNumberIdEnum.ORDER));
        entity.setGraphDefinitionVersionId(graphVersion.getGraphDefinitionVersionId());
        entity.setDefinitionSource("GRAPH");
        entity.setEngineProcessDefinitionId(graphVersion.getEngineProcessDefinitionId());
        entity.setDefinitionKeySnapshot(graphVersion.getProcessKey());
        entity.setDefinitionVersionSnapshot(graphVersion.getDefinitionVersion());
        entity.setCategoryIdSnapshot(graphVersion.getCategoryIdSnapshot());
        entity.setCategoryNameSnapshot(graphVersion.getCategoryNameSnapshot());
        entity.setTitle(approvalBinding.title());
        entity.setSummary(approvalBinding.summary());
        entity.setStartEmployeeId(employeeSnapshot.employeeId());
        entity.setStartEmployeeNameSnapshot(employeeSnapshot.actualName());
        entity.setStartDepartmentIdSnapshot(employeeSnapshot.departmentId());
        entity.setStartDepartmentNameSnapshot(employeeSnapshot.departmentName());
        entity.setBusinessType(approvalBinding.businessType());
        entity.setBusinessKey(approvalBinding.businessKey());
        entity.setApprovalSubjectSnapshotId(approvalBinding.approvalSubjectSnapshotId());
        entity.setRoutingFactSnapshotId(approvalBinding.routingFactSnapshotId());
        entity.setProcessWorkingDataId(approvalBinding.processWorkingDataId());
        entity.setInitialFormDataSnapshotJson(approvalBinding.workingDataJson());
        entity.setCurrentFormDataSnapshotJson(approvalBinding.workingDataJson());
        entity.setFormDataVersion(approvalBinding.workingDataVersion());
        entity.setStartVisibilityPolicyVersionId(visibilityPolicy.policyVersionId());
        entity.setStartVisibilityPolicyDigest(visibilityPolicy.digest());
        entity.setStartVisibilityDecisionJson(buildStartVisibilityDecisionJson(startDecision, now));
        entity.setRunState(BpmInstanceRunStateEnum.RUNNING.getValue());
        entity.setActiveTaskCount(0);
        entity.setStartedAt(now);
        entity.setLastActionAt(now);
        bpmInstanceDao.insert(entity);

        String engineProcessInstanceId = flowableProcessInstanceGateway.start(
                graphVersion.getEngineProcessDefinitionId(),
                entity.getInstanceId(),
                employeeSnapshot.employeeId(),
                approvalBinding.workingDataJson(),
                Map.of()
        );
        BpmInstanceEntity engineUpdate = new BpmInstanceEntity();
        engineUpdate.setInstanceId(entity.getInstanceId());
        engineUpdate.setEngineProcessInstanceId(engineProcessInstanceId);
        bpmInstanceDao.updateById(engineUpdate);
        bpmTaskProjectionService.syncActiveTasksForInstance(entity.getInstanceId());
        return ResponseDTO.ok(entity.getInstanceId());
    }

    private GraphStartVisibilityPolicy readGraphStartVisibilityPolicy(GraphDefinitionVersionEntity graphVersion) {
        JSONObject dependencies = JSON.parseObject(graphVersion.getDependencyVersionsJson());
        JSONObject policy = dependencies == null ? null : dependencies.getJSONObject("startVisibilityPolicy");
        if (policy == null) {
            throw new IllegalStateException("Graph 定义版本缺少冻结发起可见范围策略");
        }
        Long policyVersionId = policy.getLong("policyVersionId");
        Integer schemaVersion = policy.getInteger("schemaVersion");
        String canonicalPayload = policy.getString("canonicalPayload");
        String digest = policy.getString("digest");
        if (policyVersionId == null || policyVersionId <= 0
                || schemaVersion == null || schemaVersion <= 0
                || StringUtils.isBlank(canonicalPayload) || StringUtils.isBlank(digest)) {
            throw new IllegalStateException("Graph 定义版本的冻结发起可见范围策略内容不完整");
        }
        return new GraphStartVisibilityPolicy(policyVersionId, schemaVersion, canonicalPayload, digest);
    }

    private boolean canEmployeeStartGraph(
            GraphDefinitionVersionEntity graphVersion,
            BpmEmployeeSnapshot employeeSnapshot
    ) {
        if (graphVersion.getCategoryIdSnapshot() == null
                || StringUtils.isBlank(graphVersion.getCategoryNameSnapshot())) {
            return false;
        }
        try {
            GraphStartVisibilityPolicy visibilityPolicy = readGraphStartVisibilityPolicy(graphVersion);
            return startVisibilityPolicyEvaluator.evaluateStart(
                    visibilityPolicy.schemaVersion(),
                    visibilityPolicy.canonicalPayload(),
                    new StartVisibilityEvaluationContext(CURRENT_TENANT_ID, employeeSnapshot, java.util.Set.of(), false)
            ).allowed();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private BpmStartableDefinitionVO toGraphStartableDefinition(GraphDefinitionVersionEntity graphVersion) {
        BpmStartableDefinitionVO vo = new BpmStartableDefinitionVO();
        vo.setGraphDefinitionVersionId(graphVersion.getGraphDefinitionVersionId());
        vo.setDefinitionSource("GRAPH");
        vo.setDefinitionKey(graphVersion.getProcessKey());
        vo.setDefinitionName(graphVersion.getProcessNameSnapshot());
        vo.setDefinitionVersion(graphVersion.getDefinitionVersion());
        vo.setCategoryNameSnapshot(graphVersion.getCategoryNameSnapshot());
        return vo;
    }

    private String buildStartVisibilityDecisionJson(StartDecision decision, LocalDateTime decidedAt) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("tenantId", CURRENT_TENANT_ID);
        snapshot.put("allowed", decision.allowed());
        snapshot.put("matchedRule", decision.matchedRule());
        snapshot.put("reason", decision.reason());
        snapshot.put("decidedAt", decidedAt.toString());
        return JSON.toJSONString(snapshot);
    }

    private record GraphStartVisibilityPolicy(
            Long policyVersionId,
            Integer schemaVersion,
            String canonicalPayload,
            String digest
    ) {
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Long> startBusinessInstance(BpmBusinessStartCommand command) {
        if (command == null) {
            return ResponseDTO.userErrorParam("业务流程发起命令不能为空");
        }
        BpmDefinitionEntity definitionEntity = bpmDefinitionDao.selectCurrentByDefinitionKey(command.getDefinitionKey());
        BpmInstanceStartForm startForm = new BpmInstanceStartForm();
        startForm.setDefinitionId(definitionEntity == null ? null : definitionEntity.getDefinitionId());
        startForm.setTitle(command.getTitle());
        startForm.setSummary(command.getSummary());
        startForm.setFormDataJson(StringUtils.defaultIfBlank(command.getFormDataJson(), "{}"));
        startForm.setBusinessType(command.getBusinessType());
        startForm.setBusinessId(command.getBusinessId());
        startForm.setBusinessKey(command.getBusinessKey());
        return startInstanceWithDefinition(startForm, definitionEntity, command.getStartEmployeeId());
    }

    private ResponseDTO<Long> startInstanceWithDefinition(
            BpmInstanceStartForm startForm,
            BpmDefinitionEntity definitionEntity,
            Long employeeId
    ) {
        ResponseDTO<Long> validationResponse = validateCurrentStartableDefinition(definitionEntity, employeeId);
        if (validationResponse != null) {
            return validationResponse;
        }

        if (StringUtils.isNotBlank(definitionEntity.getFormSchemaSnapshotJson())) {
            ResponseDTO<String> formDataResponse = bpmRuntimeFormDataValidator.validateFullData(
                    definitionEntity.getFormSchemaSnapshotJson(),
                    startForm.getFormDataJson()
            );
            if (!Boolean.TRUE.equals(formDataResponse.getOk())) {
                return ResponseDTO.userErrorParam(formDataResponse.getMsg());
            }
        }

        BpmEmployeeSnapshot employeeSnapshot = bpmOrgIdentityGateway.requireEmployee(employeeId);
        SerialNumberIdEnum serialNumberIdEnum = SmartEnumUtil.getEnumByValue(
                definitionEntity.getInstanceNoRuleIdSnapshot(),
                SerialNumberIdEnum.class
        );
        if (serialNumberIdEnum == null) {
            serialNumberIdEnum = SerialNumberIdEnum.ORDER;
        }

        String instanceNo = serialNumberService.generate(serialNumberIdEnum);
        List<BpmDefinitionNodeEntity> definitionNodes = bpmDefinitionNodeDao.selectList(
                Wrappers.<BpmDefinitionNodeEntity>lambdaQuery()
                        .eq(BpmDefinitionNodeEntity::getDefinitionId, definitionEntity.getDefinitionId())
                        .orderByAsc(BpmDefinitionNodeEntity::getSortOrder, BpmDefinitionNodeEntity::getDefinitionNodeId)
        );

        Map<String, Object> runtimeAssignmentVariables;
        try {
            runtimeAssignmentVariables = bpmTaskAssignmentResolver.resolve(
                    definitionNodes,
                    new BpmTaskAssignmentContext(employeeSnapshot, startForm.getFormDataJson())
            );
        } catch (IllegalArgumentException ex) {
            return ResponseDTO.userErrorParam(ex.getMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        BpmInstanceEntity entity = new BpmInstanceEntity();
        entity.setInstanceNo(instanceNo);
        entity.setDefinitionId(definitionEntity.getDefinitionId());
        entity.setEngineProcessDefinitionId(definitionEntity.getEngineProcessDefinitionId());
        entity.setDefinitionKeySnapshot(definitionEntity.getDefinitionKey());
        entity.setDefinitionVersionSnapshot(definitionEntity.getDefinitionVersion());
        entity.setCategoryIdSnapshot(definitionEntity.getCategoryIdSnapshot());
        entity.setCategoryNameSnapshot(definitionEntity.getCategoryNameSnapshot());
        entity.setTitle(StringUtils.isBlank(startForm.getTitle()) ? definitionEntity.getDefinitionName() : startForm.getTitle());
        entity.setSummary(startForm.getSummary());
        entity.setStartEmployeeId(employeeSnapshot.employeeId());
        entity.setStartEmployeeNameSnapshot(employeeSnapshot.actualName());
        entity.setStartDepartmentIdSnapshot(employeeSnapshot.departmentId());
        entity.setStartDepartmentNameSnapshot(employeeSnapshot.departmentName());
        entity.setBusinessType(startForm.getBusinessType());
        entity.setBusinessId(startForm.getBusinessId());
        entity.setBusinessKey(startForm.getBusinessKey());
        entity.setInitialFormDataSnapshotJson(startForm.getFormDataJson());
        entity.setCurrentFormDataSnapshotJson(startForm.getFormDataJson());
        entity.setFormDataVersion(1L);
        entity.setRunState(BpmInstanceRunStateEnum.RUNNING.getValue());
        entity.setActiveTaskCount(0);
        entity.setStartedAt(now);
        entity.setLastActionAt(now);
        bpmInstanceDao.insert(entity);
        if (StringUtils.isNotBlank(definitionEntity.getFormSchemaSnapshotJson())) {
            insertInitialFormDataChange(entity, employeeSnapshot);
        }
        String engineProcessInstanceId = flowableProcessInstanceGateway.start(
                definitionEntity.getEngineProcessDefinitionId(),
                entity.getInstanceId(),
                employeeId,
                startForm.getFormDataJson(),
                runtimeAssignmentVariables
        );
        BpmInstanceEntity engineUpdate = new BpmInstanceEntity();
        engineUpdate.setInstanceId(entity.getInstanceId());
        engineUpdate.setEngineProcessInstanceId(engineProcessInstanceId);
        bpmInstanceDao.updateById(engineUpdate);
        bpmTaskProjectionService.syncActiveTasksForInstance(entity.getInstanceId());
        return ResponseDTO.ok(entity.getInstanceId());
    }

    private void insertInitialFormDataChange(
            BpmInstanceEntity instance,
            BpmEmployeeSnapshot actor
    ) {
        JSONObject initialData = JSON.parseObject(
                StringUtils.defaultIfBlank(instance.getInitialFormDataSnapshotJson(), "{}")
        );
        JSONArray changedFields = new JSONArray();
        initialData.keySet().forEach(changedFields::add);
        BpmFormDataChangeEntity change = new BpmFormDataChangeEntity();
        change.setInstanceId(instance.getInstanceId());
        change.setChangeSource(BpmFormDataChangeSourceEnum.INSTANCE_STARTED.name());
        change.setActorEmployeeId(actor.employeeId());
        change.setActorNameSnapshot(actor.actualName());
        change.setBeforeVersion(0L);
        change.setAfterVersion(1L);
        change.setChangedFieldsJson(JSON.toJSONString(changedFields));
        change.setBeforeValuesJson("{}");
        change.setAfterValuesJson(JSON.toJSONString(initialData));
        bpmFormDataChangeDao.insert(change);
    }

    private BpmRuntimeStartDraftVO buildStartDraft(
            BpmDefinitionEntity definitionEntity,
            String title,
            String summary,
            String formDataJson,
            Long sourceInstanceId,
            Long formDataVersion
    ) {
        BpmRuntimeStartDraftVO draftVO = new BpmRuntimeStartDraftVO();
        draftVO.setDefinitionId(definitionEntity.getDefinitionId());
        draftVO.setDefinitionName(definitionEntity.getDefinitionName());
        draftVO.setFormNameSnapshot(definitionEntity.getFormNameSnapshot());
        draftVO.setFormSchemaSnapshotJson(definitionEntity.getFormSchemaSnapshotJson());
        draftVO.setTitle(StringUtils.isBlank(title) ? definitionEntity.getDefinitionName() : title);
        draftVO.setSummary(summary);
        draftVO.setFormDataJson(StringUtils.defaultIfBlank(formDataJson, "{}"));
        draftVO.setFormDataVersion(formDataVersion);
        draftVO.setSourceInstanceId(sourceInstanceId);
        return draftVO;
    }

    private BpmRuntimeStartDraftVO buildGraphStartDraft(GraphDefinitionVersionEntity graphVersion) {
        BpmRuntimeStartDraftVO draftVO = new BpmRuntimeStartDraftVO();
        draftVO.setGraphDefinitionVersionId(graphVersion.getGraphDefinitionVersionId());
        draftVO.setDefinitionSource("GRAPH");
        draftVO.setDefinitionName(StringUtils.defaultIfBlank(
                graphVersion.getProcessNameSnapshot(),
                graphVersion.getProcessKey()
        ));
        draftVO.setTitle(draftVO.getDefinitionName());
        draftVO.setFormDataJson("{}");
        return draftVO;
    }

    private <T> ResponseDTO<T> validateCurrentStartableDefinition(BpmDefinitionEntity definitionEntity) {
        return validateCurrentStartableDefinition(definitionEntity, bpmCurrentActorProvider.requireCurrentEmployeeId());
    }

    private <T> ResponseDTO<T> validateCurrentStartableDefinition(BpmDefinitionEntity definitionEntity, Long employeeId) {
        if (definitionEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        if (employeeId == null) {
            return ResponseDTO.userErrorParam("发起员工不能为空");
        }
        if (!BpmDefinitionLifecycleStateEnum.CURRENT.equalsValue(definitionEntity.getLifecycleState())) {
            return ResponseDTO.userErrorParam("当前流程定义不是可运行的当前版本");
        }
        if (!BpmDefinitionStartStateEnum.STARTABLE.equalsValue(definitionEntity.getStartState())) {
            return ResponseDTO.userErrorParam("当前流程定义已停用，无法发起");
        }
        if (!canEmployeeStart(definitionEntity.getStartScopeJson(), employeeId)) {
            return ResponseDTO.userErrorParam("当前流程定义不在可发起范围内");
        }
        return null;
    }

    private boolean canEmployeeStart(String startScopeJson, Long employeeId) {
        if (StringUtils.isBlank(startScopeJson)) {
            return true;
        }
        try {
            JSONObject scopeObject = JSON.parseObject(startScopeJson);
            String type = scopeObject.getString("type");
            if (StringUtils.isBlank(type) || "ALL".equalsIgnoreCase(type)) {
                return true;
            }
            if ("EMPLOYEE".equalsIgnoreCase(type)) {
                return containsLong(scopeObject.getJSONArray("employeeIds"), employeeId);
            }
            if ("DEPARTMENT".equalsIgnoreCase(type)) {
                BpmEmployeeSnapshot employeeSnapshot = bpmOrgIdentityGateway.requireEmployee(employeeId);
                return containsLong(scopeObject.getJSONArray("departmentIds"), employeeSnapshot.departmentId());
            }
            if ("ROLE".equalsIgnoreCase(type)) {
                JSONArray roleIds = scopeObject.getJSONArray("roleIds");
                if (roleIds == null || roleIds.isEmpty()) {
                    return false;
                }
                for (Object roleId : roleIds) {
                    if (roleId != null && bpmOrgIdentityGateway.listEmployeeIdsByRoleId(Long.valueOf(roleId.toString())).contains(employeeId)) {
                        return true;
                    }
                }
                return false;
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean containsLong(JSONArray values, Long targetValue) {
        if (values == null || targetValue == null) {
            return false;
        }
        for (Object value : values) {
            if (value != null && targetValue.toString().equals(value.toString())) {
                return true;
            }
        }
        return false;
    }

    private void closePendingTasks(Long instanceId, LocalDateTime actionAt) {
        List<BpmTaskEntity> pendingTasks = bpmTaskDao.selectList(Wrappers.<BpmTaskEntity>lambdaQuery()
                .eq(BpmTaskEntity::getInstanceId, instanceId)
                .isNull(BpmTaskEntity::getApprovalGroupId)
                .eq(BpmTaskEntity::getTaskState, BpmTaskStateEnum.PENDING.getValue()));
        for (BpmTaskEntity pendingTask : pendingTasks) {
            BpmTaskEntity updateTaskEntity = new BpmTaskEntity();
            updateTaskEntity.setTaskId(pendingTask.getTaskId());
            updateTaskEntity.setTaskState(BpmTaskStateEnum.CANCELLED.getValue());
            updateTaskEntity.setTaskResult(BpmTaskResultEnum.INSTANCE_CANCELLED.getValue());
            updateTaskEntity.setCancelledAt(actionAt);
            updateTaskEntity.setLastActionAt(actionAt);
            bpmTaskDao.updateById(updateTaskEntity);
        }
    }

    private BpmTaskActionLogEntity buildInstanceActionLog(
            BpmInstanceEntity instanceEntity,
            BpmEmployeeSnapshot actorSnapshot,
            String commentText,
            LocalDateTime actionAt
    ) {
        BpmTaskActionLogEntity actionLogEntity = new BpmTaskActionLogEntity();
        actionLogEntity.setInstanceId(instanceEntity.getInstanceId());
        actionLogEntity.setDefinitionId(instanceEntity.getDefinitionId());
        actionLogEntity.setActionType("INSTANCE_CANCELLED");
        actionLogEntity.setActorEmployeeId(actorSnapshot.employeeId());
        actionLogEntity.setActorNameSnapshot(actorSnapshot.actualName());
        actionLogEntity.setCommentText(commentText);
        actionLogEntity.setActionAt(actionAt);
        return actionLogEntity;
    }

    private BpmTaskActionLogEntity buildAdminInstanceCancelActionLog(
            BpmInstanceEntity instanceEntity,
            BpmEmployeeSnapshot actorSnapshot,
            String commentText,
            LocalDateTime actionAt
    ) {
        BpmTaskActionLogEntity actionLogEntity = new BpmTaskActionLogEntity();
        actionLogEntity.setInstanceId(instanceEntity.getInstanceId());
        actionLogEntity.setDefinitionId(instanceEntity.getDefinitionId());
        actionLogEntity.setActionType("ADMIN_INSTANCE_CANCELLED");
        actionLogEntity.setActorEmployeeId(actorSnapshot.employeeId());
        actionLogEntity.setActorNameSnapshot(actorSnapshot.actualName());
        actionLogEntity.setCommentText(commentText);
        actionLogEntity.setActionAt(actionAt);
        return actionLogEntity;
    }

    private BpmTaskActionLogEntity buildResubmitActionLog(
            BpmInstanceEntity instanceEntity,
            BpmEmployeeSnapshot actorSnapshot,
            LocalDateTime actionAt
    ) {
        BpmTaskActionLogEntity actionLogEntity = new BpmTaskActionLogEntity();
        actionLogEntity.setInstanceId(instanceEntity.getInstanceId());
        actionLogEntity.setDefinitionId(instanceEntity.getDefinitionId());
        actionLogEntity.setActionType("RESUBMITTED");
        actionLogEntity.setActorEmployeeId(actorSnapshot.employeeId());
        actionLogEntity.setActorNameSnapshot(actorSnapshot.actualName());
        actionLogEntity.setActionAt(actionAt);
        return actionLogEntity;
    }
}
