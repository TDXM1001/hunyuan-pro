package com.hunyuan.sa.base.module.support.serialnumber.api;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 平台序列号生成记录摘要。
 */
@Data
public class PlatformSerialNumberRecord {

    private Integer serialNumberId;
    private LocalDate recordDate;
    private Long lastNumber;
    private LocalDateTime lastTime;
    private Long count;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;
}
