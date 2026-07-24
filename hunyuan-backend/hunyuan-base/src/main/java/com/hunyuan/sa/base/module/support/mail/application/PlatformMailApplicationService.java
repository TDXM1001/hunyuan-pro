package com.hunyuan.sa.base.module.support.mail.application;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.mail.MailService;
import com.hunyuan.sa.base.module.support.mail.api.PlatformMailFacade;
import com.hunyuan.sa.base.module.support.mail.api.PlatformMailTemplateCode;
import com.hunyuan.sa.base.module.support.mail.api.PlatformTemplateMailCommand;
import com.hunyuan.sa.base.module.support.mail.constant.MailTemplateCodeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 平台事务邮件发送用例实现，隔离历史模板枚举和底层邮件服务。
 */
@Service
public class PlatformMailApplicationService implements PlatformMailFacade {

    @Resource
    private MailService mailService;

    @Override
    public ResponseDTO<String> sendTemplateMail(PlatformTemplateMailCommand command) {
        return mailService.sendMail(
                toLegacyTemplateCode(command.templateCode()),
                command.templateParameters(),
                command.recipients());
    }

    /**
     * 显式映射稳定模板编码，新增模板时必须同步评估公开范围。
     */
    private MailTemplateCodeEnum toLegacyTemplateCode(PlatformMailTemplateCode templateCode) {
        return switch (templateCode) {
            case LOGIN_VERIFICATION_CODE -> MailTemplateCodeEnum.LOGIN_VERIFICATION_CODE;
        };
    }
}
