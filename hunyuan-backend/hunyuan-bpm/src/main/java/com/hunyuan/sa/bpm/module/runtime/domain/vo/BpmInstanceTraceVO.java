package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import com.hunyuan.sa.bpm.module.integration.domain.vo.BpmCallbackRecordVO;
import com.hunyuan.sa.bpm.module.integration.domain.vo.BpmCommandRecordVO;
import lombok.Data;

import java.util.List;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTimeEventEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmExternalWaitEntity;

/**
 * BPM 实例可靠性追踪。
 */
@Data
public class BpmInstanceTraceVO {

    private BpmInstanceDetailVO instance;

    private List<BpmTaskVO> currentTasks;

    private List<BpmTaskActionLogVO> actionLogs;

    private List<BpmCallbackRecordVO> callbackRecords;

    private List<BpmCommandRecordVO> commandRecords;

    private List<BpmNotificationRecordVO> notificationRecords;

    private List<BpmTimeEventEntity> timeEvents;

    private List<BpmExternalWaitEntity> externalWaits;

    private List<BpmApprovalGroupDetailVO> approvalGroups;

    private List<BpmApprovalStageTraceVO> approvalStages;

    private List<BpmFormDataChangeVO> formDataChanges;

    private BpmRuntimeGraphVO processGraph;

    private List<BpmRuntimeGraphVO.RouteDecision> routeDecisions;
}
