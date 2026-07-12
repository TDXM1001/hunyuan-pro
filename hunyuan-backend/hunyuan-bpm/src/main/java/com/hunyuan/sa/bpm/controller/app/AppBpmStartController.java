package com.hunyuan.sa.bpm.controller.app;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceStartForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmRuntimeStartDraftVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmStartableDefinitionVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 员工端流程发起接口。
 */
@RestController
@Tag(name = "App BPM Start")
public class AppBpmStartController {

    @Resource
    private BpmInstanceService bpmInstanceService;

    @Operation(summary = "查询我可发起的流程")
    @GetMapping("/app/bpm/startable")
    public ResponseDTO<List<BpmStartableDefinitionVO>> queryStartableDefinitions() {
        return bpmInstanceService.queryStartableDefinitions();
    }

    @Operation(summary = "查询发起草稿")
    @GetMapping("/app/bpm/start-draft/{definitionId}")
    public ResponseDTO<BpmRuntimeStartDraftVO> startDraft(@PathVariable Long definitionId) {
        return bpmInstanceService.getStartDraft(definitionId);
    }

    @Operation(summary = "查询 Graph 发起草稿")
    @GetMapping("/app/bpm/graph-start-draft/{graphDefinitionVersionId}")
    public ResponseDTO<BpmRuntimeStartDraftVO> graphStartDraft(@PathVariable Long graphDefinitionVersionId) {
        return bpmInstanceService.getGraphStartDraft(graphDefinitionVersionId);
    }

    @Operation(summary = "发起流程实例")
    @PostMapping("/app/bpm/start")
    public ResponseDTO<Long> start(@RequestBody @Valid BpmInstanceStartForm startForm) {
        return bpmInstanceService.startInstance(startForm);
    }
}
