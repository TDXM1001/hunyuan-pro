package com.hunyuan.sa.admin.module.identity.employee.application;

import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeQuery;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeSummary;
import com.hunyuan.sa.admin.module.identity.employee.domain.EmployeePage;
import com.hunyuan.sa.admin.module.identity.employee.domain.EmployeeRepository;
import com.hunyuan.sa.admin.module.organization.department.application.OrganizationDepartmentFacade;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.module.support.file.api.PlatformFileFacade;
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
class EmployeeDirectoryApplicationServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private OrganizationDepartmentFacade organizationDepartmentFacade;
    @Mock
    private PlatformFileFacade platformFileFacade;

    private EmployeeDirectoryApplicationService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeDirectoryApplicationService();
        ReflectionTestUtils.setField(service, "employeeRepository", employeeRepository);
        ReflectionTestUtils.setField(service, "organizationDepartmentFacade", organizationDepartmentFacade);
        ReflectionTestUtils.setField(service, "platformFileFacade", platformFileFacade);
    }

    @Test
    void queriesDepartmentDescendantsAndReturnsPublicSummaries() {
        EmployeeQuery query = new EmployeeQuery();
        query.setPageNum(1L);
        query.setPageSize(20L);
        query.setDepartmentId(10L);
        query.setKeyword("张");
        query.setDisabled(false);

        EmployeeSummary employee = new EmployeeSummary(
                7L, "zhangsan", "张三", null, 1, "13800000000", "a@example.com",
                20L, null, 30L, false, LocalDateTime.of(2026, 7, 21, 9, 0));
        when(organizationDepartmentFacade.selfAndDescendantIdsForCollaboration(10L))
                .thenReturn(List.of(10L, 20L));
        when(employeeRepository.query(query, List.of(10L, 20L)))
                .thenReturn(new EmployeePage(1, 20, 1, 1, List.of(employee)));
        when(organizationDepartmentFacade.pathForCollaboration(20L)).thenReturn("总部/研发部");

        PageResult<EmployeeSummary> result = service.query(query);

        assertThat(result.getList()).containsExactly(employee.withDepartmentName("总部/研发部"));
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getEmptyFlag()).isFalse();
        verify(employeeRepository).query(query, List.of(10L, 20L));
    }

    @Test
    void publicSummaryDoesNotExposeAuthenticationOrAdministratorFields() {
        assertThat(EmployeeSummary.class.getRecordComponents())
                .extracting(component -> component.getName())
                .doesNotContain("passwordHash", "employeeUid", "administrator", "deleted");
    }

    @Test
    void departmentOccupancyCountsDisabledButNonDeletedEmployees() {
        when(employeeRepository.countNonDeletedByDepartmentId(20L)).thenReturn(2);

        assertThat(service.countActiveEmployees(20L)).isEqualTo(2);

        verify(employeeRepository).countNonDeletedByDepartmentId(20L);
    }
}
