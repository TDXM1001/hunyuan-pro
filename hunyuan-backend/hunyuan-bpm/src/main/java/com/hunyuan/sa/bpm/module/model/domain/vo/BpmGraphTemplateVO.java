package com.hunyuan.sa.bpm.module.model.domain.vo;

import lombok.Data;

/**
 * Graph 模板冻结事实。
 */
@Data
public class BpmGraphTemplateVO {

    private Long templateId;

    private Long sourceDraftId;

    private String templateKey;

    private String templateName;

    private String semanticHash;
}
