package com.hunyuan.sa.admin.module.identity.employee.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_employee")
public class EmployeePersistenceEntity {

    @TableId(value = "employee_id", type = IdType.AUTO)
    private Long employeeId;
    private String employeeUid;
    private String loginName;
    private String loginPwd;
    private String actualName;
    private String avatar;
    private Integer gender;
    private String phone;
    private String email;
    private Long departmentId;
    private Long positionId;
    private Boolean administratorFlag;
    private Boolean disabledFlag;
    private Boolean deletedFlag;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
