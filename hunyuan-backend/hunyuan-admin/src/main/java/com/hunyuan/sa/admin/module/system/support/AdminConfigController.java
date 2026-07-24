package com.hunyuan.sa.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.hunyuan.sa.base.common.controller.SupportBaseController;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.constant.SwaggerTagConst;
import com.hunyuan.sa.base.module.support.config.api.PlatformConfigurationCreateCommand;
import com.hunyuan.sa.base.module.support.config.api.PlatformConfigurationFacade;
import com.hunyuan.sa.base.module.support.config.api.PlatformConfigurationPageQuery;
import com.hunyuan.sa.base.module.support.config.api.PlatformConfigurationSummary;
import com.hunyuan.sa.base.module.support.config.api.PlatformConfigurationUpdateCommand;
import com.hunyuan.sa.base.module.support.config.domain.ConfigAddForm;
import com.hunyuan.sa.base.module.support.config.domain.ConfigQueryForm;
import com.hunyuan.sa.base.module.support.config.domain.ConfigUpdateForm;
import com.hunyuan.sa.base.module.support.config.domain.ConfigVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 配置
 *
 * @Author 1024创新实验室-主任: 卓大
 * @Date 2022-03-14 20:46:27
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright  <a href="https://1024lab.net">1024创新实验室</a>
 */
@Tag(name = SwaggerTagConst.Support.CONFIG)
@RestController
public class AdminConfigController extends SupportBaseController {

    @Resource
    private PlatformConfigurationFacade platformConfigurationFacade;

    @Operation(summary = "分页查询系统配置 @author 卓大")
    @PostMapping("/config/query")
    @SaCheckPermission("support:config:query")
    public ResponseDTO<PageResult<ConfigVO>> queryConfigPage(@RequestBody @Valid ConfigQueryForm queryForm) {
        PlatformConfigurationPageQuery query = SmartBeanUtil.copy(queryForm, PlatformConfigurationPageQuery.class);
        ResponseDTO<PageResult<PlatformConfigurationSummary>> response = platformConfigurationFacade.queryPage(query);
        if (!Boolean.TRUE.equals(response.getOk())) {
            return ResponseDTO.error(response);
        }
        PageResult<PlatformConfigurationSummary> result = response.getData();
        PageResult<ConfigVO> legacyResult = new PageResult<>();
        legacyResult.setPageNum(result.getPageNum());
        legacyResult.setPageSize(result.getPageSize());
        legacyResult.setTotal(result.getTotal());
        legacyResult.setPages(result.getPages());
        legacyResult.setEmptyFlag(result.getEmptyFlag());
        legacyResult.setList(SmartBeanUtil.copyList(result.getList(), ConfigVO.class));
        return ResponseDTO.ok(legacyResult);
    }

    @Operation(summary = "添加配置参数 @author 卓大")
    @PostMapping("/config/add")
    @SaCheckPermission("support:config:add")
    public ResponseDTO<String> addConfig(@RequestBody @Valid ConfigAddForm configAddForm) {
        PlatformConfigurationCreateCommand command = SmartBeanUtil.copy(
                configAddForm, PlatformConfigurationCreateCommand.class);
        return platformConfigurationFacade.create(command);
    }

    @Operation(summary = "修改配置参数 @author 卓大")
    @PostMapping("/config/update")
    @SaCheckPermission("support:config:update")
    public ResponseDTO<String> updateConfig(@RequestBody @Valid ConfigUpdateForm updateForm) {
        PlatformConfigurationUpdateCommand command = SmartBeanUtil.copy(
                updateForm, PlatformConfigurationUpdateCommand.class);
        return platformConfigurationFacade.update(updateForm.getConfigId(), command);
    }

}
