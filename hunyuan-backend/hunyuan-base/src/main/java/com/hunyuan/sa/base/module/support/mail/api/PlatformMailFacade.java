package com.hunyuan.sa.base.module.support.mail.api;

import com.hunyuan.sa.base.common.domain.ResponseDTO;

/**
 * 平台事务邮件发送公开边界。
 *
 * <p>该边界只承载受控模板邮件发送，不暴露 SMTP 配置、模板实体或底层邮件客户端。</p>
 */
public interface PlatformMailFacade {

    ResponseDTO<String> sendTemplateMail(PlatformTemplateMailCommand command);
}
