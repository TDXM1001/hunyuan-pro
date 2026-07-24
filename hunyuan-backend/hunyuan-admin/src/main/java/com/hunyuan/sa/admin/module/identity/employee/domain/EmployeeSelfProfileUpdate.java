package com.hunyuan.sa.admin.module.identity.employee.domain;

/**
 * 个人资料写入模型，隔离公开命令与持久化对象。
 */
public record EmployeeSelfProfileUpdate(
        Long employeeId,
        String actualName,
        Integer gender,
        String phone,
        String email,
        Long positionId,
        String avatar,
        String remark
) {
}
