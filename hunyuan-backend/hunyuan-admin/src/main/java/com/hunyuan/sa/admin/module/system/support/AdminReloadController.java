package com.hunyuan.sa.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.hunyuan.sa.base.common.controller.SupportBaseController;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.constant.SwaggerTagConst;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.module.support.reload.api.PlatformRuntimeReloadFacade;
import com.hunyuan.sa.base.module.support.reload.api.PlatformRuntimeReloadItemView;
import com.hunyuan.sa.base.module.support.reload.api.PlatformRuntimeReloadResultView;
import com.hunyuan.sa.base.module.support.reload.api.PlatformRuntimeReloadUpdateCommand;
import com.hunyuan.sa.base.module.support.reload.domain.ReloadForm;
import com.hunyuan.sa.base.module.support.reload.domain.ReloadItemVO;
import com.hunyuan.sa.base.module.support.reload.domain.ReloadResultVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * reload (内存热加载、钩子等)
 *
 * @Author 1024创新实验室-主任: 卓大
 * @Date 2015-03-02 19:11:52
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright  <a href="https://1024lab.net">1024创新实验室</a>
 */
@RestController
@Tag(name = SwaggerTagConst.Support.RELOAD)
public class AdminReloadController extends SupportBaseController {

    @Resource
    private PlatformRuntimeReloadFacade platformRuntimeReloadFacade;

    @Operation(summary = "查询reload列表 @author 开云")
    @GetMapping("/reload/query")
    public ResponseDTO<List<ReloadItemVO>> query() {
        ResponseDTO<List<PlatformRuntimeReloadItemView>> response =
                platformRuntimeReloadFacade.listItems();
        if (!response.getOk()) {
            return ResponseDTO.error(response);
        }
        return ResponseDTO.ok(SmartBeanUtil.copyList(response.getData(), ReloadItemVO.class));
    }

    @Operation(summary = "获取reload result @author 开云")
    @GetMapping("/reload/result/{tag}")
    @SaCheckPermission("support:reload:result")
    public ResponseDTO<List<ReloadResultVO>> queryReloadResult(@PathVariable("tag") String tag) {
        ResponseDTO<List<PlatformRuntimeReloadResultView>> response =
                platformRuntimeReloadFacade.listResults(tag);
        if (!response.getOk()) {
            return ResponseDTO.error(response);
        }
        return ResponseDTO.ok(SmartBeanUtil.copyList(response.getData(), ReloadResultVO.class));
    }

    @Operation(summary = "通过tag更新标识 @author 开云")
    @PostMapping("/reload/update")
    @SaCheckPermission("support:reload:update")
    public ResponseDTO<String> updateByTag(@RequestBody @Valid ReloadForm reloadForm) {
        return platformRuntimeReloadFacade.updateItem(
                SmartBeanUtil.copy(reloadForm, PlatformRuntimeReloadUpdateCommand.class));
    }
}
