package com.hunyuan.sa.bpm.module.model.domain.vo;

import lombok.Data;

/**
 * Graph 草稿保存后的版本事实。
 */
@Data
public class BpmGraphDraftVO {

    private Long draftId;

    private Integer revision;

    private String graphJson;

    private String layoutJson;

    private String semanticHash;
}
