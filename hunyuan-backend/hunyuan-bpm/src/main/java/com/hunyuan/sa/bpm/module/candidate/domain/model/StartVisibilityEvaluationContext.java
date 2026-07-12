package com.hunyuan.sa.bpm.module.candidate.domain.model;

import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;

import java.util.Set;

/**
 * 发起和实例可见性判断只接受服务端已经确认的当前身份与实例参与事实。
 */
public record StartVisibilityEvaluationContext(
        Long tenantId,
        BpmEmployeeSnapshot actor,
        Set<Long> participantEmployeeIds,
        boolean explicitlyAuthorized
) {

    public StartVisibilityEvaluationContext {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("租户不能为空");
        }
        if (actor == null || actor.employeeId() == null || actor.employeeId() <= 0) {
            throw new IllegalArgumentException("当前员工快照不能为空");
        }
        participantEmployeeIds = participantEmployeeIds == null ? Set.of() : Set.copyOf(participantEmployeeIds);
    }
}
