package com.hunyuan.sa.admin.module.platform.security.captcha.application;

import com.hunyuan.sa.admin.module.platform.security.captcha.CaptchaService;
import com.hunyuan.sa.admin.module.platform.security.captcha.domain.CaptchaForm;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.module.support.captcha.api.PlatformCaptchaChallenge;
import com.hunyuan.sa.base.module.support.captcha.api.PlatformCaptchaFacade;
import com.hunyuan.sa.base.module.support.captcha.api.PlatformCaptchaValidationCommand;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 图形验证码公开协议适配器，隔离历史 Form/VO 与 Redis 实现。
 */
@Service
public class PlatformCaptchaApplicationService implements PlatformCaptchaFacade {

    @Resource
    private CaptchaService captchaService;

    @Override
    public ResponseDTO<PlatformCaptchaChallenge> generateChallenge() {
        return ResponseDTO.ok(SmartBeanUtil.copy(
                captchaService.generateCaptcha(), PlatformCaptchaChallenge.class));
    }

    @Override
    public ResponseDTO<String> validate(PlatformCaptchaValidationCommand command) {
        CaptchaForm form = new CaptchaForm();
        form.setCaptchaUuid(command.captchaUuid());
        form.setCaptchaCode(command.captchaCode());
        return captchaService.checkCaptcha(form);
    }
}
