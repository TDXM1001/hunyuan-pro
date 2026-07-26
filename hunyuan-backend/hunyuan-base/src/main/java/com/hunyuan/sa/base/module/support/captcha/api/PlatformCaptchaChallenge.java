package com.hunyuan.sa.base.module.support.captcha.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 图形验证码公开视图，字段名保持历史登录和验证码接口兼容。
 */
@Data
public class PlatformCaptchaChallenge {

    @Schema(description = "验证码唯一标识")
    private String captchaUuid;

    @Schema(description = "验证码文本，仅非生产环境返回")
    private String captchaText;

    @Schema(description = "验证码 Base64 图片")
    private String captchaBase64Image;

    @Schema(description = "过期时间（秒）")
    private Long expireSeconds;
}
