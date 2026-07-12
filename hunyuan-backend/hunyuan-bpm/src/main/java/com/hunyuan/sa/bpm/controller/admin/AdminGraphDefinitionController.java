package com.hunyuan.sa.bpm.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.module.definition.domain.form.GraphDefinitionPublishCommand;
import com.hunyuan.sa.bpm.module.definition.domain.form.GraphDefinitionPublishForm;
import com.hunyuan.sa.bpm.module.definition.domain.vo.GraphDefinitionDetailVO;
import com.hunyuan.sa.bpm.module.definition.service.GraphDefinitionPublicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "BPM Graph Definition")
public class AdminGraphDefinitionController {

    @Resource
    private GraphDefinitionPublicationService graphDefinitionPublicationService;

    @Resource
    private BpmCurrentActorProvider bpmCurrentActorProvider;

    @Operation(summary = "发布 Graph 定义")
    @PostMapping("/bpm/graph-definition/publish")
    @SaCheckPermission("bpm:graph-definition:publish")
    public ResponseDTO<Long> publish(@RequestBody @Valid GraphDefinitionPublishForm form) {
        return ResponseDTO.ok(graphDefinitionPublicationService.publish(new GraphDefinitionPublishCommand(
                form.getDraftId(), bpmCurrentActorProvider.requireCurrentEmployeeId()
        )));
    }

    @Operation(summary = "查看 Graph 定义版本")
    @GetMapping("/bpm/graph-definition/detail/{versionId}")
    @SaCheckPermission("bpm:graph-definition:detail")
    public ResponseDTO<GraphDefinitionDetailVO> detail(@PathVariable Long versionId) {
        return graphDefinitionPublicationService.getDefinitionDetail(versionId);
    }

    @Operation(summary = "查看草稿最新 Graph 定义版本")
    @GetMapping("/bpm/graph-definition/latest-by-draft/{draftId}")
    @SaCheckPermission("bpm:graph-definition:detail")
    public ResponseDTO<GraphDefinitionDetailVO> latestByDraft(@PathVariable Long draftId) {
        return graphDefinitionPublicationService.getLatestDefinitionDetail(draftId);
    }

    @Operation(summary = "下线 Graph 定义")
    @PostMapping("/bpm/graph-definition/deactivate/{versionId}")
    @SaCheckPermission("bpm:graph-definition:deactivate")
    public ResponseDTO<String> deactivate(@PathVariable Long versionId) {
        graphDefinitionPublicationService.deactivate(versionId);
        return ResponseDTO.ok();
    }
}
