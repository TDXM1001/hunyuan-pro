package com.hunyuan.sa.base.module.support.config.application;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.module.support.config.ConfigService;
import com.hunyuan.sa.base.module.support.config.api.PlatformConfigurationCreateCommand;
import com.hunyuan.sa.base.module.support.config.api.PlatformConfigurationFacade;
import com.hunyuan.sa.base.module.support.config.api.PlatformConfigurationPageQuery;
import com.hunyuan.sa.base.module.support.config.api.PlatformConfigurationSummary;
import com.hunyuan.sa.base.module.support.config.api.PlatformConfigurationUpdateCommand;
import com.hunyuan.sa.base.module.support.config.domain.ConfigAddForm;
import com.hunyuan.sa.base.module.support.config.domain.ConfigQueryForm;
import com.hunyuan.sa.base.module.support.config.domain.ConfigUpdateForm;
import com.hunyuan.sa.base.module.support.config.domain.ConfigVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 平台配置管理读取用例，隔离历史配置 VO 与稳定 HTTP 契约。
 */
@Service
public class PlatformConfigurationApplicationService implements PlatformConfigurationFacade {

    @Resource
    private ConfigService configService;

    @Override
    public ResponseDTO<PageResult<PlatformConfigurationSummary>> queryPage(
            PlatformConfigurationPageQuery query) {
        ConfigQueryForm legacyQuery = SmartBeanUtil.copy(query, ConfigQueryForm.class);
        ResponseDTO<PageResult<ConfigVO>> legacyResponse = configService.queryConfigPage(legacyQuery);
        if (!Boolean.TRUE.equals(legacyResponse.getOk())) {
            return ResponseDTO.error(legacyResponse);
        }

        PageResult<ConfigVO> legacyResult = legacyResponse.getData();
        PageResult<PlatformConfigurationSummary> result = new PageResult<>();
        result.setPageNum(legacyResult.getPageNum());
        result.setPageSize(legacyResult.getPageSize());
        result.setTotal(legacyResult.getTotal());
        result.setPages(legacyResult.getPages());
        result.setEmptyFlag(legacyResult.getEmptyFlag());
        result.setList(SmartBeanUtil.copyList(legacyResult.getList(), PlatformConfigurationSummary.class));
        return ResponseDTO.ok(result);
    }

    @Override
    public ResponseDTO<String> create(PlatformConfigurationCreateCommand command) {
        ConfigAddForm legacyCommand = SmartBeanUtil.copy(command, ConfigAddForm.class);
        return configService.add(legacyCommand);
    }

    @Override
    public ResponseDTO<String> update(Long configurationId, PlatformConfigurationUpdateCommand command) {
        ConfigUpdateForm legacyCommand = SmartBeanUtil.copy(command, ConfigUpdateForm.class);
        legacyCommand.setConfigId(configurationId);
        return configService.updateConfig(legacyCommand);
    }
}
