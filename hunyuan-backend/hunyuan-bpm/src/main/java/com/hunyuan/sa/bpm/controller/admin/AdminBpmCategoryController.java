package com.hunyuan.sa.bpm.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.category.domain.form.BpmCategoryAddForm;
import com.hunyuan.sa.bpm.module.category.domain.form.BpmCategoryQueryForm;
import com.hunyuan.sa.bpm.module.category.domain.form.BpmCategoryUpdateForm;
import com.hunyuan.sa.bpm.module.category.domain.vo.BpmCategoryVO;
import com.hunyuan.sa.bpm.module.category.service.BpmCategoryService;
import org.springframework.web.bind.annotation.*;

/**
 * 流程分类管理接口。
 */
@RestController
@Tag(name = "BPM Category")
public class AdminBpmCategoryController {

    @Resource
    private BpmCategoryService bpmCategoryService;

    @Operation(summary = "分页查询流程分类")
    @PostMapping("/bpm/category/query")
    @SaCheckPermission("bpm:category:query")
    public ResponseDTO<PageResult<BpmCategoryVO>> query(@RequestBody @Valid BpmCategoryQueryForm queryForm) {
        return bpmCategoryService.queryCategory(queryForm);
    }

    @Operation(summary = "新增流程分类")
    @PostMapping("/bpm/category/add")
    @SaCheckPermission("bpm:category:add")
    public ResponseDTO<String> add(@RequestBody @Valid BpmCategoryAddForm addForm) {
        return bpmCategoryService.addCategory(addForm);
    }

    @Operation(summary = "更新流程分类")
    @PostMapping("/bpm/category/update")
    @SaCheckPermission("bpm:category:update")
    public ResponseDTO<String> update(@RequestBody @Valid BpmCategoryUpdateForm updateForm) {
        return bpmCategoryService.updateCategory(updateForm);
    }

    @Operation(summary = "获取流程分类详情")
    @GetMapping("/bpm/category/detail/{categoryId}")
    @SaCheckPermission("bpm:category:detail")
    public ResponseDTO<BpmCategoryVO> detail(@PathVariable Long categoryId) {
        return bpmCategoryService.getCategoryDetail(categoryId);
    }
}
