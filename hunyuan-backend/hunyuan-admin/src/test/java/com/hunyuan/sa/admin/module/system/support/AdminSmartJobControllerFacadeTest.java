package com.hunyuan.sa.admin.module.system.support;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobFacade;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobLogView;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobView;
import com.hunyuan.sa.base.module.support.job.api.domain.SmartJobLogQueryForm;
import com.hunyuan.sa.base.module.support.job.api.domain.SmartJobQueryForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 锁定历史定时任务路由通过平台运行时 Facade 适配。
 */
@ExtendWith(MockitoExtension.class)
class AdminSmartJobControllerFacadeTest {

    @Mock
    private PlatformJobFacade facade;

    private AdminSmartJobController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminSmartJobController();
        ReflectionTestUtils.setField(controller, "platformJobFacade", facade);
    }

    @Test
    void mapsStableJobPageAndNestedLogToLegacyResponse() {
        PlatformJobLogView log = new PlatformJobLogView();
        log.setLogId(9L);
        PlatformJobView job = new PlatformJobView();
        job.setJobId(7);
        job.setJobName("同步任务");
        job.setLastJobLog(log);
        when(facade.queryJobs(any())).thenReturn(ResponseDTO.ok(pageOf(job)));

        var response = controller.queryJob(new SmartJobQueryForm());

        assertThat(response.getData().getList()).singleElement().satisfies(item -> {
            assertThat(item.getJobId()).isEqualTo(7);
            assertThat(item.getLastJobLog().getLogId()).isEqualTo(9L);
        });
    }

    @Test
    void mapsStableLogPageToLegacyResponse() {
        PlatformJobLogView log = new PlatformJobLogView();
        log.setLogId(11L);
        when(facade.queryLogs(any())).thenReturn(ResponseDTO.ok(pageOf(log)));

        var response = controller.queryJobLog(new SmartJobLogQueryForm());

        assertThat(response.getData().getList()).singleElement().satisfies(item ->
                assertThat(item.getLogId()).isEqualTo(11L));
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
}
