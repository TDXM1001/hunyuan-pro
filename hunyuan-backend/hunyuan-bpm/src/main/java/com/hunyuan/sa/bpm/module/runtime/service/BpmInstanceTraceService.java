package com.hunyuan.sa.bpm.module.runtime.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessIntegrationRecordService;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceDetailVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceTraceVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmFormDataChangeVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmApprovalStageMemberTraceVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmApprovalStageTraceVO;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmFormDataChangeEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageMemberEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmFormDataChangeDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTimeEventDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmExternalWaitDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageMemberDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmSubProcessLinkDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTimeEventEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmExternalWaitEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmSubProcessLinkEntity;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * BPM 实例可靠性追踪服务。
 */
@Service
public class BpmInstanceTraceService {

    @Resource
    private BpmInstanceService bpmInstanceService;

    @Resource
    private BpmBusinessIntegrationRecordService integrationRecordService;

    @Resource
    private BpmNotificationRecordService notificationRecordService;

    @Resource
    private BpmFormDataChangeDao bpmFormDataChangeDao;

    @Resource
    private BpmRuntimeGraphService bpmRuntimeGraphService;

    @Resource
    private BpmTimeEventDao bpmTimeEventDao;

    @Resource
    private BpmExternalWaitDao bpmExternalWaitDao;

    @Resource
    private BpmApprovalStageDao bpmApprovalStageDao;

    @Resource
    private BpmApprovalStageMemberDao bpmApprovalStageMemberDao;

    @Resource
    private BpmTaskDao bpmTaskDao;

    @Resource
    private BpmSubProcessLinkDao bpmSubProcessLinkDao;

    public ResponseDTO<BpmInstanceTraceVO> getTrace(Long instanceId) {
        return getTrace(instanceId, false);
    }

    public ResponseDTO<BpmInstanceTraceVO> getEmployeeTrace(Long instanceId) {
        return getTrace(instanceId, true);
    }

