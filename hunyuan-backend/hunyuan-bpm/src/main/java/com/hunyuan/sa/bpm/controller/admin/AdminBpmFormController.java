package com.hunyuan.sa.bpm.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.form.domain.form.BpmFormAddForm;
import com.hunyuan.sa.bpm.module.form.domain.form.BpmFormQueryForm;
import com.hunyuan.sa.bpm.module.form.domain.form.BpmFormUpdateForm;
import com.hunyuan.sa.bpm.module.form.domain.vo.BpmFormVO;
import com.hunyuan.sa.bpm.module.form.service.BpmFormService;
import org.springframework.web.bind.annotation.*;

/**
 * 流程表单管理接口。
 */
@RestController
@Tag(name = "BPM Form")
public class AdminBpmFormController {

    @Resource
    private BpmFormService bpmFormService;

    @Operation(summary = "分页查询流程表单")
    @PostMapping("/bpm/form/query")
    @SaCheckPermission("bpm:form:query")
    public ResponseDTO<PageResult<BpmFormVO>> query(@RequestBody @Valid BpmFormQueryForm queryForm) {
        return bpmFormService.queryForm(queryForm);
    }

    @Operation(summary = "新增流程表单")
    @PostMapping("/bpm/form/add")
    @SaCheckPermission("bpm:form:add")
    public ResponseDTO<String> add(@RequestBody @Valid BpmFormAddForm addForm) {
        return bpmFormService.addForm(addForm);
    }

    @Operation(summary = "更新流程表单")
    @PostMapping("/bpm/form/update")
    @SaCheckPermission("bpm:form:update")
    public ResponseDTO<String> update(@RequestBody @Valid BpmFormUpdateForm updateForm) {
        return bpmFormService.updateForm(updateForm);
    }

    @Operation(summary = "获取流程表单详情")
    @GetMapping("/bpm/form/detail/{formId}")
    @SaCheckPermission("bpm:form:detail")
    public ResponseDTO<BpmFormVO> detail(@PathVariable Long formId) {
        return bpmFormService.getFormDetail(formId);
    }
}
