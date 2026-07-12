package com.hunyuan.sa.bpm.module.runtime.domain.form;

import com.hunyuan.sa.base.common.domain.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 时间事件分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmTimeEventQueryForm extends PageParam {
    private Long instanceId;
    private String eventKind;
    private String eventStatus;
}
