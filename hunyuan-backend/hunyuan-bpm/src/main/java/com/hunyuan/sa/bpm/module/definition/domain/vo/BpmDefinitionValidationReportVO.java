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

    @Schema(description = "候选策略预检结果")
    private List<CandidateCheck> candidateChecks = new ArrayList<>();

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

    @Data
    public static class CandidateCheck {

        @Schema(description = "阻断编码")
        private String code;

        @Schema(description = "节点编码")
        private String nodeKey;

        @Schema(description = "节点名称")
        private String nodeName;

        @Schema(description = "候选解析类型")
        private String candidateResolverType;

        @Schema(description = "候选解析类型说明")
        private String candidateResolverLabel;

        @Schema(description = "依赖配置摘要")
        private String requiredConfig;

        @Schema(description = "当前是否可直接解析")
        private Boolean canResolveNow;

        @Schema(description = "是否依赖运行时表单值")
        private Boolean requiresRuntimeFormData;

        @Schema(description = "状态")
        private String status;

        @Schema(description = "提示信息")
        private String message;

        @Schema(description = "关联字段")
        private String field;
    }
}
