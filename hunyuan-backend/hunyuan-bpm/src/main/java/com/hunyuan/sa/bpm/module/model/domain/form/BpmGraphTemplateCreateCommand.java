package com.hunyuan.sa.bpm.module.model.domain.form;

/**
 * 从 Graph 草稿冻结模板的服务命令。
 */
public record BpmGraphTemplateCreateCommand(
        String templateKey,
        String templateName,
        Long sourceDraftId,
        Long operatorEmployeeId
) {
}
