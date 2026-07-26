package com.hunyuan.sa.admin.module.system.login.service;

import com.hunyuan.sa.base.module.support.captcha.api.PlatformCaptchaFacade;
import com.hunyuan.sa.base.module.support.securityprotect.api.PlatformLoginSecurityFacade;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定登录服务的 Facade 注入名称，避免 @Resource 按名误命中平台模块内部 Bean。
 */
class LoginServicePlatformFacadeResourceContractTest {

    @Test
    void platformFacadeResourcesUseBoundaryNames() throws Exception {
        assertResourceField("platformCaptchaFacade", PlatformCaptchaFacade.class);
        assertResourceField("platformLoginSecurityFacade", PlatformLoginSecurityFacade.class);
    }

    private static void assertResourceField(String fieldName, Class<?> fieldType) throws Exception {
        Field field = LoginService.class.getDeclaredField(fieldName);
        assertThat(field.getType()).isEqualTo(fieldType);
        assertThat(field.getAnnotation(Resource.class)).isNotNull();
    }
}
