package com.hunyuan.sa.base.module.support.sms.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;

/**
 * 平台短信管理公开边界。
 *
 * <p>该边界只承载短信模板和发送记录治理，不接管业务短信发送链。</p>
 */
public interface PlatformSmsFacade {

    ResponseDTO<PageResult<PlatformSmsTemplateSummary>> queryTemplates(
            PlatformSmsTemplatePageQuery query);

    ResponseDTO<String> createTemplate(PlatformSmsTemplateCreateCommand command);

    ResponseDTO<String> updateTemplate(
            String templateCode, PlatformSmsTemplateUpdateCommand command);

    ResponseDTO<String> updateTemplateDisabled(String templateCode, Boolean disableFlag);

    ResponseDTO<PageResult<PlatformSmsSendLogSummary>> querySendLogs(
            PlatformSmsSendLogPageQuery query);
}
