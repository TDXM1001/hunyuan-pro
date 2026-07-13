package com.hunyuan.sa.bpm.module.runtime.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 任务拒绝表单。
 */
@Data
public class BpmTaskRejectForm {

    @Schema(description = "任务ID")
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @Schema(description = "客户端审批命令幂等标识")
    @Size(max = 128, message = "requestId 最多 128 个字符")
    private String requestId;

    @Schema(description = "拒绝意见")
    private String commentText;

    @Schema(description = "手工抄送员工ID列表")
    private List<Long> copyEmployeeIds;

    @Schema(description = "客户端加载的流程工作数据版本")
    private Long workingDataVersion;

    @Schema(description = "流程工作数据修改 JSON，拒绝通常为空")
    private String workingDataPatchJson;

    @Schema(description = "动作附件 JSON")
    private String actionAttachmentsJson;
}
