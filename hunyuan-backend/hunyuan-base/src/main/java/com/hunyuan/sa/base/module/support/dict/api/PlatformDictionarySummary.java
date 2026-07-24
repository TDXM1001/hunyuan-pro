package com.hunyuan.sa.base.module.support.dict.api;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台字典摘要，供管理端稳定读取接口使用。
 */
@Data
public class PlatformDictionarySummary {

    private Long dictId;
    private String dictName;
    private String dictCode;
    private String remark;
    private Boolean disabledFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
