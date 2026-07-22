package com.hunyuan.sa.admin.module.system.employee.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import com.hunyuan.sa.admin.module.system.employee.domain.vo.EmployeeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface EmployeeDao extends BaseMapper<EmployeeEntity> {

    EmployeeEntity getByLoginName(
            @Param("loginName") String loginName,
            @Param("deletedFlag") Boolean deletedFlag);

    EmployeeEntity getByActualName(
            @Param("actualName") String actualName,
            @Param("deletedFlag") Boolean deletedFlag);

    EmployeeEntity getByPhone(
            @Param("phone") String phone,
            @Param("deletedFlag") Boolean deletedFlag);

    EmployeeEntity getByEmail(
            @Param("email") String email,
            @Param("deletedFlag") Boolean deletedFlag);

    List<EmployeeVO> listAll();

    Integer countByDepartmentId(
            @Param("departmentId") Long departmentId,
            @Param("deletedFlag") Boolean deletedFlag);

    List<EmployeeVO> getEmployeeByIds(@Param("employeeIds") Collection<Long> employeeIds);

    EmployeeVO getEmployeeById(@Param("employeeId") Long employeeId);

    List<EmployeeEntity> selectByActualName(
            @Param("departmentIdList") List<Long> departmentIdList,
            @Param("actualName") String actualName,
            @Param("deletedFlag") Boolean deletedFlag);

    List<Long> getEmployeeIdByDepartmentIdList(
            @Param("departmentIds") List<Long> departmentIds,
            @Param("deletedFlag") Boolean deletedFlag);

    List<Long> getEmployeeId(
            @Param("leaveFlag") Boolean leaveFlag,
            @Param("deletedFlag") Boolean deletedFlag);

    List<Long> getEmployeeIdByDepartmentId(
            @Param("departmentId") Long departmentId,
            @Param("deletedFlag") Boolean deletedFlag);

    Integer updatePassword(
            @Param("employeeId") Long employeeId,
            @Param("password") String password);
}
