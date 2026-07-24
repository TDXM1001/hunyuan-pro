package com.hunyuan.sa.admin.module.system.message;

import com.hunyuan.sa.base.common.domain.ValidateList;
import com.hunyuan.sa.base.module.support.message.api.PlatformMessagePageQuery;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 稳定消息管理 HTTP 路由契约，防止管理端回退到历史消息路由。
 */
class PlatformMessageApiContractTest {

    @Test
    void exposesStableMessageManagementRoutes() throws Exception {
        RequestMapping mapping = PlatformMessageController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping.value()).containsExactly("/api/admin/v1/platform/messages");
        assertThat(PlatformMessageController.class
                .getMethod("queryPage", PlatformMessagePageQuery.class)
                .getAnnotation(PostMapping.class).value()).containsExactly("/query");
        assertThat(PlatformMessageController.class
                .getMethod("send", ValidateList.class)
                .getAnnotation(PostMapping.class).value()).isEmpty();
        assertThat(PlatformMessageController.class
                .getMethod("delete", Long.class)
                .getAnnotation(DeleteMapping.class).value()).containsExactly("/{messageId}");
    }
}
