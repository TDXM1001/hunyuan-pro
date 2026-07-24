package com.hunyuan.sa.base.module.support.sms.application;

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
import com.hunyuan.sa.base.module.support.sms.service.SmsService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 平台短信管理用例实现，隔离历史短信对象和服务。
 */
@Service
public class PlatformSmsApplicationService implements PlatformSmsFacade {

    @Resource
    private SmsService smsService;

    @Override
    public ResponseDTO<PageResult<PlatformSmsTemplateSummary>> queryTemplates(
            PlatformSmsTemplatePageQuery query) {
        ResponseDTO<PageResult<SmsTemplateVO>> legacyResponse = smsService.queryTemplate(
                SmartBeanUtil.copy(query, SmsTemplateQueryForm.class));
        if (!Boolean.TRUE.equals(legacyResponse.getOk())) {
            return ResponseDTO.error(legacyResponse);
        }
        return ResponseDTO.ok(copyPage(
                legacyResponse.getData(), PlatformSmsTemplateSummary.class));
    }

    @Override
    public ResponseDTO<String> createTemplate(PlatformSmsTemplateCreateCommand command) {
        return smsService.addTemplate(SmartBeanUtil.copy(command, SmsTemplateAddForm.class));
    }

    @Override
    public ResponseDTO<String> updateTemplate(
            String templateCode, PlatformSmsTemplateUpdateCommand command) {
        SmsTemplateUpdateForm legacyForm = SmartBeanUtil.copy(
                command, SmsTemplateUpdateForm.class);
        legacyForm.setTemplateCode(templateCode);
        return smsService.updateTemplate(legacyForm);
    }

    @Override
    public ResponseDTO<String> updateTemplateDisabled(
            String templateCode, Boolean disableFlag) {
        return smsService.updateTemplateDisabled(templateCode, disableFlag);
    }

    @Override
    public ResponseDTO<PageResult<PlatformSmsSendLogSummary>> querySendLogs(
            PlatformSmsSendLogPageQuery query) {
        PageResult<SmsSendLogVO> legacyPage = smsService.querySendLog(
                SmartBeanUtil.copy(query, SmsSendLogQueryForm.class));
        return ResponseDTO.ok(copyPage(legacyPage, PlatformSmsSendLogSummary.class));
    }

    /**
     * 复制分页元数据并转换平台公开对象。
     */
    private <S, T> PageResult<T> copyPage(PageResult<S> source, Class<T> targetType) {
        PageResult<T> result = new PageResult<>();
        result.setPageNum(source.getPageNum());
        result.setPageSize(source.getPageSize());
        result.setTotal(source.getTotal());
        result.setPages(source.getPages());
        result.setEmptyFlag(source.getEmptyFlag());
        result.setList(SmartBeanUtil.copyList(source.getList(), targetType));
        return result;
    }
}
