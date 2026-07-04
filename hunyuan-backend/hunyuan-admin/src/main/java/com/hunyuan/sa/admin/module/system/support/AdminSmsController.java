package com.hunyuan.sa.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.controller.SupportBaseController;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.sms.domain.SmsSendLogQueryForm;
import com.hunyuan.sa.base.module.support.sms.domain.SmsSendLogVO;
import com.hunyuan.sa.base.module.support.sms.domain.SmsTemplateAddForm;
import com.hunyuan.sa.base.module.support.sms.domain.SmsTemplateQueryForm;
import com.hunyuan.sa.base.module.support.sms.domain.SmsTemplateUpdateForm;
import com.hunyuan.sa.base.module.support.sms.domain.SmsTemplateVO;
import com.hunyuan.sa.base.module.support.sms.service.SmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin SMS management.
 */
@RestController
@Tag(name = "SMS")
public class AdminSmsController extends SupportBaseController {

    @Resource
    private SmsService smsService;

    @Operation(summary = "Query SMS templates")
    @PostMapping("/sms/template/query")
    @SaCheckPermission("support:sms:template:query")
    public ResponseDTO<PageResult<SmsTemplateVO>> queryTemplate(@RequestBody @Valid SmsTemplateQueryForm queryForm) {
        return smsService.queryTemplate(queryForm);
    }

    @Operation(summary = "Add SMS template")
    @PostMapping("/sms/template/add")
    @SaCheckPermission("support:sms:template:add")
    public ResponseDTO<String> addTemplate(@RequestBody @Valid SmsTemplateAddForm addForm) {
        return smsService.addTemplate(addForm);
    }

    @Operation(summary = "Update SMS template")
    @PostMapping("/sms/template/update")
    @SaCheckPermission("support:sms:template:update")
    public ResponseDTO<String> updateTemplate(@RequestBody @Valid SmsTemplateUpdateForm updateForm) {
        return smsService.updateTemplate(updateForm);
    }

    @Operation(summary = "Enable or disable SMS template")
    @GetMapping("/sms/template/updateDisabled/{templateCode}/{disableFlag}")
    @SaCheckPermission("support:sms:template:update")
    public ResponseDTO<String> updateTemplateDisabled(@PathVariable String templateCode, @PathVariable Boolean disableFlag) {
        return smsService.updateTemplateDisabled(templateCode, disableFlag);
    }

    @Operation(summary = "Query SMS send logs")
    @PostMapping("/sms/sendLog/query")
    @SaCheckPermission("support:sms:sendLog:query")
    public ResponseDTO<PageResult<SmsSendLogVO>> querySendLog(@RequestBody @Valid SmsSendLogQueryForm queryForm) {
        return ResponseDTO.ok(smsService.querySendLog(queryForm));
    }
}
