package com.hunyuan.sa.admin.module.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.admin.module.system.support.AdminProtectController;
import com.hunyuan.sa.admin.module.system.support.AdminSmartJobController;
import com.hunyuan.sa.base.common.domain.ValidateList;
import com.hunyuan.sa.base.module.support.job.api.domain.SmartJobAddForm;
import com.hunyuan.sa.base.module.support.job.api.domain.SmartJobEnabledUpdateForm;
import com.hunyuan.sa.base.module.support.job.api.domain.SmartJobExecuteForm;
import com.hunyuan.sa.base.module.support.job.api.domain.SmartJobLogQueryForm;
import com.hunyuan.sa.base.module.support.job.api.domain.SmartJobQueryForm;
import com.hunyuan.sa.base.module.support.job.api.domain.SmartJobUpdateForm;
import com.hunyuan.sa.base.module.support.securityprotect.domain.Level3ProtectConfigForm;
import com.hunyuan.sa.base.module.support.securityprotect.domain.LoginFailQueryForm;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformPermissionContractTest {

    @Test
    void platformMutationAndSecurityEndpointsShouldKeepTheirPermissionGuards() throws Exception {
        assertPermission(AdminProtectController.class, "queryPage",
                "support:protect:loginFail:query", LoginFailQueryForm.class);
        assertPermission(AdminProtectController.class, "batchDelete",
                "support:protect:loginFail:delete", ValidateList.class);
        assertPermission(AdminProtectController.class, "updateConfig",
                "support:protect:level3:update", Level3ProtectConfigForm.class);
        assertPermission(AdminProtectController.class, "getConfig",
                "support:protect:level3:query");

        assertPermission(AdminSmartJobController.class, "execute",
                "support:job:execute", SmartJobExecuteForm.class);
        assertPermission(AdminSmartJobController.class, "queryJobInfo",
                "support:job:query", Integer.class);
        assertPermission(AdminSmartJobController.class, "queryJob",
                "support:job:query", SmartJobQueryForm.class);
        assertPermission(AdminSmartJobController.class, "addJob",
                "support:job:update", SmartJobAddForm.class);
        assertPermission(AdminSmartJobController.class, "updateJob",
                "support:job:update", SmartJobUpdateForm.class);
        assertPermission(AdminSmartJobController.class, "updateJobEnabled",
                "support:job:update", SmartJobEnabledUpdateForm.class);
        assertPermission(AdminSmartJobController.class, "deleteJob",
                "support:job:update", Integer.class);
        assertPermission(AdminSmartJobController.class, "queryJobLog",
                "support:job:log:query", SmartJobLogQueryForm.class);
    }

    private void assertPermission(
            Class<?> controller,
            String methodName,
            String expectedPermission,
            Class<?>... parameterTypes) throws Exception {
        SaCheckPermission annotation = controller
                .getDeclaredMethod(methodName, parameterTypes)
                .getAnnotation(SaCheckPermission.class);
        assertThat(annotation)
                .as(controller.getSimpleName() + "." + methodName)
                .isNotNull();
        assertThat(annotation.value()).containsExactly(expectedPermission);
    }
}
