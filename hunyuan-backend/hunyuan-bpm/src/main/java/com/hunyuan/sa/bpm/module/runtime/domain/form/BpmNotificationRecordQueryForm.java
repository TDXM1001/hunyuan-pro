package com.hunyuan.sa.bpm.module.runtime.domain.form;

import com.hunyuan.sa.base.common.domain.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * BPM 通知投递记录查询表单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmNotificationRecordQueryForm extends PageParam {

    private Long instanceId;

    private Long taskId;

    private String eventKey;

    private String channel;

    private Long receiverEmployeeId;

    private Integer sendStatus;
}
