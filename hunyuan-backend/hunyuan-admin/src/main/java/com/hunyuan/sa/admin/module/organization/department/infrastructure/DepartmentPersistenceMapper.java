package com.hunyuan.sa.admin.module.organization.department.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DepartmentPersistenceMapper extends BaseMapper<DepartmentPersistenceEntity> {

    @Select("SELECT d.department_id, d.department_name, d.manager_id, d.parent_id, d.sort, d.create_time, d.update_time, e.actual_name AS manager_name " +
            "FROM t_department d LEFT JOIN t_employee e ON d.manager_id = e.employee_id " +
            "ORDER BY d.sort DESC, d.department_id ASC")
    List<DepartmentPersistenceEntity> selectDirectory();

    @Select("SELECT COUNT(1) FROM t_department WHERE parent_id = #{departmentId}")
    int countChildren(@Param("departmentId") Long departmentId);
}
