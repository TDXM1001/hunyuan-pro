package com.hunyuan.sa.bpm.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.integration.domain.form.BpmCallbackRecordQueryForm;
import com.hunyuan.sa.bpm.module.integration.domain.form.BpmCommandRecordQueryForm;
import com.hunyuan.sa.bpm.module.integration.domain.vo.BpmCallbackRecordVO;
import com.hunyuan.sa.bpm.module.integration.domain.vo.BpmCommandRecordVO;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackService;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessIntegrationRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * BPM 业务集成可靠性监控接口。
 */
@RestController
@Tag(name = "BPM Integration")
public class AdminBpmIntegrationController {

    @Resource
    private BpmBusinessIntegrationRecordService bpmBusinessIntegrationRecordService;

    @Resource
    private BpmBusinessCallbackService bpmBusinessCallbackService;

    @Operation(summary = "分页查询 BPM 业务回调记录")
    @PostMapping("/bpm/integration/callback/query")
    @SaCheckPermission("bpm:integration:query")
    public ResponseDTO<PageResult<BpmCallbackRecordVO>> queryCallback(
            @RequestBody @Valid BpmCallbackRecordQueryForm queryForm
    ) {
        return bpmBusinessIntegrationRecordService.queryCallbackPage(queryForm);
    }

    @Operation(summary = "重试 BPM 业务回调")
    @PostMapping("/bpm/integration/callback/retry/{callbackRecordId}")
    @SaCheckPermission("bpm:integration:update")
    public ResponseDTO<String> retryCallback(@PathVariable Long callbackRecordId) {
        return bpmBusinessCallbackService.retry(callbackRecordId);
    }

    @Operation(summary = "分页查询 BPM 命令执行记录")
    @PostMapping("/bpm/integration/command/query")
    @SaCheckPermission("bpm:integration:query")
    public ResponseDTO<PageResult<BpmCommandRecordVO>> queryCommand(
            @RequestBody @Valid BpmCommandRecordQueryForm queryForm
    ) {
        return bpmBusinessIntegrationRecordService.queryCommandPage(queryForm);
    }
}
