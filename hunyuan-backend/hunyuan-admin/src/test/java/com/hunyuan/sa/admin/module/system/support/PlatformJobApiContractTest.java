package com.hunyuan.sa.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobCreateCommand;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobEnabledCommand;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobExecuteCommand;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobLogPageQuery;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobPageQuery;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobUpdateCommand;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定平台定时任务稳定路由和历史权限映射。
 */
class PlatformJobApiContractTest {

    @Test
    void exposesStableRuntimeRoutesAndLegacyPermissions() throws Exception {
        RequestMapping mapping = PlatformJobController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping.value()).containsExactly("/api/admin/v1/platform/runtime/jobs");

        assertRoute("getJob", GetMapping.class, "/{jobId}",
                "support:job:query", Integer.class);
        assertRoute("queryJobs", PostMapping.class, "/query",
                "support:job:query", PlatformJobPageQuery.class);
        assertRoute("queryLogs", PostMapping.class, "/logs/query",
                "support:job:log:query", PlatformJobLogPageQuery.class);
        assertRoute("createJob", PostMapping.class, "",
                "support:job:update", PlatformJobCreateCommand.class);
        assertRoute("updateJob", PutMapping.class, "",
                "support:job:update", PlatformJobUpdateCommand.class);
        assertRoute("updateEnabled", PutMapping.class, "/enabled",
                "support:job:update", PlatformJobEnabledCommand.class);
        assertRoute("executeJob", PostMapping.class, "/execute",
                "support:job:execute", PlatformJobExecuteCommand.class);
        assertRoute("deleteJob", DeleteMapping.class, "/{jobId}",
                "support:job:update", Integer.class);
    }

    private void assertRoute(
            String methodName,
            Class<? extends java.lang.annotation.Annotation> mappingType,
            String expectedPath,
            String permission,
            Class<?> parameterType) throws Exception {
        var method = PlatformJobController.class.getMethod(methodName, parameterType);
        Annotation annotation = method.getAnnotation(mappingType);
        String[] path;
        if (annotation instanceof GetMapping value) {
            path = value.value();
        } else if (annotation instanceof PostMapping value) {
            path = value.value();
        } else if (annotation instanceof PutMapping value) {
            path = value.value();
        } else {
            path = ((DeleteMapping) annotation).value();
        }
        if (expectedPath.isEmpty()) {
            assertThat(path).isEmpty();
        } else {
            assertThat(path).containsExactly(expectedPath);
        }
        assertThat(method.getAnnotation(SaCheckPermission.class).value())
                .containsExactly(permission);
    }
}
