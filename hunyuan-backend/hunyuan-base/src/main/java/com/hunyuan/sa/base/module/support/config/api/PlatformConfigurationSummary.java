package com.hunyuan.sa.base.module.support.config.api;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台配置管理摘要，仅供已授权的管理端读取。
 */
@Data
public class PlatformConfigurationSummary {

    private Long configId;
    private String configKey;
    private String configName;
    private String configValue;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
