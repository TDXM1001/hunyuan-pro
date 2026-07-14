package com.hunyuan.sa.bpm.module.model.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Graph 草稿保存后的版本事实。
 */
@Data
public class BpmGraphDraftVO {

    private Long draftId;

    private String processKey;

    private String processName;

    private Long categoryId;

    private Integer revision;

    private String graphJson;

    private String layoutJson;

    private String semanticHash;

    private String draftStatus;

    private LocalDateTime updateTime;
}
