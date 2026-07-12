package com.hunyuan.sa.bpm.module.candidate.domain.model;

import java.util.Map;
import java.util.Set;

/**
 * M3 传入的已脱敏、可用于候选解析的人员路由事实。
 */
public record RoutingFactView(
        String businessContractVersion,
        String routeFactVersion,
        Set<String> allowedEmployeeFactKeys,
        Map<String, Long> employeeFacts
) {

    public RoutingFactView {
        allowedEmployeeFactKeys = Set.copyOf(allowedEmployeeFactKeys);
        employeeFacts = Map.copyOf(employeeFacts);
    }

    public Long requireEmployeeFact(String factKey) {
        if (factKey == null || !allowedEmployeeFactKeys.contains(factKey)) {
            throw new IllegalArgumentException("候选策略引用了未授权的路由事实：" + factKey);
        }
        Long employeeId = employeeFacts.get(factKey);
        if (employeeId == null || employeeId <= 0) {
            throw new IllegalArgumentException("候选策略缺少有效的人员路由事实：" + factKey);
        }
        return employeeId;
    }
}
