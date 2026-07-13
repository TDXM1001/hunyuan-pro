package com.hunyuan.sa.bpm.module.integration.domain.model;

public record BpmSourceApplicationPrincipal(Long applicationId, String sourceSystemCode,
                                            String applicationCode, String scopes, String status) {
    public boolean hasScope(String scope) {
        return scopes != null && java.util.Arrays.asList(scopes.split("\\s+")).contains(scope);
    }
}
