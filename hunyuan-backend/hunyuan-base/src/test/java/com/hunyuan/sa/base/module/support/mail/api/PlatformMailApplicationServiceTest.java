package com.hunyuan.sa.base.module.support.mail.api;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.mail.MailService;
import com.hunyuan.sa.base.module.support.mail.application.PlatformMailApplicationService;
import com.hunyuan.sa.base.module.support.mail.constant.MailTemplateCodeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定平台邮件边界与历史邮件服务之间的映射。
 */
@ExtendWith(MockitoExtension.class)
class PlatformMailApplicationServiceTest {

    @Mock
    private MailService mailService;

    private PlatformMailApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PlatformMailApplicationService();
        ReflectionTestUtils.setField(service, "mailService", mailService);
    }

    @Test
    void mapsStableTemplateCommandToLegacyMailService() {
        PlatformTemplateMailCommand command = new PlatformTemplateMailCommand(
                PlatformMailTemplateCode.LOGIN_VERIFICATION_CODE,
                Map.of("code", "1234"),
                List.of("hunyuan@example.com"));
        when(mailService.sendMail(
                MailTemplateCodeEnum.LOGIN_VERIFICATION_CODE,
                command.templateParameters(),
                command.recipients())).thenReturn(ResponseDTO.ok());

        ResponseDTO<String> response = service.sendTemplateMail(command);

        assertThat(response.getOk()).isTrue();
        verify(mailService).sendMail(
                MailTemplateCodeEnum.LOGIN_VERIFICATION_CODE,
                Map.of("code", "1234"),
                List.of("hunyuan@example.com"));
    }

    @Test
    void keepsLegacyMailFailureCodeAndMessage() {
        PlatformTemplateMailCommand command = new PlatformTemplateMailCommand(
                PlatformMailTemplateCode.LOGIN_VERIFICATION_CODE,
                Map.of("code", "1234"),
                List.of("hunyuan@example.com"));
        when(mailService.sendMail(
                MailTemplateCodeEnum.LOGIN_VERIFICATION_CODE,
                command.templateParameters(),
                command.recipients()))
                .thenReturn(ResponseDTO.userErrorParam("邮件发送失败"));

        ResponseDTO<String> response = service.sendTemplateMail(command);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).isEqualTo("邮件发送失败");
    }

    @Test
    void commandKeepsImmutableParameterAndRecipientSnapshots() {
        Map<String, Object> parameters = new java.util.HashMap<>();
        parameters.put("code", "1234");
        List<String> recipients = new java.util.ArrayList<>();
        recipients.add("hunyuan@example.com");

        PlatformTemplateMailCommand command = new PlatformTemplateMailCommand(
                PlatformMailTemplateCode.LOGIN_VERIFICATION_CODE,
                parameters,
                recipients);
        parameters.put("code", "9999");
        recipients.add("other@example.com");

        assertThat(command.templateParameters()).containsEntry("code", "1234");
        assertThat(command.recipients()).containsExactly("hunyuan@example.com");
    }
}
