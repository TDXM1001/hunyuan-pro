package com.hunyuan.sa.admin.module.access.authorization.api;

import java.util.List;
import java.util.Set;

public record AccessAuthorizationSnapshot(
        Set<String> roleCodes,
        Set<String> capabilityCodes,
        List<AccessMenuItem> menuItems) {

    public AccessAuthorizationSnapshot {
        roleCodes = roleCodes == null ? Set.of() : Set.copyOf(roleCodes);
        capabilityCodes = capabilityCodes == null ? Set.of() : Set.copyOf(capabilityCodes);
        menuItems = menuItems == null ? List.of() : List.copyOf(menuItems);
    }
}
