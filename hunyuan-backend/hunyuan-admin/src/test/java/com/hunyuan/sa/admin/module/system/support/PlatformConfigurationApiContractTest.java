package com.hunyuan.sa.admin.module.system.support;

import com.hunyuan.sa.base.module.support.config.api.PlatformConfigurationCreateCommand;
import com.hunyuan.sa.base.module.support.config.api.PlatformConfigurationPageQuery;
import com.hunyuan.sa.base.module.support.config.api.PlatformConfigurationUpdateCommand;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 稳定配置 HTTP 路由契约，防止管理读取回退到历史支持路由。
 */
class PlatformConfigurationApiContractTest {

    @Test
    void exposesStableConfigurationQueryRoute() throws Exception {
        RequestMapping mapping = PlatformConfigurationController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping.value()).containsExactly("/api/admin/v1/platform/configurations");
        assertThat(PlatformConfigurationController.class
                .getMethod("queryPage", PlatformConfigurationPageQuery.class)
                .getAnnotation(PostMapping.class).value()).containsExactly("/query");
        assertThat(PlatformConfigurationController.class
                .getMethod("create", PlatformConfigurationCreateCommand.class)
                .getAnnotation(PostMapping.class).value()).isEmpty();
        assertThat(PlatformConfigurationController.class
                .getMethod("update", Long.class, PlatformConfigurationUpdateCommand.class)
                .getAnnotation(PutMapping.class).value()).containsExactly("/{configurationId}");
    }
}
