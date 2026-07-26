package com.hunyuan.sa.base.module.support.captcha.api;

/**
 * 图形验证码校验命令。
 */
public record PlatformCaptchaValidationCommand(String captchaUuid, String captchaCode) {
}
