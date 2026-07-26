package com.hunyuan.sa.admin.module.platform.security.captcha;

import com.hunyuan.sa.admin.module.platform.security.captcha.application.PlatformCaptchaApplicationService;
import com.hunyuan.sa.admin.module.platform.security.captcha.domain.CaptchaForm;
import com.hunyuan.sa.admin.module.platform.security.captcha.domain.CaptchaVO;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.captcha.api.PlatformCaptchaValidationCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformCaptchaApplicationServiceTest {

    @Mock
    private CaptchaService captchaService;

    private PlatformCaptchaApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PlatformCaptchaApplicationService();
        ReflectionTestUtils.setField(service, "captchaService", captchaService);
    }

    @Test
    void mapsLegacyChallengeAndValidationCommand() {
        CaptchaVO legacy = new CaptchaVO();
        legacy.setCaptchaUuid("captcha-1");
        legacy.setCaptchaBase64Image("data:image/png;base64,image");
        legacy.setExpireSeconds(65L);
        when(captchaService.generateCaptcha()).thenReturn(legacy);
        when(captchaService.checkCaptcha(any())).thenReturn(ResponseDTO.ok());

        var challenge = service.generateChallenge().getData();
        service.validate(new PlatformCaptchaValidationCommand("captcha-1", "1234"));

        assertThat(challenge.getCaptchaUuid()).isEqualTo("captcha-1");
        assertThat(challenge.getExpireSeconds()).isEqualTo(65L);
        ArgumentCaptor<CaptchaForm> captor = ArgumentCaptor.forClass(CaptchaForm.class);
        verify(captchaService).checkCaptcha(captor.capture());
        assertThat(captor.getValue().getCaptchaCode()).isEqualTo("1234");
    }
}
