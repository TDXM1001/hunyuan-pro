package com.hunyuan.sa.bpm.module.runtime.service;

import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessIntegrationRecordService;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceDetailVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceTraceVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmFormDataChangeVO;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmFormDataChangeEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmFormDataChangeDao;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public ResponseDTO<BpmInstanceTraceVO> getTrace(Long instanceId) {
        ResponseDTO<BpmInstanceDetailVO> detailResponse = bpmInstanceService.getDetail(instanceId);
        if (!Boolean.TRUE.equals(detailResponse.getOk()) || detailResponse.getData() == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        BpmInstanceDetailVO detail = detailResponse.getData();
        BpmInstanceTraceVO trace = new BpmInstanceTraceVO();
        trace.setInstance(detail);
        trace.setCurrentTasks(detail.getCurrentTasks() == null ? List.of() : detail.getCurrentTasks());
        trace.setActionLogs(detail.getActionLogs() == null ? List.of() : detail.getActionLogs());
        trace.setCallbackRecords(integrationRecordService.queryCallbackRecordsByInstanceId(instanceId));
        trace.setCommandRecords(integrationRecordService.queryCommandRecordsByInstanceId(instanceId));
        trace.setNotificationRecords(notificationRecordService.queryByInstanceId(instanceId));
        trace.setApprovalGroups(detail.getApprovalGroups() == null ? List.of() : detail.getApprovalGroups());
        trace.setFormDataChanges(
                bpmFormDataChangeDao.queryByInstanceId(instanceId).stream()
                        .map(this::toFormDataChangeVO)
                        .toList()
        );
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
}
