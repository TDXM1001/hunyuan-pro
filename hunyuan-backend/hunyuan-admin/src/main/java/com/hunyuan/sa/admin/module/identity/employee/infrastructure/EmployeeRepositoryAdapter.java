package com.hunyuan.sa.admin.module.identity.employee.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeAuthenticationAccount;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeCollaborationProfile;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeQuery;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeSummary;
import com.hunyuan.sa.admin.module.identity.employee.domain.EmployeeCreateDraft;
import com.hunyuan.sa.admin.module.identity.employee.domain.EmployeePage;
import com.hunyuan.sa.admin.module.identity.employee.domain.EmployeeProfileUpdate;
import com.hunyuan.sa.admin.module.identity.employee.domain.EmployeeRepository;
import com.hunyuan.sa.admin.module.identity.employee.domain.EmployeeSelfProfileUpdate;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EmployeeRepositoryAdapter implements EmployeeRepository {

    @Resource
    private EmployeePersistenceMapper mapper;

    @Override
    public Optional<EmployeeAuthenticationAccount> findAuthenticationAccountByLoginName(String loginName) {
        EmployeePersistenceEntity entity = mapper.selectOne(new LambdaQueryWrapper<EmployeePersistenceEntity>()
                .eq(EmployeePersistenceEntity::getLoginName, loginName)
                .last("LIMIT 1"));
        return Optional.ofNullable(entity).map(this::toAuthenticationAccount);
    }

    @Override
    public Optional<EmployeeAuthenticationAccount> findAuthenticationAccountById(Long employeeId) {
        return Optional.ofNullable(mapper.selectById(employeeId)).map(this::toAuthenticationAccount);
    }

    @Override
    public Optional<EmployeeSummary> findSummaryById(Long employeeId) {
        return Optional.ofNullable(mapper.selectById(employeeId)).map(this::toSummary);
    }

    @Override
    public Optional<EmployeeCollaborationProfile> findCollaborationProfileById(Long employeeId) {
        return Optional.ofNullable(mapper.selectById(employeeId)).map(this::toCollaborationProfile);
    }

    @Override
    public List<EmployeeCollaborationProfile> findCollaborationProfilesByIds(List<Long> employeeIds) {
        if (employeeIds.isEmpty()) {
            return List.of();
        }
        return mapper.selectBatchIds(employeeIds).stream().map(this::toCollaborationProfile).toList();
    }

    @Override
    public List<Long> findNonDeletedEmployeeIdsByDepartmentIds(List<Long> departmentIds) {
        if (departmentIds.isEmpty()) {
            return List.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<EmployeePersistenceEntity>()
                        .select(EmployeePersistenceEntity::getEmployeeId)
                        .in(EmployeePersistenceEntity::getDepartmentId, departmentIds)
                        .eq(EmployeePersistenceEntity::getDeletedFlag, false))
                .stream()
                .map(EmployeePersistenceEntity::getEmployeeId)
                .toList();
    }

    @Override
    public Optional<Long> findIdByLoginName(String loginName) {
        return findId(EmployeePersistenceEntity::getLoginName, loginName);
    }

    @Override
    public Optional<Long> findIdByPhone(String phone) {
        return findId(EmployeePersistenceEntity::getPhone, phone);
    }

    @Override
    public Optional<Long> findIdByEmail(String email) {
        return findId(EmployeePersistenceEntity::getEmail, email);
    }

    @Override
    public List<EmployeeAuthenticationAccount> findAuthenticationAccountsByIds(List<Long> employeeIds) {
        if (employeeIds.isEmpty()) {
            return List.of();
        }
        return mapper.selectBatchIds(employeeIds).stream().map(this::toAuthenticationAccount).toList();
    }

    @Override
    public EmployeePage query(EmployeeQuery query, List<Long> departmentIds) {
        Page<EmployeePersistenceEntity> page = new Page<>(query.getPageNum(), query.getPageSize());
        if (query.getSearchCount() != null) {
            page.setSearchCount(query.getSearchCount());
        }

        LambdaQueryWrapper<EmployeePersistenceEntity> wrapper = new LambdaQueryWrapper<EmployeePersistenceEntity>()
                .eq(EmployeePersistenceEntity::getDeletedFlag, false)
                .eq(query.getDisabled() != null, EmployeePersistenceEntity::getDisabledFlag, query.getDisabled())
                .in(!departmentIds.isEmpty(), EmployeePersistenceEntity::getDepartmentId, departmentIds)
                .and(StringUtils.isNotBlank(query.getKeyword()), keyword -> keyword
                        .like(EmployeePersistenceEntity::getActualName, query.getKeyword())
                        .or()
                        .like(EmployeePersistenceEntity::getPhone, query.getKeyword())
                        .or()
                        .like(EmployeePersistenceEntity::getLoginName, query.getKeyword()))
                .orderByDesc(EmployeePersistenceEntity::getCreateTime);

        Page<EmployeePersistenceEntity> result = mapper.selectPage(page, wrapper);
        return new EmployeePage(result.getCurrent(), result.getSize(), result.getTotal(), result.getPages(),
                result.getRecords().stream().map(this::toSummary).toList());
    }

    @Override
    public boolean exists(Long employeeId) {
        return employeeId != null && mapper.selectCount(new LambdaQueryWrapper<EmployeePersistenceEntity>()
                .eq(EmployeePersistenceEntity::getEmployeeId, employeeId)
                .eq(EmployeePersistenceEntity::getDeletedFlag, false)) > 0;
    }

    @Override
    public Long create(EmployeeCreateDraft draft) {
        EmployeePersistenceEntity entity = new EmployeePersistenceEntity();
        entity.setEmployeeUid(draft.employeeUid());
        entity.setLoginName(draft.loginName());
        entity.setLoginPwd(draft.passwordHash());
        entity.setActualName(draft.actualName());
        entity.setGender(draft.gender());
        entity.setPhone(draft.phone());
        entity.setEmail(draft.email());
        entity.setDepartmentId(draft.departmentId());
        entity.setPositionId(draft.positionId());
        entity.setAdministratorFlag(false);
        entity.setDisabledFlag(draft.disabled());
        entity.setDeletedFlag(false);
        entity.setRemark(draft.remark());
        mapper.insert(entity);
        return entity.getEmployeeId();
    }

    @Override
    public void updateProfile(EmployeeProfileUpdate update) {
        EmployeePersistenceEntity entity = new EmployeePersistenceEntity();
        entity.setEmployeeId(update.employeeId());
        entity.setLoginName(update.loginName());
        entity.setActualName(update.actualName());
        entity.setGender(update.gender());
        entity.setPhone(update.phone());
        entity.setEmail(update.email());
        entity.setDepartmentId(update.departmentId());
        entity.setPositionId(update.positionId());
        entity.setRemark(update.remark());
        mapper.updateById(entity);
    }

    @Override
    public void updateSelfProfile(EmployeeSelfProfileUpdate update) {
        EmployeePersistenceEntity entity = new EmployeePersistenceEntity();
        entity.setEmployeeId(update.employeeId());
        entity.setActualName(update.actualName());
        entity.setGender(update.gender());
        entity.setPhone(update.phone());
        entity.setEmail(update.email());
        entity.setPositionId(update.positionId());
        entity.setAvatar(update.avatar());
        entity.setRemark(update.remark());
        mapper.updateById(entity);
    }

    @Override
    public void updateAvatar(Long employeeId, String avatar) {
        EmployeePersistenceEntity entity = new EmployeePersistenceEntity();
        entity.setEmployeeId(employeeId);
        entity.setAvatar(avatar);
        mapper.updateById(entity);
    }

    @Override
    public void updateDisabled(Long employeeId, boolean disabled) {
        EmployeePersistenceEntity entity = new EmployeePersistenceEntity();
        entity.setEmployeeId(employeeId);
        entity.setDisabledFlag(disabled);
        mapper.updateById(entity);
    }

    @Override
    public void assignDepartment(List<Long> employeeIds, Long departmentId) {
        for (Long employeeId : employeeIds) {
            EmployeePersistenceEntity entity = new EmployeePersistenceEntity();
            entity.setEmployeeId(employeeId);
            entity.setDepartmentId(departmentId);
            mapper.updateById(entity);
        }
    }

    @Override
    public void markDeleted(List<Long> employeeIds) {
        for (Long employeeId : employeeIds) {
            EmployeePersistenceEntity entity = new EmployeePersistenceEntity();
            entity.setEmployeeId(employeeId);
            entity.setDeletedFlag(true);
            mapper.updateById(entity);
        }
    }

    @Override
    public void updatePassword(Long employeeId, String passwordHash) {
        EmployeePersistenceEntity entity = new EmployeePersistenceEntity();
        entity.setEmployeeId(employeeId);
        entity.setLoginPwd(passwordHash);
        mapper.updateById(entity);
    }

    @Override
    public int countNonDeletedByDepartmentId(Long departmentId) {
        return Math.toIntExact(mapper.selectCount(new LambdaQueryWrapper<EmployeePersistenceEntity>()
                .eq(EmployeePersistenceEntity::getDepartmentId, departmentId)
                .eq(EmployeePersistenceEntity::getDeletedFlag, false)));
    }

    @Override
    public int countNonDeletedByPositionId(Long positionId) {
        return Math.toIntExact(mapper.selectCount(new LambdaQueryWrapper<EmployeePersistenceEntity>()
                .eq(EmployeePersistenceEntity::getPositionId, positionId)
                .eq(EmployeePersistenceEntity::getDeletedFlag, false)));
    }

    @Override
    public List<EmployeeSummary> findActive() {
        return mapper.selectList(new LambdaQueryWrapper<EmployeePersistenceEntity>()
                        .eq(EmployeePersistenceEntity::getDisabledFlag, false)
                        .eq(EmployeePersistenceEntity::getDeletedFlag, false)
                        .orderByDesc(EmployeePersistenceEntity::getCreateTime))
                .stream()
                .map(this::toSummary)
                .toList();
    }

    private EmployeeAuthenticationAccount toAuthenticationAccount(EmployeePersistenceEntity entity) {
        return new EmployeeAuthenticationAccount(
                entity.getEmployeeId(),
                entity.getEmployeeUid(),
                entity.getLoginName(),
                entity.getLoginPwd(),
                entity.getActualName(),
                entity.getAvatar(),
                entity.getGender(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getDepartmentId(),
                entity.getPositionId(),
                entity.getAdministratorFlag(),
                entity.getDisabledFlag(),
                entity.getDeletedFlag(),
                entity.getRemark()
        );
    }

    private EmployeeSummary toSummary(EmployeePersistenceEntity entity) {
        return new EmployeeSummary(
                entity.getEmployeeId(),
                entity.getLoginName(),
                entity.getActualName(),
                entity.getAvatar(),
                entity.getGender(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getDepartmentId(),
                null,
                entity.getPositionId(),
                entity.getDisabledFlag(),
                entity.getCreateTime()
        );
    }

    private EmployeeCollaborationProfile toCollaborationProfile(EmployeePersistenceEntity entity) {
        return new EmployeeCollaborationProfile(
                entity.getEmployeeId(),
                entity.getActualName(),
                entity.getDepartmentId(),
                entity.getAdministratorFlag(),
                entity.getDisabledFlag(),
                entity.getDeletedFlag()
        );
    }

    private <T> Optional<Long> findId(
            com.baomidou.mybatisplus.core.toolkit.support.SFunction<EmployeePersistenceEntity, T> column,
            T value) {
        EmployeePersistenceEntity entity = mapper.selectOne(new LambdaQueryWrapper<EmployeePersistenceEntity>()
                .select(EmployeePersistenceEntity::getEmployeeId)
                .eq(column, value)
                .last("LIMIT 1"));
        return Optional.ofNullable(entity).map(EmployeePersistenceEntity::getEmployeeId);
    }
}
