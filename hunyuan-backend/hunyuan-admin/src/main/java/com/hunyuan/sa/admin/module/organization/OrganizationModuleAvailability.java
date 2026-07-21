package com.hunyuan.sa.admin.module.organization;

import com.hunyuan.sa.base.module.support.config.ConfigService;
import com.hunyuan.sa.base.module.support.config.domain.ConfigVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class OrganizationModuleAvailability {

    private static final String CONFIG_KEY = "module.organization.directory.enabled";

    @Resource
    private ConfigService configService;

    public void requireEnabled() {
        String value = value();
        if (value != null && !Boolean.parseBoolean(value.trim())) {
            throw new OrganizationBusinessException(OrganizationErrorCode.MODULE_DISABLED);
        }
    }

    public boolean enabled() {
        String value = value();
        return value == null || Boolean.parseBoolean(value.trim());
    }

    private String value() {
        ConfigVO config = configService.getConfig(CONFIG_KEY);
        return config == null ? null : config.getConfigValue();
    }
}
