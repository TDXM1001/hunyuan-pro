package com.hunyuan.sa.bpm.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmModelAddForm;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmModelQueryForm;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmModelUpdateForm;
import com.hunyuan.sa.bpm.module.model.domain.vo.BpmModelVO;
import com.hunyuan.sa.bpm.module.model.service.BpmModelService;
import com.hunyuan.sa.bpm.engine.route.BpmRouteExpressionDescriptor;
import com.hunyuan.sa.bpm.engine.route.BpmRouteExpressionRegistry;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程模型管理接口。
 */
@RestController
@Tag(name = "BPM Model")
public class AdminBpmModelController {

    @Resource
    private BpmModelService bpmModelService;

    @Resource
    private BpmRouteExpressionRegistry bpmRouteExpressionRegistry;

    @Operation(summary = "分页查询流程模型")
    @PostMapping("/bpm/model/query")
    @SaCheckPermission("bpm:model:query")
    public ResponseDTO<PageResult<BpmModelVO>> query(@RequestBody @Valid BpmModelQueryForm queryForm) {
        return bpmModelService.queryModel(queryForm);
    }

    @Operation(summary = "新增流程模型")
    @PostMapping("/bpm/model/add")
    @SaCheckPermission("bpm:model:add")
    public ResponseDTO<String> add(@RequestBody @Valid BpmModelAddForm addForm) {
        return bpmModelService.addModel(addForm);
    }

    @Operation(summary = "更新流程模型")
    @PostMapping("/bpm/model/update")
    @SaCheckPermission("bpm:model:update")
    public ResponseDTO<String> update(@RequestBody @Valid BpmModelUpdateForm updateForm) {
        return bpmModelService.updateModel(updateForm);
    }

    @Operation(summary = "获取流程模型详情")
    @GetMapping("/bpm/model/detail/{modelId}")
    @SaCheckPermission("bpm:model:detail")
    public ResponseDTO<BpmModelVO> detail(@PathVariable Long modelId) {
        return bpmModelService.getModelDetail(modelId);
    }

    @Operation(summary = "查询登记路由表达式目录")
    @GetMapping("/bpm/model/route-expression/catalog")
    @SaCheckPermission("bpm:model:detail")
    public ResponseDTO<List<BpmRouteExpressionDescriptor>> routeExpressionCatalog() {
        return ResponseDTO.ok(bpmRouteExpressionRegistry.listDescriptors());
    }
}
