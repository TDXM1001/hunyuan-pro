package com.hunyuan.sa.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.module.support.serialnumber.api.PlatformSerialNumberGenerateCommand;
import com.hunyuan.sa.base.module.support.serialnumber.api.PlatformSerialNumberRecordPageQuery;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 稳定序列号 HTTP 路由与权限契约。
 */
class PlatformSerialNumberApiContractTest {

    @Test
    void exposesStableRuntimeRoutesAndLegacyPermissions() throws Exception {
        RequestMapping mapping = PlatformSerialNumberController.class
                .getAnnotation(RequestMapping.class);
        assertThat(mapping.value())
                .containsExactly("/api/admin/v1/platform/runtime/serial-numbers");

        var list = PlatformSerialNumberController.class.getMethod("listDefinitions");
        assertThat(list.getAnnotation(GetMapping.class).value()).isEmpty();
        assertThat(list.getAnnotation(SaCheckPermission.class)).isNull();

        var records = PlatformSerialNumberController.class.getMethod(
                "queryRecords", PlatformSerialNumberRecordPageQuery.class);
        assertThat(records.getAnnotation(PostMapping.class).value())
                .containsExactly("/records/query");
        assertThat(records.getAnnotation(SaCheckPermission.class).value())
                .containsExactly("support:serialNumber:record");

        var generate = PlatformSerialNumberController.class.getMethod(
                "generate", PlatformSerialNumberGenerateCommand.class);
        assertThat(generate.getAnnotation(PostMapping.class).value())
                .containsExactly("/generate");
        assertThat(generate.getAnnotation(SaCheckPermission.class).value())
                .containsExactly("support:serialNumber:generate");
    }
}
