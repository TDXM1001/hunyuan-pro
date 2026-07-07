package com.hunyuan.sa.bpm.module.runtime.domain.form;

import com.hunyuan.sa.base.common.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程实例分页查询表单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmInstanceQueryForm extends PageParam {

    @Schema(description = "实例编号")
    private String instanceNo;

    @Schema(description = "流程标题")
    private String title;

    @Schema(description = "运行状态")
    private Integer runState;

    @Schema(hidden = true)
    private Long startEmployeeId;
}
