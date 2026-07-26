package com.hunyuan.sa.admin.module.organization;

import com.hunyuan.sa.base.module.support.config.api.PlatformConfigurationValueReader;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class OrganizationModuleAvailability {

    private static final String CONFIG_KEY = "module.organization.directory.enabled";

    @Resource
    private PlatformConfigurationValueReader configurationValueReader;

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
        return configurationValueReader.getValue(CONFIG_KEY);
    }
}
