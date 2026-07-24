package com.hunyuan.sa.admin.module.system.support;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.heartbeat.api.PlatformHeartbeatFacade;
import com.hunyuan.sa.base.module.support.heartbeat.api.PlatformHeartbeatRecordView;
import com.hunyuan.sa.base.module.support.heartbeat.domain.HeartBeatRecordQueryForm;
import com.hunyuan.sa.base.module.support.reload.api.PlatformRuntimeReloadFacade;
import com.hunyuan.sa.base.module.support.reload.api.PlatformRuntimeReloadItemView;
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
 * 锁定历史运行时机制路由通过平台 Facade 兼容响应。
 */
@ExtendWith(MockitoExtension.class)
class AdminRuntimeMechanismControllerFacadeTest {

    @Mock
    private PlatformRuntimeReloadFacade reloadFacade;

    @Mock
    private PlatformHeartbeatFacade heartbeatFacade;

    @Test
    void mapsStableReloadItemsToLegacyView() {
        AdminReloadController controller = new AdminReloadController();
        ReflectionTestUtils.setField(controller, "platformRuntimeReloadFacade", reloadFacade);
        PlatformRuntimeReloadItemView item = new PlatformRuntimeReloadItemView();
        item.setTag("login-config");
        when(reloadFacade.listItems()).thenReturn(ResponseDTO.ok(List.of(item)));

        var response = controller.query();

        assertThat(response.getData()).singleElement().satisfies(value ->
                assertThat(value.getTag()).isEqualTo("login-config"));
    }

    @Test
    void mapsStableHeartbeatPageToLegacyView() {
        AdminHeartBeatController controller = new AdminHeartBeatController();
        ReflectionTestUtils.setField(controller, "platformHeartbeatFacade", heartbeatFacade);
        PlatformHeartbeatRecordView record = new PlatformHeartbeatRecordView();
        record.setHeartBeatRecordId(7);
        when(heartbeatFacade.queryRecords(any())).thenReturn(ResponseDTO.ok(pageOf(record)));
        HeartBeatRecordQueryForm query = new HeartBeatRecordQueryForm();
        query.setPageNum(1L);
        query.setPageSize(10L);

        var response = controller.query(query);

        assertThat(response.getData().getList()).singleElement().satisfies(value ->
                assertThat(value.getHeartBeatRecordId()).isEqualTo(7));
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
