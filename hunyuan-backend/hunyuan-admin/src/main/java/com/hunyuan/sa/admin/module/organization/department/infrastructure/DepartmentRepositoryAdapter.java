package com.hunyuan.sa.admin.module.organization.department.infrastructure;

import com.hunyuan.sa.admin.module.organization.department.domain.Department;
import com.hunyuan.sa.admin.module.organization.department.domain.DepartmentRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class DepartmentRepositoryAdapter implements DepartmentRepository {

    @Resource
    private DepartmentPersistenceMapper mapper;

    @Override
    public List<Department> findAll() {
        return mapper.selectDirectory().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Department> findById(Long departmentId) {
        return Optional.ofNullable(mapper.selectById(departmentId)).map(this::toDomain);
    }

    @Override
    public boolean exists(Long departmentId) {
        return departmentId != null && mapper.selectById(departmentId) != null;
    }

    @Override
    public int countChildren(Long departmentId) {
        return mapper.countChildren(departmentId);
    }

    @Override
    public Long insert(Department department) {
        DepartmentPersistenceEntity entity = toEntity(department);
        mapper.insert(entity);
        return entity.getDepartmentId();
    }

    @Override
    public void update(Department department) {
        mapper.updateById(toEntity(department));
    }

    @Override
    public void delete(Long departmentId) {
        mapper.deleteById(departmentId);
    }

    private Department toDomain(DepartmentPersistenceEntity entity) {
        return new Department(entity.getDepartmentId(), entity.getDepartmentName(), entity.getManagerId(), entity.getParentId(),
                entity.getSort(), entity.getManagerName(), entity.getCreateTime(), entity.getUpdateTime());
    }

    private DepartmentPersistenceEntity toEntity(Department department) {
        DepartmentPersistenceEntity entity = new DepartmentPersistenceEntity();
        entity.setDepartmentId(department.departmentId());
        entity.setDepartmentName(department.departmentName());
        entity.setManagerId(department.managerId());
        entity.setParentId(department.parentId());
        entity.setSort(department.sort());
        return entity;
    }
}
