package com.hunyuan.sa.bpm.module.runtime.domain.form;

import com.hunyuan.sa.base.common.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程任务分页查询表单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmTaskQueryForm extends PageParam {

    @Schema(description = "实例编号")
    private String instanceNo;

    @Schema(description = "流程标题")
    private String instanceTitle;

    @Schema(description = "任务状态")
    private Integer taskState;

    @Schema(hidden = true)
    private Long assigneeEmployeeId;
}
