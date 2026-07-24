package com.hunyuan.sa.base.module.support.config.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.config.ConfigService;
import com.hunyuan.sa.base.module.support.config.application.PlatformConfigurationApplicationService;
import com.hunyuan.sa.base.module.support.config.domain.ConfigQueryForm;
import com.hunyuan.sa.base.module.support.config.domain.ConfigAddForm;
import com.hunyuan.sa.base.module.support.config.domain.ConfigUpdateForm;
import com.hunyuan.sa.base.module.support.config.domain.ConfigVO;
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
 * 锁定稳定配置读取边界与历史配置服务之间的映射。
 */
@ExtendWith(MockitoExtension.class)
class PlatformConfigurationApplicationServiceTest {

    @Mock
    private ConfigService configService;

    private PlatformConfigurationApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PlatformConfigurationApplicationService();
        ReflectionTestUtils.setField(service, "configService", configService);
    }

    @Test
    void mapsQueryAndResultToStableContract() {
        ConfigVO legacyItem = new ConfigVO();
        legacyItem.setConfigId(8L);
        legacyItem.setConfigKey("system.welcome.text");
        legacyItem.setConfigValue("欢迎使用");
        PageResult<ConfigVO> legacyResult = new PageResult<>();
        legacyResult.setPageNum(1L);
        legacyResult.setPageSize(20L);
        legacyResult.setTotal(1L);
        legacyResult.setPages(1L);
        legacyResult.setEmptyFlag(false);
        legacyResult.setList(List.of(legacyItem));
        when(configService.queryConfigPage(org.mockito.ArgumentMatchers.any(ConfigQueryForm.class)))
                .thenReturn(ResponseDTO.ok(legacyResult));

        PlatformConfigurationPageQuery query = new PlatformConfigurationPageQuery();
        query.setPageNum(1L);
        query.setPageSize(20L);
        query.setConfigKey("system.welcome.text");
        ResponseDTO<PageResult<PlatformConfigurationSummary>> response = service.queryPage(query);

        ArgumentCaptor<ConfigQueryForm> captor = ArgumentCaptor.forClass(ConfigQueryForm.class);
        verify(configService).queryConfigPage(captor.capture());
        assertThat(captor.getValue().getConfigKey()).isEqualTo("system.welcome.text");
        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getList()).singleElement().satisfies(item -> {
            assertThat(item.getConfigId()).isEqualTo(8L);
            assertThat(item.getConfigValue()).isEqualTo("欢迎使用");
        });
    }

    @Test
    void keepsLegacyFailureCodeAndMessage() {
        PlatformConfigurationPageQuery query = new PlatformConfigurationPageQuery();
        when(configService.queryConfigPage(org.mockito.ArgumentMatchers.any(ConfigQueryForm.class)))
                .thenReturn(ResponseDTO.userErrorParam("无权读取配置"));

        ResponseDTO<PageResult<PlatformConfigurationSummary>> response = service.queryPage(query);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).isEqualTo("无权读取配置");
    }

    @Test
    void delegatesCreateToLegacyWritePath() {
        PlatformConfigurationCreateCommand command = new PlatformConfigurationCreateCommand();
        command.setConfigKey("system.welcome.text");
        command.setConfigName("欢迎文案");
        command.setConfigValue("欢迎使用");
        when(configService.add(org.mockito.ArgumentMatchers.any(ConfigAddForm.class)))
                .thenReturn(ResponseDTO.ok());

        ResponseDTO<String> response = service.create(command);

        ArgumentCaptor<ConfigAddForm> captor = ArgumentCaptor.forClass(ConfigAddForm.class);
        verify(configService).add(captor.capture());
        assertThat(captor.getValue().getConfigKey()).isEqualTo("system.welcome.text");
        assertThat(response.getOk()).isTrue();
    }

    @Test
    void delegatesUpdateWithPathIdentifierToLegacyWritePath() {
        PlatformConfigurationUpdateCommand command = new PlatformConfigurationUpdateCommand();
        command.setConfigKey("system.welcome.text");
        command.setConfigName("欢迎文案");
        command.setConfigValue("新的欢迎文案");
        when(configService.updateConfig(org.mockito.ArgumentMatchers.any(ConfigUpdateForm.class)))
                .thenReturn(ResponseDTO.ok());

        ResponseDTO<String> response = service.update(8L, command);

        ArgumentCaptor<ConfigUpdateForm> captor = ArgumentCaptor.forClass(ConfigUpdateForm.class);
        verify(configService).updateConfig(captor.capture());
        assertThat(captor.getValue().getConfigId()).isEqualTo(8L);
        assertThat(captor.getValue().getConfigValue()).isEqualTo("新的欢迎文案");
        assertThat(response.getOk()).isTrue();
    }
}
