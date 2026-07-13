package com.hunyuan.sa.bpm.integration;
import cn.dev33.satoken.annotation.SaIgnore; import com.hunyuan.sa.base.common.annoation.NoNeedLogin; import com.hunyuan.sa.bpm.controller.open.OpenBpmProcessController; import org.junit.jupiter.api.Test; import java.util.Arrays; import static org.assertj.core.api.Assertions.assertThat;
class BpmM6OpenControllerContractTest {
 @Test void everyOpenProtocolMethodMustBypassSessionLoginAndUseApplicationAuthentication(){for(String name:new String[]{"start","task","act"}){var methods=Arrays.stream(OpenBpmProcessController.class.getDeclaredMethods()).filter(m->m.getName().equals(name)).toList();assertThat(methods).hasSize(1);assertThat(methods.get(0).isAnnotationPresent(SaIgnore.class)).isTrue();assertThat(methods.get(0).isAnnotationPresent(NoNeedLogin.class)).isTrue();}}
}
