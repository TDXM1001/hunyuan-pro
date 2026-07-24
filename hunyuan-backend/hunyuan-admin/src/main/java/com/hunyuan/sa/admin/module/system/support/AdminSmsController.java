package com.hunyuan.sa.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.controller.SupportBaseController;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.module.support.sms.api.PlatformSmsFacade;
import com.hunyuan.sa.base.module.support.sms.api.PlatformSmsSendLogPageQuery;
import com.hunyuan.sa.base.module.support.sms.api.PlatformSmsSendLogSummary;
import com.hunyuan.sa.base.module.support.sms.api.PlatformSmsTemplateCreateCommand;
import com.hunyuan.sa.base.module.support.sms.api.PlatformSmsTemplatePageQuery;
import com.hunyuan.sa.base.module.support.sms.api.PlatformSmsTemplateSummary;
import com.hunyuan.sa.base.module.support.sms.api.PlatformSmsTemplateUpdateCommand;
import com.hunyuan.sa.base.module.support.sms.domain.SmsSendLogQueryForm;
import com.hunyuan.sa.base.module.support.sms.domain.SmsSendLogVO;
import com.hunyuan.sa.base.module.support.sms.domain.SmsTemplateAddForm;
import com.hunyuan.sa.base.module.support.sms.domain.SmsTemplateQueryForm;
import com.hunyuan.sa.base.module.support.sms.domain.SmsTemplateUpdateForm;
import com.hunyuan.sa.base.module.support.sms.domain.SmsTemplateVO;
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
 * 历史短信管理接口，通过平台短信公开边界保持兼容。
 */
@RestController
@Tag(name = "SMS")
public class AdminSmsController extends SupportBaseController {

    @Resource
    private PlatformSmsFacade platformSmsFacade;

    @Operation(summary = "Query SMS templates")
    @PostMapping("/sms/template/query")
    @SaCheckPermission("support:sms:template:query")
    public ResponseDTO<PageResult<SmsTemplateVO>> queryTemplate(@RequestBody @Valid SmsTemplateQueryForm queryForm) {
        ResponseDTO<PageResult<PlatformSmsTemplateSummary>> response =
                platformSmsFacade.queryTemplates(SmartBeanUtil.copy(
                        queryForm, PlatformSmsTemplatePageQuery.class));
        return mapPageResponse(response, SmsTemplateVO.class);
    }

    @Operation(summary = "Add SMS template")
    @PostMapping("/sms/template/add")
    @SaCheckPermission("support:sms:template:add")
    public ResponseDTO<String> addTemplate(@RequestBody @Valid SmsTemplateAddForm addForm) {
        return platformSmsFacade.createTemplate(SmartBeanUtil.copy(
                addForm, PlatformSmsTemplateCreateCommand.class));
    }

    @Operation(summary = "Update SMS template")
    @PostMapping("/sms/template/update")
    @SaCheckPermission("support:sms:template:update")
    public ResponseDTO<String> updateTemplate(@RequestBody @Valid SmsTemplateUpdateForm updateForm) {
        return platformSmsFacade.updateTemplate(
                updateForm.getTemplateCode(),
                SmartBeanUtil.copy(updateForm, PlatformSmsTemplateUpdateCommand.class));
    }

    @Operation(summary = "Enable or disable SMS template")
    @GetMapping("/sms/template/updateDisabled/{templateCode}/{disableFlag}")
    @SaCheckPermission("support:sms:template:update")
    public ResponseDTO<String> updateTemplateDisabled(@PathVariable String templateCode, @PathVariable Boolean disableFlag) {
        return platformSmsFacade.updateTemplateDisabled(templateCode, disableFlag);
    }

    @Operation(summary = "Query SMS send logs")
    @PostMapping("/sms/sendLog/query")
    @SaCheckPermission("support:sms:sendLog:query")
    public ResponseDTO<PageResult<SmsSendLogVO>> querySendLog(@RequestBody @Valid SmsSendLogQueryForm queryForm) {
        ResponseDTO<PageResult<PlatformSmsSendLogSummary>> response =
                platformSmsFacade.querySendLogs(SmartBeanUtil.copy(
                        queryForm, PlatformSmsSendLogPageQuery.class));
        return mapPageResponse(response, SmsSendLogVO.class);
    }

    /**
     * 将平台分页对象还原为历史响应对象，保持旧接口契约不变。
     */
    private <S, T> ResponseDTO<PageResult<T>> mapPageResponse(
            ResponseDTO<PageResult<S>> response, Class<T> targetType) {
        if (!Boolean.TRUE.equals(response.getOk())) {
            return ResponseDTO.error(response);
        }
        PageResult<S> source = response.getData();
        PageResult<T> result = new PageResult<>();
        result.setPageNum(source.getPageNum());
        result.setPageSize(source.getPageSize());
        result.setTotal(source.getTotal());
        result.setPages(source.getPages());
        result.setEmptyFlag(source.getEmptyFlag());
        result.setList(SmartBeanUtil.copyList(source.getList(), targetType));
        return ResponseDTO.ok(result);
    }
}
