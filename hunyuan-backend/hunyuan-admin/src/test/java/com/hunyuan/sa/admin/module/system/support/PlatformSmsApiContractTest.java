package com.hunyuan.sa.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.module.support.sms.api.PlatformSmsSendLogPageQuery;
import com.hunyuan.sa.base.module.support.sms.api.PlatformSmsTemplateCreateCommand;
import com.hunyuan.sa.base.module.support.sms.api.PlatformSmsTemplateDisabledCommand;
import com.hunyuan.sa.base.module.support.sms.api.PlatformSmsTemplatePageQuery;
import com.hunyuan.sa.base.module.support.sms.api.PlatformSmsTemplateUpdateCommand;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 稳定短信管理 HTTP 路由与权限契约。
 */
class PlatformSmsApiContractTest {

    @Test
    void exposesStableSmsRoutesAndLegacyPermissions() throws Exception {
        RequestMapping mapping = PlatformSmsController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping.value())
                .containsExactly("/api/admin/v1/platform/notifications/sms");

        var query = PlatformSmsController.class
                .getMethod("queryTemplates", PlatformSmsTemplatePageQuery.class);
        assertThat(query.getAnnotation(PostMapping.class).value())
                .containsExactly("/templates/query");
        assertThat(query.getAnnotation(SaCheckPermission.class).value())
                .containsExactly("support:sms:template:query");

        var create = PlatformSmsController.class
                .getMethod("createTemplate", PlatformSmsTemplateCreateCommand.class);
        assertThat(create.getAnnotation(PostMapping.class).value())
                .containsExactly("/templates");
        assertThat(create.getAnnotation(SaCheckPermission.class).value())
                .containsExactly("support:sms:template:add");

        var update = PlatformSmsController.class.getMethod(
                "updateTemplate", String.class, PlatformSmsTemplateUpdateCommand.class);
        assertThat(update.getAnnotation(PutMapping.class).value())
                .containsExactly("/templates/{templateCode}");
        assertThat(update.getAnnotation(SaCheckPermission.class).value())
                .containsExactly("support:sms:template:update");

        var disabled = PlatformSmsController.class.getMethod(
                "updateTemplateDisabled",
                String.class,
                PlatformSmsTemplateDisabledCommand.class);
        assertThat(disabled.getAnnotation(PutMapping.class).value())
                .containsExactly("/templates/{templateCode}/disabled");
        assertThat(disabled.getAnnotation(SaCheckPermission.class).value())
                .containsExactly("support:sms:template:update");

        var sendLogs = PlatformSmsController.class
                .getMethod("querySendLogs", PlatformSmsSendLogPageQuery.class);
        assertThat(sendLogs.getAnnotation(PostMapping.class).value())
                .containsExactly("/send-logs/query");
        assertThat(sendLogs.getAnnotation(SaCheckPermission.class).value())
                .containsExactly("support:sms:sendLog:query");
    }
}
