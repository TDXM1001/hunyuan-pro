package com.hunyuan.sa.base.module.support.reload.api;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.reload.ReloadService;
import com.hunyuan.sa.base.module.support.reload.application.PlatformRuntimeReloadApplicationService;
import com.hunyuan.sa.base.module.support.reload.domain.ReloadForm;
import com.hunyuan.sa.base.module.support.reload.domain.ReloadItemVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定运行时重载公开模型与既有服务之间的映射。
 */
@ExtendWith(MockitoExtension.class)
class PlatformRuntimeReloadApplicationServiceTest {

    @Mock
    private ReloadService reloadService;

    private PlatformRuntimeReloadApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PlatformRuntimeReloadApplicationService();
        ReflectionTestUtils.setField(service, "reloadService", reloadService);
    }

    @Test
    void mapsLegacyItemToPublicView() {
        ReloadItemVO item = new ReloadItemVO();
        item.setTag("login-config");
        item.setIdentification("20260724");
        when(reloadService.query()).thenReturn(ResponseDTO.ok(List.of(item)));

        var response = service.listItems();

        assertThat(response.getData()).singleElement().satisfies(value -> {
            assertThat(value.getTag()).isEqualTo("login-config");
            assertThat(value.getIdentification()).isEqualTo("20260724");
        });
    }

    @Test
    void mapsPublicUpdateCommandToLegacyForm() {
        when(reloadService.updateByTag(org.mockito.ArgumentMatchers.any()))
                .thenReturn(ResponseDTO.ok());
        PlatformRuntimeReloadUpdateCommand command = new PlatformRuntimeReloadUpdateCommand();
        command.setTag("login-config");
        command.setIdentification("20260724");
        command.setArgs("force=true");

        service.updateItem(command);

        ArgumentCaptor<ReloadForm> captor = ArgumentCaptor.forClass(ReloadForm.class);
        verify(reloadService).updateByTag(captor.capture());
        assertThat(captor.getValue().getTag()).isEqualTo("login-config");
        assertThat(captor.getValue().getArgs()).isEqualTo("force=true");
    }
}
