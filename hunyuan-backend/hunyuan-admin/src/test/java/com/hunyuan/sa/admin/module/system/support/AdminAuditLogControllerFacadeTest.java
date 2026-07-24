package com.hunyuan.sa.admin.module.system.support;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.RequestUser;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.enumeration.UserTypeEnum;
import com.hunyuan.sa.base.common.util.SmartRequestUtil;
import com.hunyuan.sa.base.module.support.audit.api.PlatformAuditLogFacade;
import com.hunyuan.sa.base.module.support.audit.api.PlatformLoginLogPageQuery;
import com.hunyuan.sa.base.module.support.audit.api.PlatformLoginLogSummary;
import com.hunyuan.sa.base.module.support.audit.api.PlatformOperateLogPageQuery;
import com.hunyuan.sa.base.module.support.audit.api.PlatformOperateLogSummary;
import com.hunyuan.sa.base.module.support.loginlog.domain.LoginLogQueryForm;
import com.hunyuan.sa.base.module.support.operatelog.domain.OperateLogQueryForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定历史审计日志路由通过稳定 Facade 适配。
 */
@ExtendWith(MockitoExtension.class)
class AdminAuditLogControllerFacadeTest {

    @Mock
    private PlatformAuditLogFacade facade;

    private AdminOperateLogController operateController;
    private AdminLoginLogController loginController;

    @BeforeEach
    void setUp() {
        operateController = new AdminOperateLogController();
        loginController = new AdminLoginLogController();
        ReflectionTestUtils.setField(operateController, "platformAuditLogFacade", facade);
        ReflectionTestUtils.setField(loginController, "platformAuditLogFacade", facade);
    }

    @Test
    void mapsLegacyManagementResponses() {
        PlatformOperateLogSummary operateLog = new PlatformOperateLogSummary();
        operateLog.setOperateLogId(11L);
        PlatformLoginLogSummary loginLog = new PlatformLoginLogSummary();
        loginLog.setLoginLogId(21L);
        when(facade.queryOperateLogs(any())).thenReturn(ResponseDTO.ok(pageOf(operateLog)));
        when(facade.queryLoginLogs(any())).thenReturn(ResponseDTO.ok(pageOf(loginLog)));

        OperateLogQueryForm operateQuery = new OperateLogQueryForm();
        operateQuery.setPageNum(1L);
        operateQuery.setPageSize(10L);
        LoginLogQueryForm loginQuery = new LoginLogQueryForm();
        loginQuery.setPageNum(1L);
        loginQuery.setPageSize(10L);

        assertThat(operateController.queryByPage(operateQuery).getData().getList())
                .singleElement().satisfies(log -> assertThat(log.getOperateLogId()).isEqualTo(11L));
        assertThat(loginController.queryByPage(loginQuery).getData().getList())
                .singleElement().satisfies(log -> assertThat(log.getLoginLogId()).isEqualTo(21L));
    }

    @Test
    void currentUserCompatibilityRoutesForceLoginScope() {
        when(facade.queryOperateLogs(any())).thenReturn(ResponseDTO.ok(emptyPage()));
        when(facade.queryLoginLogs(any())).thenReturn(ResponseDTO.ok(emptyPage()));
        RequestUser user = mock(RequestUser.class);
        when(user.getUserId()).thenReturn(7L);
        when(user.getUserType()).thenReturn(UserTypeEnum.ADMIN_EMPLOYEE);
        OperateLogQueryForm operateQuery = new OperateLogQueryForm();
        LoginLogQueryForm loginQuery = new LoginLogQueryForm();

        try (var mocked = org.mockito.Mockito.mockStatic(SmartRequestUtil.class)) {
            mocked.when(SmartRequestUtil::getRequestUser).thenReturn(user);
            operateController.queryByPageLogin(operateQuery);
            loginController.queryByPageLogin(loginQuery);
        }

        ArgumentCaptor<PlatformOperateLogPageQuery> operateCaptor =
                ArgumentCaptor.forClass(PlatformOperateLogPageQuery.class);
        ArgumentCaptor<PlatformLoginLogPageQuery> loginCaptor =
                ArgumentCaptor.forClass(PlatformLoginLogPageQuery.class);
        verify(facade).queryOperateLogs(operateCaptor.capture());
        verify(facade).queryLoginLogs(loginCaptor.capture());
        assertThat(operateCaptor.getValue().getOperateUserId()).isEqualTo(7L);
        assertThat(operateCaptor.getValue().getOperateUserType())
                .isEqualTo(UserTypeEnum.ADMIN_EMPLOYEE.getValue());
        assertThat(loginCaptor.getValue().getUserId()).isEqualTo(7L);
        assertThat(loginCaptor.getValue().getUserType())
                .isEqualTo(UserTypeEnum.ADMIN_EMPLOYEE.getValue());
    }

    private <T> PageResult<T> pageOf(T item) {
        PageResult<T> page = new PageResult<>();
        page.setPageNum(1L);
        page.setPageSize(10L);
        page.setTotal(1L);
        page.setPages(1L);
        page.setEmptyFlag(false);
        page.setList(List.of(item));
        return page;
    }

    private <T> PageResult<T> emptyPage() {
        PageResult<T> page = new PageResult<>();
        page.setPageNum(1L);
        page.setPageSize(10L);
        page.setTotal(0L);
        page.setPages(0L);
        page.setEmptyFlag(true);
        page.setList(List.of());
        return page;
    }
}
