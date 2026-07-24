package com.hunyuan.sa.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.config.api.PlatformConfigurationCreateCommand;
import com.hunyuan.sa.base.module.support.config.api.PlatformConfigurationFacade;
import com.hunyuan.sa.base.module.support.config.api.PlatformConfigurationPageQuery;
import com.hunyuan.sa.base.module.support.config.api.PlatformConfigurationSummary;
import com.hunyuan.sa.base.module.support.config.api.PlatformConfigurationUpdateCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台配置管理稳定 HTTP 接口。
 */
@RestController
@RequestMapping("/api/admin/v1/platform/configurations")
@Tag(name = "平台能力 - 配置")
public class PlatformConfigurationController {

    @Resource
    private PlatformConfigurationFacade platformConfigurationFacade;

    @PostMapping("/query")
    @Operation(operationId = "platformConfigurationQuery", summary = "分页查询系统配置")
    @SaCheckPermission("support:config:query")
    public ResponseDTO<PageResult<PlatformConfigurationSummary>> queryPage(
            @RequestBody @Valid PlatformConfigurationPageQuery query) {
        return platformConfigurationFacade.queryPage(query);
    }

    @PostMapping
    @Operation(operationId = "platformConfigurationCreate", summary = "创建系统配置")
    @SaCheckPermission("support:config:add")
    public ResponseDTO<String> create(@RequestBody @Valid PlatformConfigurationCreateCommand command) {
        return platformConfigurationFacade.create(command);
    }

    @PutMapping("/{configurationId}")
    @Operation(operationId = "platformConfigurationUpdate", summary = "更新系统配置")
    @SaCheckPermission("support:config:update")
    public ResponseDTO<String> update(
            @PathVariable Long configurationId,
            @RequestBody @Valid PlatformConfigurationUpdateCommand command) {
        return platformConfigurationFacade.update(configurationId, command);
    }
}
