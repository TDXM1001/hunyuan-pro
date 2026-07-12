package com.hunyuan.sa.bpm.module.definition.domain.form;

/**
 * 发布命令不接受调用方提供的依赖版本，版本只能由 M2/M3 目录解析器冻结。
 */
public record GraphDefinitionPublishCommand(Long draftId, Long publishedByEmployeeId) {
}
