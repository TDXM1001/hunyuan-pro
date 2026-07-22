package com.hunyuan.sa.admin.module.identity.employee.infrastructure;

import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeSummary;
import com.hunyuan.sa.admin.module.identity.employee.domain.EmployeeRepository;
import com.hunyuan.sa.admin.module.organization.department.domain.OrganizationMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationEmployeeDirectoryAdapterTest {

    @Mock
    private EmployeeRepository employeeRepository;

    private OrganizationEmployeeDirectoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OrganizationEmployeeDirectoryAdapter();
        ReflectionTestUtils.setField(adapter, "employeeRepository", employeeRepository);
    }

    @Test
    void readsOrganizationDirectoryDataDirectlyFromEmployeeRepository() {
        EmployeeSummary employee = new EmployeeSummary(
                7L, "zhangsan", "Zhang San", null, 1, "13800000000", "a@example.com",
                20L, "Headquarters/R&D", 30L, false, LocalDateTime.of(2026, 7, 22, 9, 0));
        when(employeeRepository.exists(7L)).thenReturn(true);
        when(employeeRepository.countNonDeletedByDepartmentId(20L)).thenReturn(2);
        when(employeeRepository.findActive()).thenReturn(List.of(employee));

        assertThat(adapter.employeeExists(7L)).isTrue();
        assertThat(adapter.countActiveEmployees(20L)).isEqualTo(2);
        assertThat(adapter.listActiveEmployees())
                .containsExactly(new OrganizationMember(7L, "Zhang San", 20L));

        verify(employeeRepository).exists(7L);
        verify(employeeRepository).countNonDeletedByDepartmentId(20L);
        verify(employeeRepository).findActive();
    }
}
