package com.hunyuan.sa.bpm.module.runtime.service;

import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessIntegrationRecordService;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceDetailVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceTraceVO;
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
        return ResponseDTO.ok(trace);
    }
}
