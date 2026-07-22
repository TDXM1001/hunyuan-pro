package com.hunyuan.sa.admin.module.identity.employee.application;

import cn.hutool.core.lang.UUID;
import com.hunyuan.sa.admin.module.access.role.api.AccessRoleAssignmentFacade;
import com.hunyuan.sa.admin.module.access.role.api.ReplaceEmployeeRolesCommand;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeAdministrationFacade;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeAuthenticationAccount;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeCreateCommand;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeDeleteCommand;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeDepartmentAssignmentCommand;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeOneTimeCredential;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeePasswordSalt;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeUpdateCommand;
import com.hunyuan.sa.admin.module.identity.employee.application.port.EmployeeSessionPort;
import com.hunyuan.sa.admin.module.identity.employee.domain.EmployeeCreateDraft;
import com.hunyuan.sa.admin.module.identity.employee.domain.EmployeeProfileUpdate;
import com.hunyuan.sa.admin.module.identity.employee.domain.EmployeeRepository;
import com.hunyuan.sa.admin.module.organization.department.application.OrganizationDepartmentFacade;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.securityprotect.service.SecurityPasswordService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class EmployeeAdministrationApplicationService implements EmployeeAdministrationFacade {

    @Resource
    private EmployeeRepository employeeRepository;

    @Resource
    private OrganizationDepartmentFacade organizationDepartmentFacade;

    @Resource
    private SecurityPasswordService securityPasswordService;

    @Resource
    private AccessRoleAssignmentFacade accessRoleAssignmentFacade;

    @Resource
    private EmployeeSessionPort employeeSessionPort;

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public synchronized ResponseDTO<EmployeeOneTimeCredential> create(EmployeeCreateCommand command) {
        return createInternal(command, null);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public synchronized ResponseDTO<EmployeeOneTimeCredential> createWithLegacyRoles(
            EmployeeCreateCommand command, List<Long> roleIds) {
        return createInternal(command, normalizeIds(roleIds));
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public synchronized ResponseDTO<String> update(EmployeeUpdateCommand command) {
        return updateInternal(command, null, null);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public synchronized ResponseDTO<String> updateWithLegacyRoles(
            EmployeeUpdateCommand command, Boolean disabled, List<Long> roleIds) {
        return updateInternal(command, disabled, normalizeIds(roleIds));
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<String> enable(Long employeeId) {
        return setDisabled(employeeId, false);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<String> disable(Long employeeId) {
        return setDisabled(employeeId, true);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<String> toggleDisabledForLegacy(Long employeeId) {
        Optional<EmployeeAuthenticationAccount> account = activeAccount(employeeId);
        if (account.isEmpty()) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        return setDisabled(account.get(), !Boolean.TRUE.equals(account.get().disabled()));
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<String> assignDepartment(EmployeeDepartmentAssignmentCommand command) {
        if (organizationDepartmentFacade.findForCollaboration(command.departmentId()).isEmpty()) {
            return ResponseDTO.userErrorParam("部门不存在");
        }

        List<Long> employeeIds = normalizeIds(command.employeeIds());
        List<EmployeeAuthenticationAccount> accounts =
                employeeRepository.findAuthenticationAccountsByIds(employeeIds).stream()
                        .filter(account -> !Boolean.TRUE.equals(account.deleted()))
                        .toList();
        if (accounts.size() != employeeIds.size()) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        employeeRepository.assignDepartment(employeeIds, command.departmentId());
        employeeIds.forEach(employeeSessionPort::clearCache);
        return ResponseDTO.ok();
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<String> delete(EmployeeDeleteCommand command) {
        List<Long> requestedIds = normalizeIds(command.employeeIds());
        List<EmployeeAuthenticationAccount> accounts =
                employeeRepository.findAuthenticationAccountsByIds(requestedIds).stream()
                        .filter(account -> !Boolean.TRUE.equals(account.deleted()))
                        .toList();
        if (accounts.isEmpty()) {
            return ResponseDTO.ok();
        }
        if (accounts.stream().anyMatch(account -> Boolean.TRUE.equals(account.administrator()))) {
            return ResponseDTO.userErrorParam("超级管理员不能通过员工管理删除");
        }

        List<Long> existingIds = accounts.stream().map(EmployeeAuthenticationAccount::employeeId).toList();
        employeeRepository.markDeleted(existingIds);
        existingIds.forEach(employeeSessionPort::clearCache);
        existingIds.forEach(employeeSessionPort::logout);
        return ResponseDTO.ok();
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<EmployeeOneTimeCredential> resetPassword(
            Long employeeId, Long operatorEmployeeId) {
        Optional<EmployeeAuthenticationAccount> target = activeAccount(employeeId);
        if (target.isEmpty()) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        if (Boolean.TRUE.equals(target.get().administrator())
                && !isAnotherActiveAdministrator(operatorEmployeeId, employeeId)) {
            return ResponseDTO.userErrorParam("超级管理员密码只能由另一名已认证超级管理员重置");
        }

        String password = securityPasswordService.randomPassword();
        String passwordHash = SecurityPasswordService.getEncryptPwd(
                EmployeePasswordSalt.apply(password, target.get().employeeUid()));
        employeeRepository.updatePassword(employeeId, passwordHash);
        employeeSessionPort.clearCache(employeeId);
        employeeSessionPort.logout(employeeId);
        return ResponseDTO.ok(new EmployeeOneTimeCredential(employeeId, password));
    }

    private ResponseDTO<EmployeeOneTimeCredential> createInternal(
            EmployeeCreateCommand command, List<Long> legacyRoleIds) {
        ResponseDTO<String> uniqueness = validateUniqueness(
                null, command.loginName(), command.phone(), command.email());
        if (uniqueness != null) {
            return ResponseDTO.error(uniqueness);
        }
        if (organizationDepartmentFacade.findForCollaboration(command.departmentId()).isEmpty()) {
            return ResponseDTO.userErrorParam("部门不存在");
        }

        String employeeUid = UUID.randomUUID(true).toString(true);
        String password = securityPasswordService.randomPassword();
        String passwordHash = SecurityPasswordService.getEncryptPwd(
                EmployeePasswordSalt.apply(password, employeeUid));
        Long employeeId = employeeRepository.create(new EmployeeCreateDraft(
                employeeUid,
                command.loginName(),
                passwordHash,
                command.actualName(),
                command.gender(),
                command.phone(),
                command.email(),
                command.departmentId(),
                command.positionId(),
                command.disabled(),
                command.remark()
        ));
        if (legacyRoleIds != null) {
            accessRoleAssignmentFacade.replaceEmployeeRoles(
                    new ReplaceEmployeeRolesCommand(employeeId, Set.copyOf(legacyRoleIds)));
        }
        return ResponseDTO.ok(new EmployeeOneTimeCredential(employeeId, password));
    }

    private ResponseDTO<String> updateInternal(
            EmployeeUpdateCommand command, Boolean legacyDisabled, List<Long> legacyRoleIds) {
        Optional<EmployeeAuthenticationAccount> account = activeAccount(command.employeeId());
        if (account.isEmpty()) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        if (organizationDepartmentFacade.findForCollaboration(command.departmentId()).isEmpty()) {
            return ResponseDTO.userErrorParam("部门不存在");
        }

        ResponseDTO<String> uniqueness = validateUniqueness(
                command.employeeId(), command.loginName(), command.phone(), command.email());
        if (uniqueness != null) {
            return uniqueness;
        }
        if (Boolean.TRUE.equals(account.get().administrator()) && Boolean.TRUE.equals(legacyDisabled)) {
            return ResponseDTO.userErrorParam("超级管理员不能通过员工管理禁用");
        }

        employeeRepository.updateProfile(new EmployeeProfileUpdate(
                command.employeeId(),
                command.loginName(),
                command.actualName(),
                command.gender(),
                command.phone(),
                command.email(),
                command.departmentId(),
                command.positionId(),
                command.remark()
        ));
        if (legacyRoleIds != null) {
            accessRoleAssignmentFacade.replaceEmployeeRoles(
                    new ReplaceEmployeeRolesCommand(command.employeeId(), Set.copyOf(legacyRoleIds)));
        }
        if (legacyDisabled != null
                && !Objects.equals(Boolean.TRUE.equals(account.get().disabled()), legacyDisabled)) {
            employeeRepository.updateDisabled(command.employeeId(), legacyDisabled);
            if (Boolean.TRUE.equals(legacyDisabled)) {
                employeeSessionPort.logout(command.employeeId());
            }
        }
        employeeSessionPort.clearCache(command.employeeId());
        return ResponseDTO.ok();
    }

    private ResponseDTO<String> setDisabled(Long employeeId, boolean disabled) {
        Optional<EmployeeAuthenticationAccount> account = activeAccount(employeeId);
        if (account.isEmpty()) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        return setDisabled(account.get(), disabled);
    }

    private ResponseDTO<String> setDisabled(EmployeeAuthenticationAccount account, boolean disabled) {
        if (disabled && Boolean.TRUE.equals(account.administrator())) {
            return ResponseDTO.userErrorParam("超级管理员不能通过员工管理禁用");
        }
        if (Objects.equals(Boolean.TRUE.equals(account.disabled()), disabled)) {
            return ResponseDTO.ok();
        }

        employeeRepository.updateDisabled(account.employeeId(), disabled);
        employeeSessionPort.clearCache(account.employeeId());
        if (disabled) {
            employeeSessionPort.logout(account.employeeId());
        }
        return ResponseDTO.ok();
    }

    private ResponseDTO<String> validateUniqueness(
            Long employeeId, String loginName, String phone, String email) {
        if (belongsToAnotherEmployee(employeeRepository.findIdByLoginName(loginName), employeeId)) {
            return ResponseDTO.userErrorParam("登录名重复");
        }
        if (belongsToAnotherEmployee(employeeRepository.findIdByPhone(phone), employeeId)) {
            return ResponseDTO.userErrorParam("手机号已存在");
        }
        if (belongsToAnotherEmployee(employeeRepository.findIdByEmail(email), employeeId)) {
            return ResponseDTO.userErrorParam("邮箱账号已存在");
        }
        return null;
    }

    private boolean belongsToAnotherEmployee(Optional<Long> existingId, Long employeeId) {
        return existingId.isPresent() && !Objects.equals(existingId.get(), employeeId);
    }

    private Optional<EmployeeAuthenticationAccount> activeAccount(Long employeeId) {
        if (employeeId == null) {
            return Optional.empty();
        }
        return employeeRepository.findAuthenticationAccountById(employeeId)
                .filter(account -> !Boolean.TRUE.equals(account.deleted()));
    }

    private boolean isAnotherActiveAdministrator(Long operatorEmployeeId, Long targetEmployeeId) {
        if (operatorEmployeeId == null || Objects.equals(operatorEmployeeId, targetEmployeeId)) {
            return false;
        }
        return employeeRepository.findAuthenticationAccountById(operatorEmployeeId)
                .filter(account -> Boolean.TRUE.equals(account.administrator()))
                .filter(account -> !Boolean.TRUE.equals(account.disabled()))
                .filter(account -> !Boolean.TRUE.equals(account.deleted()))
                .isPresent();
    }

    private List<Long> normalizeIds(List<Long> ids) {
        return ids == null ? List.of() : ids.stream().filter(Objects::nonNull).distinct().toList();
    }
}
