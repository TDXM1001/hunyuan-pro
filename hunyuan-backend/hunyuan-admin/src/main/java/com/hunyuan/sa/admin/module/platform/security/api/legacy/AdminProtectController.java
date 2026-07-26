package com.hunyuan.sa.admin.module.platform.security.api.legacy;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.hunyuan.sa.base.common.controller.SupportBaseController;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.domain.ValidateList;
import com.hunyuan.sa.base.constant.SwaggerTagConst;
import com.hunyuan.sa.base.module.support.config.api.PlatformConfigurationValueReader;
import com.hunyuan.sa.admin.module.platform.security.protection.domain.Level3ProtectConfigForm;
import com.hunyuan.sa.admin.module.platform.security.protection.domain.LoginFailQueryForm;
import com.hunyuan.sa.admin.module.platform.security.protection.domain.LoginFailVO;
import com.hunyuan.sa.admin.module.platform.security.protection.service.Level3ProtectConfigService;
import com.hunyuan.sa.admin.module.platform.security.protection.service.SecurityLoginService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 网络安全
 *
 * @Author 1024创新实验室-主任:卓大
 * @Date 2023/10/17 19:07:27
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>，Since 2012
 */

@RestController
@Tag(name = SwaggerTagConst.Support.PROTECT)
public class AdminProtectController extends SupportBaseController {

    private static final String LEVEL3_PROTECT_CONFIGURATION_KEY = "level3_protect_config";

    @Resource
    private SecurityLoginService securityLoginService;

    @Resource
    private Level3ProtectConfigService level3ProtectConfigService;

    @Resource
    private PlatformConfigurationValueReader configurationValueReader;


    @Operation(summary = "分页查询 @author 1024创新实验室-主任-卓大")
    @PostMapping("/protect/loginFail/queryPage")
    @SaCheckPermission("support:protect:loginFail:query")
    public ResponseDTO<PageResult<LoginFailVO>> queryPage(@RequestBody @Valid LoginFailQueryForm queryForm) {
        return ResponseDTO.ok(securityLoginService.queryPage(queryForm));
    }


    @Operation(summary = "批量删除 @author 1024创新实验室-主任-卓大")
    @PostMapping("/protect/loginFail/batchDelete")
    @SaCheckPermission("support:protect:loginFail:delete")
    public ResponseDTO<String> batchDelete(@RequestBody ValidateList<Long> idList) {
        return securityLoginService.batchDelete(idList);
    }

    @Operation(summary = "更新三级等保配置 @author 1024创新实验室-主任-卓大")
    @PostMapping("/protect/level3protect/updateConfig")
    @SaCheckPermission("support:protect:level3:update")
    public ResponseDTO<String> updateConfig(@RequestBody @Valid Level3ProtectConfigForm configForm) {
        return level3ProtectConfigService.updateLevel3Config(configForm);
    }

    @Operation(summary = "查询 三级等保配置 @author 1024创新实验室-主任-卓大")
    @GetMapping("/protect/level3protect/getConfig")
    @SaCheckPermission("support:protect:level3:query")
    public ResponseDTO<String> getConfig() {
        return ResponseDTO.ok(configurationValueReader.getValue(LEVEL3_PROTECT_CONFIGURATION_KEY));
    }
}
