package com.hunyuan.sa.bpm.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphDraftCreateCommand;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphDraftCreateForm;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphDraftImportCommand;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphDraftImportForm;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphDraftQueryForm;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphDraftSaveCommand;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphDraftSaveForm;
import com.hunyuan.sa.bpm.module.model.domain.vo.BpmGraphDraftVO;
import com.hunyuan.sa.bpm.module.model.service.BpmGraphDraftService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 正式 Graph 作者草稿管理接口。
 */
@RestController
@Tag(name = "BPM Graph Draft")
public class AdminBpmGraphDraftController {

    @Resource
    private BpmGraphDraftService bpmGraphDraftService;

    @Resource
    private BpmCurrentActorProvider bpmCurrentActorProvider;

    @Operation(summary = "分页查询 Graph 流程草稿")
    @PostMapping("/bpm/graph-draft/query")
    @SaCheckPermission("bpm:graph-draft:query")
    public ResponseDTO<PageResult<BpmGraphDraftVO>> query(@RequestBody @Valid BpmGraphDraftQueryForm form) {
        return bpmGraphDraftService.queryDrafts(form);
    }

    @Operation(summary = "创建 Graph 流程草稿")
    @PostMapping("/bpm/graph-draft/create")
    @SaCheckPermission("bpm:graph-draft:add")
    public ResponseDTO<BpmGraphDraftVO> create(@RequestBody @Valid BpmGraphDraftCreateForm form) {
        return bpmGraphDraftService.createDraft(new BpmGraphDraftCreateCommand(
                form.getProcessKey(),
                form.getProcessName(),
                form.getCategoryId(),
                form.getGraph(),
                bpmCurrentActorProvider.requireCurrentEmployeeId()
        ));
    }

    @Operation(summary = "条件保存 Graph 流程草稿")
    @PostMapping("/bpm/graph-draft/save")
    @SaCheckPermission("bpm:graph-draft:update")
    public ResponseDTO<BpmGraphDraftVO> save(@RequestBody @Valid BpmGraphDraftSaveForm form) {
        return bpmGraphDraftService.saveDraft(new BpmGraphDraftSaveCommand(
                form.getDraftId(),
                form.getRevision(),
                form.getGraph(),
                bpmCurrentActorProvider.requireCurrentEmployeeId()
        ));
    }

    @Operation(summary = "读取 Graph 流程草稿")
    @GetMapping("/bpm/graph-draft/detail/{draftId}")
    @SaCheckPermission("bpm:graph-draft:detail")
    public ResponseDTO<BpmGraphDraftVO> detail(@PathVariable Long draftId) {
        return bpmGraphDraftService.getDraft(draftId);
    }

    @Operation(summary = "导出完整 Graph 流程草稿")
    @GetMapping("/bpm/graph-draft/export/{draftId}")
    @SaCheckPermission("bpm:graph-draft:detail")
    public ResponseDTO<String> export(@PathVariable Long draftId) {
        return bpmGraphDraftService.exportDraft(draftId);
    }

    @Operation(summary = "导入为新的 Graph 流程草稿")
    @PostMapping("/bpm/graph-draft/import")
    @SaCheckPermission("bpm:graph-draft:add")
    public ResponseDTO<BpmGraphDraftVO> importDraft(@RequestBody @Valid BpmGraphDraftImportForm form) {
        return bpmGraphDraftService.importDraft(new BpmGraphDraftImportCommand(
                form.getProcessKey(),
                form.getProcessName(),
                form.getCategoryId(),
                form.getGraphDocumentJson(),
                bpmCurrentActorProvider.requireCurrentEmployeeId()
        ));
    }
}
