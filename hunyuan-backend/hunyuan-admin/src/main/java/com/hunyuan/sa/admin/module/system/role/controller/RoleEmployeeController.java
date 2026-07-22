package com.hunyuan.sa.admin.module.system.role.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.admin.module.access.role.api.AccessRoleAssignmentFacade;
import com.hunyuan.sa.admin.module.access.role.api.AccessRoleMember;
import com.hunyuan.sa.admin.module.access.role.api.AccessRoleMemberQuery;
import com.hunyuan.sa.admin.module.access.role.api.AccessRoleMembershipFacade;
import com.hunyuan.sa.admin.module.access.role.api.AssignRoleEmployeesCommand;
import com.hunyuan.sa.admin.module.access.role.api.RemoveRoleEmployeesCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.hunyuan.sa.admin.constant.AdminSwaggerTagConst;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeSummary;
import com.hunyuan.sa.admin.module.system.role.domain.form.RoleEmployeeQueryForm;
import com.hunyuan.sa.admin.module.system.role.domain.form.RoleEmployeeUpdateForm;
import com.hunyuan.sa.admin.module.system.role.domain.vo.RoleSelectedVO;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色的员工
 *
 * @Author 1024创新实验室-主任: 卓大
 * @Date 2022-02-26 22:09:59
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright  <a href="https://1024lab.net">1024创新实验室</a>
 */
@RestController
@Tag(name = AdminSwaggerTagConst.System.SYSTEM_ROLE_EMPLOYEE)
public class RoleEmployeeController {

    @Resource
    private AccessRoleMembershipFacade roleMembershipFacade;

    @Resource
    private AccessRoleAssignmentFacade roleAssignmentFacade;

    @Operation(summary = "查询某个角色下的员工列表  @author 卓大")
    @PostMapping("/role/employee/queryEmployee")
    public ResponseDTO<PageResult<EmployeeSummary>> queryEmployee(
            @Valid @RequestBody RoleEmployeeQueryForm roleEmployeeQueryForm) {
        return ResponseDTO.ok(toEmployeePage(
                roleMembershipFacade.queryMembers(toMemberQuery(roleEmployeeQueryForm))));
    }

    @Operation(summary = "查询某个角色可添加的候选员工列表  @author 卓大")
    @PostMapping("/role/employee/queryCandidateEmployee")
    public ResponseDTO<PageResult<EmployeeSummary>> queryCandidateEmployee(
            @Valid @RequestBody RoleEmployeeQueryForm roleEmployeeQueryForm) {
        return ResponseDTO.ok(toEmployeePage(
                roleMembershipFacade.queryCandidates(toMemberQuery(roleEmployeeQueryForm))));
    }

    @Operation(summary = "获取某个角色下的所有员工列表(无分页)  @author 卓大")
    @GetMapping("/role/employee/getAllEmployeeByRoleId/{roleId}")
    public ResponseDTO<List<EmployeeSummary>> listAllEmployeeRoleId(@PathVariable Long roleId) {
        return ResponseDTO.ok(roleMembershipFacade.listMembers(roleId).stream()
                .map(this::toEmployeeSummary)
                .toList());
    }

    @Operation(summary = "从角色成员列表中移除员工 @author 卓大")
    @GetMapping("/role/employee/removeEmployee")
    @SaCheckPermission("system:role:employee:delete")
    public ResponseDTO<String> removeEmployee(Long employeeId, Long roleId) {
        if (employeeId == null || roleId == null) {
            return ResponseDTO.userErrorParam();
        }
        roleAssignmentFacade.removeEmployees(
                new RemoveRoleEmployeesCommand(roleId, java.util.Set.of(employeeId)));
        return ResponseDTO.ok();
    }

    @Operation(summary = "从角色成员列表中批量移除员工 @author 卓大")
    @PostMapping("/role/employee/batchRemoveRoleEmployee")
    @SaCheckPermission("system:role:employee:batch:delete")
    public ResponseDTO<String> batchRemoveEmployee(@Valid @RequestBody RoleEmployeeUpdateForm updateForm) {
        roleAssignmentFacade.removeEmployees(new RemoveRoleEmployeesCommand(
                updateForm.getRoleId(), updateForm.getEmployeeIdList()));
        return ResponseDTO.ok();
    }

    @Operation(summary = "角色成员列表中批量添加员工 @author 卓大")
    @PostMapping("/role/employee/batchAddRoleEmployee")
    @SaCheckPermission("system:role:employee:add")
    public ResponseDTO<String> addEmployeeList(@Valid @RequestBody RoleEmployeeUpdateForm addForm) {
        roleAssignmentFacade.assignEmployees(new AssignRoleEmployeesCommand(
                addForm.getRoleId(), addForm.getEmployeeIdList()));
        return ResponseDTO.ok();
    }

    @Operation(summary = "获取员工所有选中的角色和所有角色 @author 卓大")
    @GetMapping("/role/employee/getRoles/{employeeId}")
    public ResponseDTO<List<RoleSelectedVO>> getRoleByEmployeeId(@PathVariable Long employeeId) {
        List<RoleSelectedVO> roles = roleMembershipFacade.listEmployeeRoleSelections(employeeId)
                .stream()
                .map(selection -> {
                    RoleSelectedVO role = new RoleSelectedVO();
                    role.setRoleId(selection.roleId());
                    role.setRoleName(selection.roleName());
                    role.setRoleCode(selection.roleCode());
                    role.setRemark(selection.remark());
                    role.setSelected(selection.selected());
                    return role;
                })
                .toList();
        return ResponseDTO.ok(roles);
    }

    private AccessRoleMemberQuery toMemberQuery(RoleEmployeeQueryForm form) {
        AccessRoleMemberQuery query = new AccessRoleMemberQuery();
        query.setPageNum(form.getPageNum());
        query.setPageSize(form.getPageSize());
        query.setSearchCount(form.getSearchCount());
        query.setSortItemList(form.getSortItemList());
        query.setKeywords(form.getKeywords());
        query.setRoleId(form.getRoleId());
        return query;
    }

    private PageResult<EmployeeSummary> toEmployeePage(PageResult<AccessRoleMember> source) {
        PageResult<EmployeeSummary> result = new PageResult<>();
        result.setPageNum(source.getPageNum());
        result.setPageSize(source.getPageSize());
        result.setTotal(source.getTotal());
        result.setPages(source.getPages());
        result.setList(source.getList().stream().map(this::toEmployeeSummary).toList());
        result.setEmptyFlag(source.getEmptyFlag());
        return result;
    }

    private EmployeeSummary toEmployeeSummary(AccessRoleMember member) {
        return new EmployeeSummary(
                member.employeeId(),
                member.loginName(),
                member.actualName(),
                member.avatar(),
                member.gender(),
                member.phone(),
                member.email(),
                member.departmentId(),
                member.departmentName(),
                member.positionId(),
                member.disabled(),
                member.createTime());
    }
}
