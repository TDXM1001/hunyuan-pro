package com.hunyuan.sa.base.module.support.serialnumber.api;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台序列号定义摘要。
 */
@Data
public class PlatformSerialNumberDefinition {

    private Integer serialNumberId;
    private String businessName;
    private String format;
    private String ruleType;
    private Long initNumber;
    private Integer stepRandomRange;
    private String remark;
    private Long lastNumber;
    private LocalDateTime lastTime;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;
}
