package com.hunyuan.sa.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.sms.api.PlatformSmsFacade;
import com.hunyuan.sa.base.module.support.sms.api.PlatformSmsSendLogPageQuery;
import com.hunyuan.sa.base.module.support.sms.api.PlatformSmsSendLogSummary;
import com.hunyuan.sa.base.module.support.sms.api.PlatformSmsTemplateCreateCommand;
import com.hunyuan.sa.base.module.support.sms.api.PlatformSmsTemplateDisabledCommand;
import com.hunyuan.sa.base.module.support.sms.api.PlatformSmsTemplatePageQuery;
import com.hunyuan.sa.base.module.support.sms.api.PlatformSmsTemplateSummary;
import com.hunyuan.sa.base.module.support.sms.api.PlatformSmsTemplateUpdateCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台短信管理稳定 HTTP 接口。
 */
@RestController
@RequestMapping("/api/admin/v1/platform/notifications/sms")
@Tag(name = "平台能力 - 短信管理")
public class PlatformSmsController {

    @Resource
    private PlatformSmsFacade platformSmsFacade;

    @PostMapping("/templates/query")
    @Operation(operationId = "platformSmsTemplateQuery", summary = "分页查询短信模板")
    @SaCheckPermission("support:sms:template:query")
    public ResponseDTO<PageResult<PlatformSmsTemplateSummary>> queryTemplates(
            @RequestBody @Valid PlatformSmsTemplatePageQuery query) {
        return platformSmsFacade.queryTemplates(query);
    }

    @PostMapping("/templates")
    @Operation(operationId = "platformSmsTemplateCreate", summary = "创建短信模板")
    @SaCheckPermission("support:sms:template:add")
    public ResponseDTO<String> createTemplate(
            @RequestBody @Valid PlatformSmsTemplateCreateCommand command) {
        return platformSmsFacade.createTemplate(command);
    }

    @PutMapping("/templates/{templateCode}")
    @Operation(operationId = "platformSmsTemplateUpdate", summary = "更新短信模板")
    @SaCheckPermission("support:sms:template:update")
    public ResponseDTO<String> updateTemplate(
            @PathVariable String templateCode,
            @RequestBody @Valid PlatformSmsTemplateUpdateCommand command) {
        return platformSmsFacade.updateTemplate(templateCode, command);
    }

    @PutMapping("/templates/{templateCode}/disabled")
    @Operation(operationId = "platformSmsTemplateDisabledUpdate", summary = "修改短信模板启停状态")
    @SaCheckPermission("support:sms:template:update")
    public ResponseDTO<String> updateTemplateDisabled(
            @PathVariable String templateCode,
            @RequestBody @Valid PlatformSmsTemplateDisabledCommand command) {
        return platformSmsFacade.updateTemplateDisabled(
                templateCode, command.getDisableFlag());
    }

    @PostMapping("/send-logs/query")
    @Operation(operationId = "platformSmsSendLogQuery", summary = "分页查询短信发送记录")
    @SaCheckPermission("support:sms:sendLog:query")
    public ResponseDTO<PageResult<PlatformSmsSendLogSummary>> querySendLogs(
            @RequestBody @Valid PlatformSmsSendLogPageQuery query) {
        return platformSmsFacade.querySendLogs(query);
    }
}
