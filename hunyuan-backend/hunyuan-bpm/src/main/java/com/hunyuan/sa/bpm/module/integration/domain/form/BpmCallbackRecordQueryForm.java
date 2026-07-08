package com.hunyuan.sa.bpm.module.integration.domain.form;

import com.hunyuan.sa.base.common.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * BPM 业务回调记录分页查询表单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmCallbackRecordQueryForm extends PageParam {

    @Schema(description = "事件ID")
    private String eventId;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "业务ID")
    private Long businessId;

    @Schema(description = "回调状态")
    private Integer callbackStatus;
}
