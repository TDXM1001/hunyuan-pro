package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.annoation.NoNeedLogin;
import com.hunyuan.sa.bpm.controller.app.AppBpmExternalCallbackController;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BpmExternalCallbackControllerContractTest {

    @Test
    void callbackUsesProjectAnonymousAccessContract() throws NoSuchMethodException {
        var method = AppBpmExternalCallbackController.class.getDeclaredMethod(
                "callback",
                String.class,
                String.class,
                String.class
        );

        assertThat(method.isAnnotationPresent(NoNeedLogin.class)).isTrue();
    }
}
