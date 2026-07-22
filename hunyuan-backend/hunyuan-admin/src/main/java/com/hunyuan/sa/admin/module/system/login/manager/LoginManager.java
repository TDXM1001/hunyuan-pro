package com.hunyuan.sa.admin.module.system.login.manager;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import com.hunyuan.sa.admin.constant.AdminCacheConst;
import com.hunyuan.sa.admin.module.access.authorization.api.AccessAuthorizationFacade;
import com.hunyuan.sa.admin.module.access.authorization.api.AccessAuthorizationSnapshot;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeAuthenticationAccount;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeDirectoryFacade;
import com.hunyuan.sa.admin.module.organization.department.application.OrganizationDepartmentFacade;
import com.hunyuan.sa.admin.module.organization.department.domain.Department;
import com.hunyuan.sa.admin.module.system.login.domain.RequestEmployee;
import com.hunyuan.sa.base.common.constant.StringConst;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.domain.UserPermission;
import com.hunyuan.sa.base.common.enumeration.UserTypeEnum;
import com.hunyuan.sa.base.module.support.file.service.IFileStorageService;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * 登录Manager
 *
 * @Author 1024创新实验室: 卓大
 * @Date 2025-05-03 22:56:34
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@Service
public class LoginManager {

    @Resource
    private OrganizationDepartmentFacade organizationDepartmentFacade;

    @Resource
    private IFileStorageService fileStorageService;

    @Resource
    private EmployeeDirectoryFacade employeeDirectoryFacade;

    @Resource
    private AccessAuthorizationFacade accessAuthorizationFacade;


    /**
     * 获取请求用户信息
     */
    @Cacheable(AdminCacheConst.Login.REQUEST_EMPLOYEE)
    public RequestEmployee getRequestEmployee(Long requestEmployeeId ) {
        if (requestEmployeeId == null) {
            return null;
        }
        // 员工基本信息
        EmployeeAuthenticationAccount employee = employeeDirectoryFacade
                .findAuthenticationAccountById(requestEmployeeId)
                .orElse(null);
        if (employee == null) {
            return null;
        }

        return this.loadLoginInfo(employee);
    }

    /**
     * 获取登录的用户信息
     */
    @CachePut(value = AdminCacheConst.Login.REQUEST_EMPLOYEE, key = "#employee.employeeId")
    public RequestEmployee loadLoginInfo(EmployeeAuthenticationAccount employee) {
        RequestEmployee requestEmployee = new RequestEmployee();
        requestEmployee.setEmployeeId(employee.employeeId());
        requestEmployee.setLoginName(employee.loginName());
        requestEmployee.setActualName(employee.actualName());
        requestEmployee.setAvatar(employee.avatar());
        requestEmployee.setGender(employee.gender());
        requestEmployee.setPhone(employee.phone());
        requestEmployee.setEmail(employee.email());
        requestEmployee.setDepartmentId(employee.departmentId());
        requestEmployee.setPositionId(employee.positionId());
        requestEmployee.setDisabledFlag(employee.disabled());
        requestEmployee.setAdministratorFlag(employee.administrator());
        requestEmployee.setRemark(employee.remark());
        requestEmployee.setUserType(UserTypeEnum.ADMIN_EMPLOYEE);

        // 部门信息
        Department department = organizationDepartmentFacade.findForCollaboration(employee.departmentId()).orElse(null);
        requestEmployee.setDepartmentName(null == department ? StringConst.EMPTY : department.departmentName());

        // 头像信息
        String avatar = employee.avatar();
        if (StringUtils.isNotBlank(avatar)) {
            ResponseDTO<String> getFileUrl = fileStorageService.getFileUrl(avatar);
            if (BooleanUtils.isTrue(getFileUrl.getOk())) {
                requestEmployee.setAvatar(getFileUrl.getData());
            }
        }
        return requestEmployee;
    }


    /**
     * 获取用户的权限（包含 角色列表、权限列表）
     */
    @Cacheable(AdminCacheConst.Login.USER_PERMISSION)
    public UserPermission getUserPermission(Long employeeId) {
        if(null == employeeId){
            return null;
        }

        return this.loadUserPermission(employeeId);
    }

    /**
     * 获取用户的权限（包含 角色列表、权限列表）
     */
    @CachePut(AdminCacheConst.Login.USER_PERMISSION)
    public UserPermission loadUserPermission(Long employeeId) {
        UserPermission userPermission = new UserPermission();
        userPermission.setPermissionList(new ArrayList<>());
        userPermission.setRoleList(new ArrayList<>());

        EmployeeAuthenticationAccount employee = employeeDirectoryFacade
                .findAuthenticationAccountById(employeeId)
                .orElse(null);
        if (employee == null) {
            return userPermission;
        }

        AccessAuthorizationSnapshot authorization = accessAuthorizationFacade.loadEmployeeAuthorization(
                employeeId, employee.administrator());
        userPermission.getRoleList().addAll(authorization.roleCodes());
        userPermission.getPermissionList().addAll(authorization.capabilityCodes());

        return userPermission;
    }


    /**
     * 清除用户权限
     */
    @CacheEvict(value = AdminCacheConst.Login.USER_PERMISSION)
    public void clearUserPermission(Long employeeId) {

    }

    /**
     * 清除用户登录信息
     */
    @CacheEvict(value = AdminCacheConst.Login.REQUEST_EMPLOYEE)
    public void clearUserLoginInfo(Long employeeId) {

    }


}
