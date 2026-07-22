package com.hunyuan.sa.admin.module.business.oa.notice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.admin.module.business.oa.notice.constant.NoticeVisibleRangeDataTypeEnum;
import com.hunyuan.sa.admin.module.business.oa.notice.dao.NoticeDao;
import com.hunyuan.sa.admin.module.business.oa.notice.domain.form.NoticeEmployeeQueryForm;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeCollaborationProfile;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeDirectoryFacade;
import com.hunyuan.sa.admin.module.organization.department.application.OrganizationDepartmentFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeEmployeeServiceCollaborationTest {

    @Mock
    private NoticeDao noticeDao;
    @Mock
    private EmployeeDirectoryFacade employeeDirectoryFacade;
    @Mock
    private OrganizationDepartmentFacade organizationDepartmentFacade;

    private NoticeEmployeeService service;

    @BeforeEach
    void setUp() {
        service = new NoticeEmployeeService();
        ReflectionTestUtils.setField(service, "noticeDao", noticeDao);
        ReflectionTestUtils.setField(service, "employeeDirectoryFacade", employeeDirectoryFacade);
        ReflectionTestUtils.setField(service, "organizationDepartmentFacade", organizationDepartmentFacade);
    }

    @Test
    void nonAdministratorQueryUsesEmployeeDepartmentDescendants() {
        NoticeEmployeeQueryForm form = new NoticeEmployeeQueryForm();
        form.setPageNum(1L);
        form.setPageSize(20L);
        when(employeeDirectoryFacade.findCollaborationProfileById(7L))
                .thenReturn(Optional.of(new EmployeeCollaborationProfile(
                        7L, "Alice", 20L, false, false, false)));
        when(organizationDepartmentFacade.selfAndDescendantIdsForCollaboration(20L))
                .thenReturn(List.of(20L, 21L));
        when(noticeDao.queryEmployeeNotice(
                any(Page.class),
                eq(7L),
                eq(form),
                eq(List.of(20L, 21L)),
                eq(false),
                eq(false),
                eq(NoticeVisibleRangeDataTypeEnum.DEPARTMENT.getValue()),
                eq(NoticeVisibleRangeDataTypeEnum.EMPLOYEE.getValue())))
                .thenReturn(List.of());

        service.queryList(7L, form);

        verify(organizationDepartmentFacade).selfAndDescendantIdsForCollaboration(20L);
        verify(noticeDao).queryEmployeeNotice(
                any(Page.class),
                eq(7L),
                eq(form),
                eq(List.of(20L, 21L)),
                eq(false),
                eq(false),
                eq(NoticeVisibleRangeDataTypeEnum.DEPARTMENT.getValue()),
                eq(NoticeVisibleRangeDataTypeEnum.EMPLOYEE.getValue()));
    }
}
