package com.hunyuan.sa.bpm.module.model.domain.form;

/**
 * 从完整 Graph 文档创建新草稿的服务命令。
 */
public record BpmGraphDraftImportCommand(
        String processKey,
        String processName,
        Long categoryId,
        String graphDocumentJson,
        Long operatorEmployeeId
) {
}
