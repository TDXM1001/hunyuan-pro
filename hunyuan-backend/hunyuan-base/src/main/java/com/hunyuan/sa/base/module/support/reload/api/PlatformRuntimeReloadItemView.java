package com.hunyuan.sa.base.module.support.reload.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 运行时重载项公开视图。
 */
@Data
public class PlatformRuntimeReloadItemView {

    @Schema(description = "重载项标签")
    private String tag;

    @Schema(description = "重载参数")
    private String args;

    @Schema(description = "运行标识")
    private String identification;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
