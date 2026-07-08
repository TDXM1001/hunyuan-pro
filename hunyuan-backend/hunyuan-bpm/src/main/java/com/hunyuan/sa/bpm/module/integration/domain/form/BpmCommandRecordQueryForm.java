package com.hunyuan.sa.bpm.module.integration.domain.form;

import com.hunyuan.sa.base.common.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * BPM 命令执行记录分页查询表单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmCommandRecordQueryForm extends PageParam {

    @Schema(description = "命令幂等键")
    private String commandKey;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "业务ID")
    private Long businessId;

    @Schema(description = "命令状态")
    private Integer commandStatus;
}
