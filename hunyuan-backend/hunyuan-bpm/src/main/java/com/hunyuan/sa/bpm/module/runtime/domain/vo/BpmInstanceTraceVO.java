package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import com.hunyuan.sa.bpm.module.integration.domain.vo.BpmCallbackRecordVO;
import com.hunyuan.sa.bpm.module.integration.domain.vo.BpmCommandRecordVO;
import lombok.Data;

import java.util.List;

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
}
