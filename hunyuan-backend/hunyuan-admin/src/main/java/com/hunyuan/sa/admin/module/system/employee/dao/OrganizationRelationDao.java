package com.hunyuan.sa.admin.module.system.employee.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 组织域中用户组和员工汇报关系的受控只读入口。
 */
@Mapper
public interface OrganizationRelationDao {

    @Select("""
            SELECT employee.employee_id
            FROM t_user_group_employee relation
            JOIN t_user_group user_group ON user_group.user_group_id = relation.user_group_id
            JOIN t_employee employee ON employee.employee_id = relation.employee_id
            WHERE relation.user_group_id = #{userGroupId}
              AND user_group.disabled_flag = 0
              AND user_group.deleted_flag = 0
              AND employee.disabled_flag = 0
              AND employee.deleted_flag = 0
            ORDER BY employee.employee_id
            """)
    List<Long> listActiveEmployeeIdsByUserGroupId(@Param("userGroupId") Long userGroupId);

    @Select("""
            SELECT manager_employee_id
            FROM t_employee_reporting_relation
            WHERE employee_id = #{employeeId}
              AND disabled_flag = 0
            """)
    Long selectActiveManagerEmployeeId(@Param("employeeId") Long employeeId);
}
