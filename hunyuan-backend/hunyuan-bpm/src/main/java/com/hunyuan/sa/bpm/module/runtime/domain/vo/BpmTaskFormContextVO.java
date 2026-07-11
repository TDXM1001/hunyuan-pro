package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 员工任务可访问的运行表单上下文。
 */
@Data
public class BpmTaskFormContextVO {

    @Schema(description = "当前表单数据版本")
    private Long dataVersion;

    @Schema(description = "按节点权限裁剪后的表单Schema JSON")
    private String formSchemaJson;

    @Schema(description = "按节点权限裁剪后的表单数据JSON")
    private String formDataJson;

    @Schema(description = "当前节点可见字段权限")
    private List<BpmFieldPermissionVO> permissions;
}