    private ResponseDTO<BpmInstanceTraceVO> getTrace(Long instanceId, boolean employeeSafe) {
        ResponseDTO<BpmInstanceDetailVO> detailResponse = bpmInstanceService.getDetail(instanceId);
        if (!Boolean.TRUE.equals(detailResponse.getOk()) || detailResponse.getData() == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        BpmInstanceDetailVO detail = detailResponse.getData();
        if (employeeSafe) {
            detail.setCurrentFormDataSnapshotJson(null);
        }
        BpmInstanceTraceVO trace = new BpmInstanceTraceVO();
        trace.setInstance(detail);
        trace.setCurrentTasks(detail.getCurrentTasks() == null ? List.of() : detail.getCurrentTasks());
        trace.setActionLogs(detail.getActionLogs() == null ? List.of() : detail.getActionLogs());
        trace.setCallbackRecords(integrationRecordService.queryCallbackRecordsByInstanceId(instanceId));
        trace.setCommandRecords(integrationRecordService.queryCommandRecordsByInstanceId(instanceId));
        trace.setNotificationRecords(notificationRecordService.queryByInstanceId(instanceId));
        List<BpmTimeEventEntity> timeEvents = bpmTimeEventDao.selectList(
                Wrappers.<BpmTimeEventEntity>lambdaQuery()
                        .eq(BpmTimeEventEntity::getInstanceId, instanceId)
                        .orderByAsc(BpmTimeEventEntity::getScheduledAt)
        );
        List<BpmExternalWaitEntity> externalWaits = bpmExternalWaitDao.selectList(
                Wrappers.<BpmExternalWaitEntity>lambdaQuery()
                        .eq(BpmExternalWaitEntity::getInstanceId, instanceId)
                        .orderByAsc(BpmExternalWaitEntity::getCreateTime)
        );
        externalWaits.forEach(wait -> wait.setCallbackTokenHash(null));
        if (employeeSafe) {
            timeEvents.forEach(event -> event.setPolicySnapshotJson(null));
            externalWaits.forEach(wait -> {
                wait.setRequestSnapshotJson(null);
                wait.setCallbackPayloadSnapshotJson(null);
            });
        }
        trace.setTimeEvents(timeEvents);
        trace.setExternalWaits(externalWaits);
        List<BpmSubProcessLinkEntity> subProcesses = bpmSubProcessLinkDao.selectList(
                Wrappers.<BpmSubProcessLinkEntity>lambdaQuery()
                        .eq(BpmSubProcessLinkEntity::getParentInstanceId, instanceId)
                        .orderByAsc(BpmSubProcessLinkEntity::getCreateTime));
        if (employeeSafe) {
            subProcesses.forEach(link -> {
                link.setInputSnapshotJson(null);
                link.setOutputSnapshotJson(null);
                link.setParentEngineExecutionId(null);
                link.setChildEngineProcessInstanceId(null);
                link.setFailurePolicy(null);
                link.setCancelPropagation(null);
            });
        }
        trace.setSubProcesses(subProcesses);
        trace.setApprovalGroups(detail.getApprovalGroups() == null ? List.of() : detail.getApprovalGroups());
        trace.setApprovalStages(bpmApprovalStageDao.selectList(
                Wrappers.<BpmApprovalStageEntity>lambdaQuery()
                        .eq(BpmApprovalStageEntity::getInstanceId, instanceId)
                        .orderByAsc(BpmApprovalStageEntity::getOpenedAt)
                        .orderByAsc(BpmApprovalStageEntity::getApprovalStageId)
        ).stream().map(this::toApprovalStageTraceVO).toList());
        List<BpmFormDataChangeVO> formDataChanges = bpmFormDataChangeDao.queryByInstanceId(instanceId).stream()
                .map(this::toFormDataChangeVO)
                .toList();
        if (employeeSafe) {
            formDataChanges.forEach(change -> {
                change.setBeforeValuesJson("{}");
                change.setAfterValuesJson("{}");
            });
        }
        trace.setFormDataChanges(formDataChanges);
        var processGraph = bpmRuntimeGraphService.build(instanceId, employeeSafe);
        trace.setProcessGraph(processGraph);
        trace.setRouteDecisions(processGraph.getRouteDecisions());
        return ResponseDTO.ok(trace);
    }

    private BpmFormDataChangeVO toFormDataChangeVO(BpmFormDataChangeEntity entity) {
        BpmFormDataChangeVO vo = new BpmFormDataChangeVO();
        vo.setChangeId(entity.getChangeId());
        vo.setInstanceId(entity.getInstanceId());
        vo.setTaskId(entity.getTaskId());
        vo.setDefinitionNodeId(entity.getDefinitionNodeId());
        vo.setNodeKeySnapshot(entity.getNodeKeySnapshot());
        vo.setChangeSource(entity.getChangeSource());
        vo.setActorEmployeeId(entity.getActorEmployeeId());
        vo.setActorNameSnapshot(entity.getActorNameSnapshot());
        vo.setBeforeVersion(entity.getBeforeVersion());
        vo.setAfterVersion(entity.getAfterVersion());
        vo.setChangedFieldsJson(entity.getChangedFieldsJson());
        vo.setBeforeValuesJson(entity.getBeforeValuesJson());
        vo.setAfterValuesJson(entity.getAfterValuesJson());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private BpmApprovalStageTraceVO toApprovalStageTraceVO(BpmApprovalStageEntity stage) {
        List<BpmApprovalStageMemberEntity> members = bpmApprovalStageMemberDao
                .selectByApprovalStageId(stage.getApprovalStageId());
        BpmApprovalStageTraceVO vo = new BpmApprovalStageTraceVO();
        vo.setApprovalStageId(stage.getApprovalStageId());
        vo.setStageInvocationId(stage.getStageInvocationId());
        vo.setAuthoredNodeId(stage.getAuthoredNodeId());
        vo.setGeneration(stage.getGeneration());
        vo.setStageState(stage.getStageState());
        vo.setTerminalReason(stage.getTerminalReason());
        vo.setCompletionMode(stage.getCompletionMode());
        vo.setRatioPercent(stage.getRatioPercent());
        vo.setEffectiveMemberCount(stage.getEffectiveMemberCount());
        vo.setRequiredApprovalCount(stage.getRequiredApprovalCount());
        vo.setApprovedMemberCount((int) members.stream()
                .filter(member -> "APPROVED".equals(member.getMemberState()))
                .count());
        vo.setProcessedMemberCount((int) members.stream()
                .filter(member -> !Set.of("PLANNED", "ACTIVE").contains(member.getMemberState()))
                .count());
        vo.setCandidatePolicyVersionId(stage.getCandidatePolicyVersionId());
        vo.setApprovalPolicyVersionId(stage.getApprovalPolicyVersionId());
        vo.setOpenedAt(stage.getOpenedAt());
        vo.setClosedAt(stage.getClosedAt());
        vo.setMembers(members.stream().map(this::toApprovalStageMemberTraceVO).toList());
        return vo;
    }

    private BpmApprovalStageMemberTraceVO toApprovalStageMemberTraceVO(BpmApprovalStageMemberEntity member) {
        BpmApprovalStageMemberTraceVO vo = new BpmApprovalStageMemberTraceVO();
        vo.setApprovalStageMemberId(member.getApprovalStageMemberId());
        vo.setMemberOrder(member.getMemberOrder());
        vo.setSourceEmployeeId(member.getSourceEmployeeId());
        vo.setSourceEmployeeNameSnapshot(readSourceEmployeeName(member.getMemberSnapshotJson()));
        vo.setCurrentEmployeeId(member.getCurrentEmployeeId());
        BpmTaskEntity task = bpmTaskDao.selectByApprovalStageMemberId(member.getApprovalStageMemberId());
        vo.setCurrentEmployeeNameSnapshot(task == null ? null : task.getAssigneeNameSnapshot());
        vo.setMemberState(member.getMemberState());
        vo.setActionResult(member.getActionResult());
        vo.setActivatedAt(member.getActivatedAt());
        vo.setCompletedAt(member.getCompletedAt());
        vo.setCancelledAt(member.getCancelledAt());
        vo.setChangeReason(member.getChangeReason());
        return vo;
    }

    private String readSourceEmployeeName(String memberSnapshotJson) {
        try {
            JSONObject snapshot = JSON.parseObject(memberSnapshotJson);
            return snapshot == null ? null : snapshot.getString("displayName");
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
