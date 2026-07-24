package com.hunyuan.sa.base.module.support.dict.api;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台字典项，屏蔽历史字典 VO 与持久化对象。
 */
@Data
public class PlatformDictionaryItem {

    private Long dictDataId;
    private Long dictId;
    private String dictCode;
    private String dictName;
    private Boolean dictDisabledFlag;
    private String dataValue;
    private String dataLabel;
    private String dataStyle;
    private String remark;
    private Integer sortOrder;
    private Boolean disabledFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
