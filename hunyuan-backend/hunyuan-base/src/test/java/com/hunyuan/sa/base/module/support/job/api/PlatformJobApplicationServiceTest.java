package com.hunyuan.sa.base.module.support.job.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.job.api.domain.SmartJobAddForm;
import com.hunyuan.sa.base.module.support.job.api.domain.SmartJobExecuteForm;
import com.hunyuan.sa.base.module.support.job.api.domain.SmartJobLogVO;
import com.hunyuan.sa.base.module.support.job.api.domain.SmartJobVO;
import com.hunyuan.sa.base.module.support.job.application.PlatformJobApplicationService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定平台定时任务边界与既有调度管理服务之间的映射。
 */
@ExtendWith(MockitoExtension.class)
class PlatformJobApplicationServiceTest {

    @Mock
    private SmartJobService smartJobService;

    private PlatformJobApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PlatformJobApplicationService();
        ReflectionTestUtils.setField(service, "smartJobService", smartJobService);
    }

    @Test
    void mapsJobPageAndNestedExecutionLogToPublicViews() {
        SmartJobLogVO log = new SmartJobLogVO();
        log.setLogId(9L);
        SmartJobVO job = new SmartJobVO();
        job.setJobId(7);
        job.setJobName("同步任务");
        job.setLastJobLog(log);
        when(smartJobService.queryJob(any())).thenReturn(ResponseDTO.ok(pageOf(job)));

        ResponseDTO<PageResult<PlatformJobView>> response = service.queryJobs(pageQuery());

        assertThat(response.getData().getList()).singleElement().satisfies(item -> {
            assertThat(item.getJobId()).isEqualTo(7);
            assertThat(item.getLastJobLog().getLogId()).isEqualTo(9L);
        });
    }

    @Test
    void injectsServerOperatorIntoCreateAndExecuteForms() {
        when(smartJobService.addJob(any())).thenReturn(ResponseDTO.ok());
        when(smartJobService.execute(any())).thenReturn(ResponseDTO.ok());
        PlatformJobCreateCommand create = new PlatformJobCreateCommand();
        create.setJobName("同步任务");
        create.setJobClass("com.example.SyncJob");
        PlatformJobExecuteCommand execute = new PlatformJobExecuteCommand();
        execute.setJobId(7);

        service.createJob(create, "hunyuan");
        service.executeJob(execute, "hunyuan");

        ArgumentCaptor<SmartJobAddForm> createCaptor =
                ArgumentCaptor.forClass(SmartJobAddForm.class);
        ArgumentCaptor<SmartJobExecuteForm> executeCaptor =
                ArgumentCaptor.forClass(SmartJobExecuteForm.class);
        verify(smartJobService).addJob(createCaptor.capture());
        verify(smartJobService).execute(executeCaptor.capture());
        assertThat(createCaptor.getValue().getUpdateName()).isEqualTo("hunyuan");
        assertThat(executeCaptor.getValue().getUpdateName()).isEqualTo("hunyuan");
    }

    @Test
    void mapsDeleteOperatorWithoutExposingLegacyRequestUser() {
        when(smartJobService.deleteJob(anyInt(), anyString())).thenReturn(ResponseDTO.ok());

        service.deleteJob(7, "hunyuan");

        ArgumentCaptor<String> operatorCaptor = ArgumentCaptor.forClass(String.class);
        verify(smartJobService).deleteJob(anyInt(), operatorCaptor.capture());
        assertThat(operatorCaptor.getValue()).isEqualTo("hunyuan");
    }

    private PlatformJobPageQuery pageQuery() {
        PlatformJobPageQuery query = new PlatformJobPageQuery();
        query.setPageNum(1L);
        query.setPageSize(10L);
        return query;
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
