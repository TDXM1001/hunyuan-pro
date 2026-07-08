package com.hunyuan.sa.bpm.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.definition.domain.form.BpmDefinitionPublishForm;
import com.hunyuan.sa.bpm.module.definition.domain.form.BpmDefinitionQueryForm;
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionDiffVO;
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionDetailVO;
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionValidationReportVO;
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionVO;
import com.hunyuan.sa.bpm.module.definition.service.BpmDefinitionService;
import org.springframework.web.bind.annotation.*;

/**
 * 流程定义管理接口。
 */
@RestController
@Tag(name = "BPM Definition")
public class AdminBpmDefinitionController {

    @Resource
    private BpmDefinitionService bpmDefinitionService;

    @Operation(summary = "发布流程定义")
    @PostMapping("/bpm/definition/publish")
    @SaCheckPermission("bpm:definition:publish")
    public ResponseDTO<Long> publish(@RequestBody @Valid BpmDefinitionPublishForm publishForm) {
        return bpmDefinitionService.publish(publishForm);
    }

    @Operation(summary = "发布前校验流程定义")
    @GetMapping("/bpm/definition/validateForPublish/{modelId}")
    @SaCheckPermission("bpm:definition:publish")
    public ResponseDTO<BpmDefinitionValidationReportVO> validateForPublish(@PathVariable Long modelId) {
        return bpmDefinitionService.validateForPublish(modelId);
    }

    @Operation(summary = "预览流程定义发布差异")
    @GetMapping("/bpm/definition/publishDiff/{modelId}")
    @SaCheckPermission("bpm:definition:publish")
    public ResponseDTO<BpmDefinitionDiffVO> publishDiff(@PathVariable Long modelId) {
        return bpmDefinitionService.previewPublishDiff(modelId);
    }

    @Operation(summary = "分页查询流程定义")
    @PostMapping("/bpm/definition/query")
    @SaCheckPermission("bpm:definition:query")
    public ResponseDTO<PageResult<BpmDefinitionVO>> query(@RequestBody @Valid BpmDefinitionQueryForm queryForm) {
        return bpmDefinitionService.query(queryForm);
    }

    @Operation(summary = "获取流程定义详情")
    @GetMapping("/bpm/definition/detail/{definitionId}")
    @SaCheckPermission("bpm:definition:detail")
    public ResponseDTO<BpmDefinitionDetailVO> detail(@PathVariable Long definitionId) {
        return bpmDefinitionService.getDetail(definitionId);
    }
}
