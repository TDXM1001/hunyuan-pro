package com.hunyuan.sa.bpm.module.approvaldata.domain.model;

import java.util.Map;

public record RoutingDataSnapshot(
        Long version,
        Map<String, Object> facts
) {
}
