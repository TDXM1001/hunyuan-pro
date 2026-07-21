package com.hunyuan.sa.admin.module.organization.department.application;

import com.hunyuan.sa.admin.module.organization.department.domain.Department;
import com.hunyuan.sa.admin.module.organization.department.domain.DepartmentCommand;
import com.hunyuan.sa.admin.module.organization.department.domain.DepartmentRepository;
import com.hunyuan.sa.admin.module.organization.department.domain.OrganizationDirectoryPort;
import com.hunyuan.sa.admin.module.organization.department.domain.OrganizationDepartmentScopePort;
import com.hunyuan.sa.admin.module.organization.OrganizationModuleAvailability;
import com.hunyuan.sa.base.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationDepartmentApplicationServiceTest {

    @Mock
    private DepartmentRepository repository;
    @Mock
    private OrganizationDirectoryPort directoryPort;
    @Mock
    private OrganizationModuleAvailability moduleAvailability;
    @Mock
    private OrganizationDepartmentScopePort departmentScopePort;

    private OrganizationDepartmentApplicationService service;

    @BeforeEach
    void setUp() {
        service = new OrganizationDepartmentApplicationService();
        ReflectionTestUtils.setField(service, "departmentRepository", repository);
        ReflectionTestUtils.setField(service, "organizationDirectoryPort", directoryPort);
        ReflectionTestUtils.setField(service, "moduleAvailability", moduleAvailability);
        ReflectionTestUtils.setField(service, "departmentScopePort", departmentScopePort);
        org.mockito.Mockito.lenient().when(departmentScopePort.canAccess(org.mockito.ArgumentMatchers.anyLong())).thenReturn(true);
        org.mockito.Mockito.lenient().when(departmentScopePort.canCreateUnder(org.mockito.ArgumentMatchers.anyLong())).thenReturn(true);
    }

    @Test
    void rejectsMovingDepartmentUnderItsDescendant() {
        when(repository.exists(10L)).thenReturn(true);
        when(repository.exists(30L)).thenReturn(true);
        when(repository.findById(30L)).thenReturn(Optional.of(department(30L, 20L)));
        when(repository.findById(20L)).thenReturn(Optional.of(department(20L, 10L)));

        assertThatThrownBy(() -> service.update(10L, new DepartmentCommand("研发中心", null, 30L, 10)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("不能移动到自己的下级部门");

        verify(repository, never()).update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsDeletingDepartmentWithChildren() {
        when(repository.exists(10L)).thenReturn(true);
        when(repository.countChildren(10L)).thenReturn(1);

        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请先删除子部门");

        verify(repository, never()).delete(10L);
    }

    @Test
    void rejectsDeletingDepartmentWithActiveEmployees() {
        when(repository.exists(10L)).thenReturn(true);
        when(repository.countChildren(10L)).thenReturn(0);
        when(directoryPort.countActiveEmployees(10L)).thenReturn(2);

        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请先处理部门员工");

        verify(repository, never()).delete(10L);
    }

    @Test
    void deletesEmptyDepartment() {
        when(repository.exists(10L)).thenReturn(true);
        when(repository.countChildren(10L)).thenReturn(0);
        when(directoryPort.countActiveEmployees(10L)).thenReturn(0);

        service.delete(10L);

        verify(repository).delete(10L);
    }

    @Test
    void collaborationLookupKeepsMissingDepartmentSemantics() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThat(service.findForCollaboration(999L)).isEmpty();

        verifyNoInteractions(moduleAvailability, departmentScopePort);
    }

    @Test
    void collaborationQueriesBuildDescendantsAndPathWithoutPageScope() {
        when(repository.findAll()).thenReturn(List.of(
                department(10L, 0L),
                department(20L, 10L),
                department(30L, 20L)
        ));

        assertThat(service.selfAndDescendantIdsForCollaboration(10L)).containsExactly(10L, 20L, 30L);
        assertThat(service.pathForCollaboration(30L)).isEqualTo("部门10/部门20/部门30");

        verifyNoInteractions(moduleAvailability, departmentScopePort);
    }

    private Department department(Long id, Long parentId) {
        return new Department(id, "部门" + id, null, parentId, 10, null, null, null);
    }
}
