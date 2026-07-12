package com.hunyuan.sa.bpm.module.runtime.domain.form;

import com.hunyuan.sa.base.common.domain.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 外部等待分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmExternalWaitQueryForm extends PageParam {
    private Long instanceId;
    private String connectorKey;
    private String waitStatus;
}
