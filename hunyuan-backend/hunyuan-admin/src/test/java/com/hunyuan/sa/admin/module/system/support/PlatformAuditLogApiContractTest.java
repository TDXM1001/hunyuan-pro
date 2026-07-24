package com.hunyuan.sa.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.module.support.audit.api.PlatformLoginLogPageQuery;
import com.hunyuan.sa.base.module.support.audit.api.PlatformOperateLogPageQuery;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 稳定审计日志 HTTP 路由与权限契约。
 */
class PlatformAuditLogApiContractTest {

    @Test
    void exposesStableAuditLogRoutesAndLegacyPermissions() throws Exception {
        RequestMapping mapping = PlatformAuditLogController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping.value()).containsExactly("/api/admin/v1/platform/audit");

        var operateQuery = PlatformAuditLogController.class
                .getMethod("queryOperateLogs", PlatformOperateLogPageQuery.class);
        assertThat(operateQuery.getAnnotation(PostMapping.class).value())
                .containsExactly("/operation-logs/query");
        assertThat(operateQuery.getAnnotation(SaCheckPermission.class).value())
                .containsExactly("support:operateLog:query");

        var operateDetail = PlatformAuditLogController.class
                .getMethod("getOperateLog", Long.class);
        assertThat(operateDetail.getAnnotation(GetMapping.class).value())
                .containsExactly("/operation-logs/{operateLogId}");
        assertThat(operateDetail.getAnnotation(SaCheckPermission.class).value())
                .containsExactly("support:operateLog:detail");

        var loginQuery = PlatformAuditLogController.class
                .getMethod("queryLoginLogs", PlatformLoginLogPageQuery.class);
        assertThat(loginQuery.getAnnotation(PostMapping.class).value())
                .containsExactly("/login-logs/query");
        assertThat(loginQuery.getAnnotation(SaCheckPermission.class).value())
                .containsExactly("support:loginLog:query");
    }
}
