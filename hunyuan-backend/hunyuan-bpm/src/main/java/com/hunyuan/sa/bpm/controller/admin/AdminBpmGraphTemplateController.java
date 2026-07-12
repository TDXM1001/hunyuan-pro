package com.hunyuan.sa.bpm.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphTemplateCopyCommand;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphTemplateCopyForm;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphTemplateCreateCommand;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphTemplateCreateForm;
import com.hunyuan.sa.bpm.module.model.domain.vo.BpmGraphDraftVO;
import com.hunyuan.sa.bpm.module.model.domain.vo.BpmGraphTemplateVO;
import com.hunyuan.sa.bpm.module.model.service.BpmGraphTemplateService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 正式 Graph 模板冻结与复制接口。
 */
@RestController
@Tag(name = "BPM Graph Template")
public class AdminBpmGraphTemplateController {

    @Resource
    private BpmGraphTemplateService bpmGraphTemplateService;

    @Resource
    private BpmCurrentActorProvider bpmCurrentActorProvider;

    @Operation(summary = "从草稿冻结 Graph 模板")
    @PostMapping("/bpm/graph-template/create")
    @SaCheckPermission("bpm:graph-template:add")
    public ResponseDTO<BpmGraphTemplateVO> create(@RequestBody @Valid BpmGraphTemplateCreateForm form) {
        return bpmGraphTemplateService.createTemplate(new BpmGraphTemplateCreateCommand(
                form.getTemplateKey(),
                form.getTemplateName(),
                form.getSourceDraftId(),
                bpmCurrentActorProvider.requireCurrentEmployeeId()
        ));
    }

    @Operation(summary = "从 Graph 模板复制新草稿")
    @PostMapping("/bpm/graph-template/copy")
    @SaCheckPermission("bpm:graph-template:copy")
    public ResponseDTO<BpmGraphDraftVO> copy(@RequestBody @Valid BpmGraphTemplateCopyForm form) {
        return bpmGraphTemplateService.copyTemplate(new BpmGraphTemplateCopyCommand(
                form.getTemplateId(),
                form.getProcessKey(),
                form.getProcessName(),
                form.getCategoryId(),
                bpmCurrentActorProvider.requireCurrentEmployeeId()
        ));
    }
}
