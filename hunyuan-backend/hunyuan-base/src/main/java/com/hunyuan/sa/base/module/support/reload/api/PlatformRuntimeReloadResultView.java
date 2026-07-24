package com.hunyuan.sa.base.module.support.reload.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 运行时重载执行结果公开视图。
 */
@Data
public class PlatformRuntimeReloadResultView {

    @Schema(description = "重载项标签")
    private String tag;

    @Schema(description = "重载参数")
    private String args;

    @Schema(description = "执行是否成功")
    private Boolean result;

    @Schema(description = "异常信息")
    private String exception;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
