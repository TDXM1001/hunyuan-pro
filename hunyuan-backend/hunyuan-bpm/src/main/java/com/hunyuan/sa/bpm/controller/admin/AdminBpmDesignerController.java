package com.hunyuan.sa.bpm.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmDesignerSaveForm;
import com.hunyuan.sa.bpm.module.model.domain.vo.BpmDesignerDetailVO;
import com.hunyuan.sa.bpm.module.model.service.BpmDesignerService;
import org.springframework.web.bind.annotation.*;

/**
 * 流程设计器管理接口。
 */
@RestController
@Tag(name = "BPM Designer")
public class AdminBpmDesignerController {

    @Resource
    private BpmDesignerService bpmDesignerService;

    @Operation(summary = "获取流程设计器详情")
    @GetMapping("/bpm/designer/detail/{modelId}")
    @SaCheckPermission("bpm:designer:detail")
    public ResponseDTO<BpmDesignerDetailVO> detail(@PathVariable Long modelId) {
        return bpmDesignerService.getDesignerDetail(modelId);
    }

    @Operation(summary = "保存流程设计器草稿")
    @PostMapping("/bpm/designer/save")
    @SaCheckPermission("bpm:designer:update")
    public ResponseDTO<String> save(@RequestBody @Valid BpmDesignerSaveForm saveForm) {
        return bpmDesignerService.saveDesignerDraft(saveForm);
    }

    @Operation(summary = "校验流程设计器草稿")
    @GetMapping("/bpm/designer/validate/{modelId}")
    @SaCheckPermission("bpm:designer:validate")
    public ResponseDTO<String> validate(@PathVariable Long modelId) {
        return bpmDesignerService.validateDesignerDraft(modelId);
    }

    @Operation(summary = "模拟流程设计器草稿")
    @GetMapping("/bpm/designer/simulate/{modelId}")
    @SaCheckPermission("bpm:designer:simulate")
    public ResponseDTO<String> simulate(@PathVariable Long modelId) {
        return bpmDesignerService.simulateDesignerDraft(modelId);
    }
}
