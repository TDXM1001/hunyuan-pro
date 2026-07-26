package com.hunyuan.sa.base.module.support.captcha.api;

import com.hunyuan.sa.base.common.domain.ResponseDTO;

/**
 * 图形验证码生成与一次性校验的稳定访问边界。
 */
public interface PlatformCaptchaFacade {

    ResponseDTO<PlatformCaptchaChallenge> generateChallenge();

    ResponseDTO<String> validate(PlatformCaptchaValidationCommand command);
}
