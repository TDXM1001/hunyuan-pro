package com.hunyuan.sa.bpm.engine.compiler;

/**
 * 可稳定进入发布报告的流程模型校验结果。
 */
public record ProcessValidationFinding(
        String level,
        String code,
        String message,
        String nodeKey,
        String branchKey,
        String fieldKey,
        String fixHint
) {
    public static ProcessValidationFinding blocking(
            String code,
            String message,
            String nodeKey,
            String branchKey,
            String fieldKey,
            String fixHint
    ) {
        return new ProcessValidationFinding(
                "BLOCKING",
                code,
                message,
                nodeKey,
                branchKey,
                fieldKey,
                fixHint
        );
    }
}
