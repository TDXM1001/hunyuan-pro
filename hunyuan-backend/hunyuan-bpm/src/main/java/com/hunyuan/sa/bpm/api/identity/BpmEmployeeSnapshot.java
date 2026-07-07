package com.hunyuan.sa.bpm.api.identity;

/**
 * BPM 运行期使用的员工快照。
 */
public record BpmEmployeeSnapshot(
        Long employeeId,
        String actualName,
        Long departmentId,
        String departmentName,
        String phone,
        String email
) {
}
