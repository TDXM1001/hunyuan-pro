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
import com.hunyuan.sa.bpm.common.enumeration.BpmDefinitionLifecycleStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmDefinitionStartStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceResultStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceRunStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskResultEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskStateEnum;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionDao;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskActionLogEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmAdminInstanceCancelForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceCancelForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceQueryForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceResubmitForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceStartForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceDetailVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmRuntimeStartDraftVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmStartableDefinitionVO;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 流程实例服务。
 */
@Service
public class BpmInstanceService {

    @Resource
    private BpmDefinitionDao bpmDefinitionDao;

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
    private SerialNumberService serialNumberService;

    @Resource
    private BpmTaskAssignmentResolver bpmTaskAssignmentResolver;

    @Resource
    private BpmTaskProjectionService bpmTaskProjectionService;

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
        List<BpmStartableDefinitionVO> list = bpmDefinitionDao.queryStartableList(employeeId).stream()
                .filter(definition -> canEmployeeStart(definition.getStartScopeJson(), employeeId))
                .toList();
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
                null
        ));
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
        detail.setCurrentTasks(bpmTaskDao.queryCurrentTasksByInstanceId(instanceId));
        detail.setActionLogs(bpmTaskActionLogDao.queryByInstanceId(instanceId));
        return ResponseDTO.ok(detail);
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
                instanceEntity.getInstanceId()
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
        closePendingTasks(instanceEntity.getInstanceId(), now);

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
        closePendingTasks(instanceEntity.getInstanceId(), now);

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
        return ResponseDTO.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Long> resubmitMyInstance(BpmInstanceResubmitForm resubmitForm) {
        BpmInstanceEntity instanceEntity = bpmInstanceDao.selectById(resubmitForm.getInstanceId());
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
        ResponseDTO<Long> validationResponse = validateCurrentStartableDefinition(definitionEntity);
        if (validationResponse != null) {
            return validationResponse;
        }

        BpmEmployeeSnapshot employeeSnapshot = bpmOrgIdentityGateway.requireEmployee(employeeId);
        List<BpmDefinitionNodeEntity> definitionNodes = bpmDefinitionNodeDao.selectList(
                Wrappers.<BpmDefinitionNodeEntity>lambdaQuery()
                        .eq(BpmDefinitionNodeEntity::getDefinitionId, definitionEntity.getDefinitionId())
                        .orderByAsc(BpmDefinitionNodeEntity::getSortOrder, BpmDefinitionNodeEntity::getDefinitionNodeId)
        );

        Map<String, Object> runtimeAssignmentVariables;
        try {
            runtimeAssignmentVariables = bpmTaskAssignmentResolver.resolve(definitionNodes, employeeSnapshot);
        } catch (IllegalArgumentException ex) {
            return ResponseDTO.userErrorParam(ex.getMessage());
        }

        String engineProcessInstanceId = flowableProcessInstanceGateway.start(
                definitionEntity.getEngineProcessDefinitionId(),
                employeeId,
                resubmitForm.getFormDataJson(),
                runtimeAssignmentVariables
        );

        LocalDateTime now = LocalDateTime.now();
        BpmInstanceEntity updateInstanceEntity = new BpmInstanceEntity();
        updateInstanceEntity.setInstanceId(instanceEntity.getInstanceId());
        updateInstanceEntity.setEngineProcessDefinitionId(definitionEntity.getEngineProcessDefinitionId());
        updateInstanceEntity.setEngineProcessInstanceId(engineProcessInstanceId);
        updateInstanceEntity.setDefinitionKeySnapshot(definitionEntity.getDefinitionKey());
        updateInstanceEntity.setDefinitionVersionSnapshot(definitionEntity.getDefinitionVersion());
        updateInstanceEntity.setCategoryIdSnapshot(definitionEntity.getCategoryIdSnapshot());
        updateInstanceEntity.setCategoryNameSnapshot(definitionEntity.getCategoryNameSnapshot());
        updateInstanceEntity.setTitle(StringUtils.isBlank(resubmitForm.getTitle()) ? definitionEntity.getDefinitionName() : resubmitForm.getTitle());
        updateInstanceEntity.setSummary(resubmitForm.getSummary());
        updateInstanceEntity.setCurrentFormDataSnapshotJson(resubmitForm.getFormDataJson());
        updateInstanceEntity.setRunState(BpmInstanceRunStateEnum.RUNNING.getValue());
        updateInstanceEntity.setResultState(null);
        updateInstanceEntity.setActiveTaskCount(0);
        updateInstanceEntity.setCurrentNodeSummaryJson(null);
        updateInstanceEntity.setLastActionAt(now);
        bpmInstanceDao.updateById(updateInstanceEntity);

        bpmTaskActionLogDao.insert(buildResubmitActionLog(instanceEntity, employeeSnapshot, now));
        bpmTaskProjectionService.syncActiveTasksForInstance(instanceEntity.getInstanceId());
        return ResponseDTO.ok(instanceEntity.getInstanceId());
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Long> startInstance(BpmInstanceStartForm startForm) {
        Long employeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        BpmDefinitionEntity definitionEntity = bpmDefinitionDao.selectById(startForm.getDefinitionId());
        return startInstanceWithDefinition(startForm, definitionEntity, employeeId);
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
            runtimeAssignmentVariables = bpmTaskAssignmentResolver.resolve(definitionNodes, employeeSnapshot);
        } catch (IllegalArgumentException ex) {
            return ResponseDTO.userErrorParam(ex.getMessage());
        }

        String engineProcessInstanceId = flowableProcessInstanceGateway.start(
                definitionEntity.getEngineProcessDefinitionId(),
                employeeId,
                startForm.getFormDataJson(),
                runtimeAssignmentVariables
        );

        LocalDateTime now = LocalDateTime.now();
        BpmInstanceEntity entity = new BpmInstanceEntity();
        entity.setInstanceNo(instanceNo);
        entity.setDefinitionId(definitionEntity.getDefinitionId());
        entity.setEngineProcessDefinitionId(definitionEntity.getEngineProcessDefinitionId());
        entity.setEngineProcessInstanceId(engineProcessInstanceId);
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
        entity.setRunState(BpmInstanceRunStateEnum.RUNNING.getValue());
        entity.setActiveTaskCount(0);
        entity.setStartedAt(now);
        entity.setLastActionAt(now);
        bpmInstanceDao.insert(entity);
        bpmTaskProjectionService.syncActiveTasksForInstance(entity.getInstanceId());
        return ResponseDTO.ok(entity.getInstanceId());
    }

    private BpmRuntimeStartDraftVO buildStartDraft(
            BpmDefinitionEntity definitionEntity,
            String title,
            String summary,
            String formDataJson,
            Long sourceInstanceId
    ) {
        BpmRuntimeStartDraftVO draftVO = new BpmRuntimeStartDraftVO();
        draftVO.setDefinitionId(definitionEntity.getDefinitionId());
        draftVO.setDefinitionName(definitionEntity.getDefinitionName());
        draftVO.setFormNameSnapshot(definitionEntity.getFormNameSnapshot());
        draftVO.setFormSchemaSnapshotJson(definitionEntity.getFormSchemaSnapshotJson());
        draftVO.setTitle(StringUtils.isBlank(title) ? definitionEntity.getDefinitionName() : title);
        draftVO.setSummary(summary);
        draftVO.setFormDataJson(StringUtils.defaultIfBlank(formDataJson, "{}"));
        draftVO.setSourceInstanceId(sourceInstanceId);
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
