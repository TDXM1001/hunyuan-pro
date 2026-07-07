package com.hunyuan.sa.bpm.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskQueryForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流程任务管理接口。
 */
@RestController
@Tag(name = "BPM Task")
public class AdminBpmTaskController {

    @Resource
    private BpmTaskService bpmTaskService;

    @Operation(summary = "分页查询流程任务")
    @PostMapping("/bpm/task/query")
    @SaCheckPermission("bpm:task:query")
    public ResponseDTO<PageResult<BpmTaskVO>> query(@RequestBody @Valid BpmTaskQueryForm queryForm) {
        return bpmTaskService.queryAdminPage(queryForm);
    }
}
