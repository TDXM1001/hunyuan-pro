package com.hunyuan.sa.base.module.support.heartbeat.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.heartbeat.HeartBeatService;
import com.hunyuan.sa.base.module.support.heartbeat.application.PlatformHeartbeatApplicationService;
import com.hunyuan.sa.base.module.support.heartbeat.domain.HeartBeatRecordQueryForm;
import com.hunyuan.sa.base.module.support.heartbeat.domain.HeartBeatRecordVO;
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
 * 锁定心跳记录查询公开模型与既有服务之间的映射。
 */
@ExtendWith(MockitoExtension.class)
class PlatformHeartbeatApplicationServiceTest {

    @Mock
    private HeartBeatService heartBeatService;

    private PlatformHeartbeatApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PlatformHeartbeatApplicationService();
        ReflectionTestUtils.setField(service, "heartBeatService", heartBeatService);
    }

    @Test
    void mapsPageQueryAndResultWithoutTouchingHeartbeatWriter() {
        HeartBeatRecordVO record = new HeartBeatRecordVO();
        record.setHeartBeatRecordId(7);
        record.setProjectPath("/srv/hunyuan");
        when(heartBeatService.pageQuery(any())).thenReturn(ResponseDTO.ok(pageOf(record)));
        PlatformHeartbeatPageQuery query = new PlatformHeartbeatPageQuery();
        query.setPageNum(2L);
        query.setPageSize(20L);
        query.setKeywords("hunyuan");

        var response = service.queryRecords(query);

        ArgumentCaptor<HeartBeatRecordQueryForm> captor =
                ArgumentCaptor.forClass(HeartBeatRecordQueryForm.class);
        verify(heartBeatService).pageQuery(captor.capture());
        assertThat(captor.getValue().getPageNum()).isEqualTo(2L);
        assertThat(captor.getValue().getKeywords()).isEqualTo("hunyuan");
        assertThat(response.getData().getList()).singleElement().satisfies(value -> {
            assertThat(value.getHeartBeatRecordId()).isEqualTo(7);
            assertThat(value.getProjectPath()).isEqualTo("/srv/hunyuan");
        });
    }

    private <T> PageResult<T> pageOf(T item) {
        PageResult<T> page = new PageResult<>();
        page.setPageNum(2L);
        page.setPageSize(20L);
        page.setTotal(1L);
        page.setPages(1L);
        page.setEmptyFlag(false);
        page.setList(List.of(item));
        return page;
    }
}
