package com.hunyuan.sa.bpm.module.definition.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程定义发布前校验报告。
 */
@Data
public class BpmDefinitionValidationReportVO {

    @Schema(description = "是否允许发布")
    private Boolean pass;

    @Schema(description = "阻断项数量")
    private Integer blockingCount;

    @Schema(description = "警告项数量")
    private Integer warningCount;

    @Schema(description = "校验发现")
    private List<Finding> findings = new ArrayList<>();

    @Data
    public static class Finding {

        @Schema(description = "级别")
        private String level;

        @Schema(description = "编码")
        private String code;

        @Schema(description = "提示信息")
        private String message;

        @Schema(description = "节点编码")
        private String nodeKey;

        @Schema(description = "字段")
        private String field;
    }
}
