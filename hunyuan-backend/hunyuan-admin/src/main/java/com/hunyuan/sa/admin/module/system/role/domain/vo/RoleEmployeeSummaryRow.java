package com.hunyuan.sa.admin.module.system.role.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色成员查询使用的员工摘要持久化投影。
 */
@Data
public class RoleEmployeeSummaryRow {

    private Long employeeId;

    private String loginName;

    private String actualName;

    private String avatar;

    private Integer gender;

    private String phone;

    private String email;

    private Long departmentId;

    private Long positionId;

    private Boolean disabled;

    private LocalDateTime createTime;
}
