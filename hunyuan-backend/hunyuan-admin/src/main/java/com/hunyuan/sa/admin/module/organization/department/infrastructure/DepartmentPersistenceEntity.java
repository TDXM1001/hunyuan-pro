package com.hunyuan.sa.admin.module.organization.department.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_department")
public class DepartmentPersistenceEntity {

    @TableId(value = "department_id", type = IdType.AUTO)
    private Long departmentId;
    private String departmentName;
    private Long managerId;
    @TableField(exist = false)
    private String managerName;
    private Long parentId;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
