package com.hunyuan.sa.admin.module.platform.runtime.reload.application;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.admin.module.platform.runtime.reload.ReloadService;
import com.hunyuan.sa.base.module.support.reload.api.PlatformRuntimeReloadFacade;
import com.hunyuan.sa.base.module.support.reload.api.PlatformRuntimeReloadItemView;
import com.hunyuan.sa.base.module.support.reload.api.PlatformRuntimeReloadResultView;
import com.hunyuan.sa.base.module.support.reload.api.PlatformRuntimeReloadUpdateCommand;
import com.hunyuan.sa.admin.module.platform.runtime.reload.domain.ReloadForm;
import com.hunyuan.sa.admin.module.platform.runtime.reload.domain.ReloadItemVO;
import com.hunyuan.sa.admin.module.platform.runtime.reload.domain.ReloadResultVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 平台运行时重载用例实现，仅映射公开模型并复用既有重载服务。
 */
@Service
public class PlatformRuntimeReloadApplicationService implements PlatformRuntimeReloadFacade {

    @Resource
    private ReloadService reloadService;

    @Override
    public ResponseDTO<List<PlatformRuntimeReloadItemView>> listItems() {
        ResponseDTO<List<ReloadItemVO>> response = reloadService.query();
        if (!response.getOk()) {
            return ResponseDTO.error(response);
        }
        return ResponseDTO.ok(SmartBeanUtil.copyList(
                response.getData(), PlatformRuntimeReloadItemView.class));
    }

    @Override
    public ResponseDTO<List<PlatformRuntimeReloadResultView>> listResults(String tag) {
        ResponseDTO<List<ReloadResultVO>> response = reloadService.queryReloadItemResult(tag);
        if (!response.getOk()) {
            return ResponseDTO.error(response);
        }
        return ResponseDTO.ok(SmartBeanUtil.copyList(
                response.getData(), PlatformRuntimeReloadResultView.class));
    }

    @Override
    public ResponseDTO<String> updateItem(PlatformRuntimeReloadUpdateCommand command) {
        return reloadService.updateByTag(SmartBeanUtil.copy(command, ReloadForm.class));
    }
}
