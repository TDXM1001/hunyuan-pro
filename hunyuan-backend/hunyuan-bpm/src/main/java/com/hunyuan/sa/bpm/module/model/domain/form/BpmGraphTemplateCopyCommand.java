package com.hunyuan.sa.bpm.module.model.domain.form;

/**
 * 从模板复制一个拥有新稳定 ID 的流程草稿。
 */
public record BpmGraphTemplateCopyCommand(
        Long templateId,
        String processKey,
        String processName,
        Long categoryId,
        Long operatorEmployeeId
) {
}
