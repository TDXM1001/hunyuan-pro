package com.hunyuan.sa.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.reload.api.PlatformRuntimeReloadFacade;
import com.hunyuan.sa.base.module.support.reload.api.PlatformRuntimeReloadItemView;
import com.hunyuan.sa.base.module.support.reload.api.PlatformRuntimeReloadResultView;
import com.hunyuan.sa.base.module.support.reload.api.PlatformRuntimeReloadUpdateCommand;
import com.hunyuan.sa.base.module.support.repeatsubmit.annoation.RepeatSubmit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台运行时重载稳定 HTTP 接口。
 */
@RestController
@RequestMapping("/api/admin/v1/platform/runtime/reloads")
@Tag(name = "平台运行时 - 重载管理")
public class PlatformRuntimeReloadController {

    @Resource
    private PlatformRuntimeReloadFacade platformRuntimeReloadFacade;

    @GetMapping
    @Operation(operationId = "platformRuntimeReloadList", summary = "查询运行时重载项")
    public ResponseDTO<List<PlatformRuntimeReloadItemView>> listItems() {
        return platformRuntimeReloadFacade.listItems();
    }

    @GetMapping("/{tag}/results")
    @Operation(operationId = "platformRuntimeReloadResultList", summary = "查询重载执行结果")
    @SaCheckPermission("support:reload:result")
    public ResponseDTO<List<PlatformRuntimeReloadResultView>> listResults(
            @PathVariable String tag) {
        return platformRuntimeReloadFacade.listResults(tag);
    }

    @PutMapping
    @Operation(operationId = "platformRuntimeReloadUpdate", summary = "更新运行时重载项")
    @SaCheckPermission("support:reload:update")
    @RepeatSubmit
    public ResponseDTO<String> updateItem(
            @RequestBody @Valid PlatformRuntimeReloadUpdateCommand command) {
        return platformRuntimeReloadFacade.updateItem(command);
    }
}
