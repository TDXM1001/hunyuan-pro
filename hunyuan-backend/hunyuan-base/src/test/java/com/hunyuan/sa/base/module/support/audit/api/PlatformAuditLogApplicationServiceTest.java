package com.hunyuan.sa.base.module.support.audit.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.audit.application.PlatformAuditLogApplicationService;
import com.hunyuan.sa.base.module.support.loginlog.LoginLogService;
import com.hunyuan.sa.base.module.support.loginlog.domain.LoginLogQueryForm;
import com.hunyuan.sa.base.module.support.loginlog.domain.LoginLogVO;
import com.hunyuan.sa.base.module.support.operatelog.OperateLogService;
import com.hunyuan.sa.base.module.support.operatelog.domain.OperateLogQueryForm;
import com.hunyuan.sa.base.module.support.operatelog.domain.OperateLogVO;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定稳定审计日志边界与历史查询服务之间的映射。
 */
@ExtendWith(MockitoExtension.class)
class PlatformAuditLogApplicationServiceTest {

    @Mock
    private OperateLogService operateLogService;

    @Mock
    private LoginLogService loginLogService;

    private PlatformAuditLogApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PlatformAuditLogApplicationService();
        ReflectionTestUtils.setField(service, "operateLogService", operateLogService);
        ReflectionTestUtils.setField(service, "loginLogService", loginLogService);
    }

    @Test
    void mapsOperateLogQueryAndPageResult() {
        OperateLogVO legacyLog = new OperateLogVO();
        legacyLog.setOperateLogId(11L);
        legacyLog.setOperateUserName("hunyuan");
        legacyLog.setModule("系统设置");
        PageResult<OperateLogVO> legacyPage = pageOf(legacyLog);
        when(operateLogService.queryByPage(any(OperateLogQueryForm.class)))
                .thenReturn(ResponseDTO.ok(legacyPage));

        PlatformOperateLogPageQuery query = new PlatformOperateLogPageQuery();
        query.setPageNum(1L);
        query.setPageSize(10L);
        query.setUserName("hunyuan");
        ResponseDTO<PageResult<PlatformOperateLogSummary>> response =
                service.queryOperateLogs(query);

        ArgumentCaptor<OperateLogQueryForm> captor =
                ArgumentCaptor.forClass(OperateLogQueryForm.class);
        verify(operateLogService).queryByPage(captor.capture());
        assertThat(captor.getValue().getUserName()).isEqualTo("hunyuan");
        assertThat(response.getData().getList()).singleElement().satisfies(log -> {
            assertThat(log.getOperateLogId()).isEqualTo(11L);
            assertThat(log.getModule()).isEqualTo("系统设置");
        });
    }

    @Test
    void keepsOperateLogDetailFailureCodeAndMessage() {
        when(operateLogService.detail(99L)).thenReturn(ResponseDTO.userErrorParam("日志不存在"));

        ResponseDTO<PlatformOperateLogSummary> response = service.getOperateLog(99L);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).isEqualTo("日志不存在");
    }

    @Test
    void mapsLoginLogQueryAndPageResult() {
        LoginLogVO legacyLog = new LoginLogVO();
        legacyLog.setLoginLogId(21L);
        legacyLog.setUserName("hunyuan");
        legacyLog.setLoginIp("127.0.0.1");
        PageResult<LoginLogVO> legacyPage = pageOf(legacyLog);
        when(loginLogService.queryByPage(any(LoginLogQueryForm.class)))
                .thenReturn(ResponseDTO.ok(legacyPage));

        PlatformLoginLogPageQuery query = new PlatformLoginLogPageQuery();
        query.setPageNum(1L);
        query.setPageSize(10L);
        query.setIp("127.0.0.1");
        ResponseDTO<PageResult<PlatformLoginLogSummary>> response = service.queryLoginLogs(query);

        ArgumentCaptor<LoginLogQueryForm> captor = ArgumentCaptor.forClass(LoginLogQueryForm.class);
        verify(loginLogService).queryByPage(captor.capture());
        assertThat(captor.getValue().getIp()).isEqualTo("127.0.0.1");
        assertThat(response.getData().getList()).singleElement().satisfies(log -> {
            assertThat(log.getLoginLogId()).isEqualTo(21L);
            assertThat(log.getUserName()).isEqualTo("hunyuan");
        });
    }

    /**
     * 构造单条日志分页结果，避免测试重复分页元数据。
     */
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
}
