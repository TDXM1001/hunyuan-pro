package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 审批节点字段权限。
 */
@Data
public class BpmFieldPermissionVO {

    @Schema(description = "表单字段key")
    private String fieldKey;

    @Schema(description = "READONLY/EDITABLE/HIDDEN")
    private String permission;

    @Schema(description = "当前节点是否必填")
    private Boolean required;
}
