package com.hunyuan.sa.admin.module.system.support;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.sms.api.PlatformSmsFacade;
import com.hunyuan.sa.base.module.support.sms.api.PlatformSmsSendLogSummary;
import com.hunyuan.sa.base.module.support.sms.api.PlatformSmsTemplateSummary;
import com.hunyuan.sa.base.module.support.sms.domain.SmsSendLogQueryForm;
import com.hunyuan.sa.base.module.support.sms.domain.SmsTemplateQueryForm;
import com.hunyuan.sa.base.module.support.sms.domain.SmsTemplateUpdateForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定历史短信管理路由通过稳定 Facade 适配。
 */
@ExtendWith(MockitoExtension.class)
class AdminSmsControllerFacadeTest {

    @Mock
    private PlatformSmsFacade facade;

    private AdminSmsController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminSmsController();
        ReflectionTestUtils.setField(controller, "platformSmsFacade", facade);
    }

    @Test
    void mapsLegacyTemplateAndSendLogResponses() {
        PlatformSmsTemplateSummary template = new PlatformSmsTemplateSummary();
        template.setTemplateCode("login_code");
        PlatformSmsSendLogSummary sendLog = new PlatformSmsSendLogSummary();
        sendLog.setSmsSendLogId(31L);
        when(facade.queryTemplates(any())).thenReturn(ResponseDTO.ok(pageOf(template)));
        when(facade.querySendLogs(any())).thenReturn(ResponseDTO.ok(pageOf(sendLog)));

        SmsTemplateQueryForm templateQuery = new SmsTemplateQueryForm();
        SmsSendLogQueryForm sendLogQuery = new SmsSendLogQueryForm();

        assertThat(controller.queryTemplate(templateQuery).getData().getList())
                .singleElement().satisfies(item ->
                        assertThat(item.getTemplateCode()).isEqualTo("login_code"));
        assertThat(controller.querySendLog(sendLogQuery).getData().getList())
                .singleElement().satisfies(item ->
                        assertThat(item.getSmsSendLogId()).isEqualTo(31L));
    }

    @Test
    void legacyUpdateUsesTemplateCodeAsFacadePathIdentity() {
        when(facade.updateTemplate(any(), any())).thenReturn(ResponseDTO.ok());
        SmsTemplateUpdateForm form = new SmsTemplateUpdateForm();
        form.setTemplateCode("login_code");
        form.setTemplateName("登录验证码");
        form.setTemplateContent("验证码 ${code}");

        controller.updateTemplate(form);

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(facade).updateTemplate(codeCaptor.capture(), any());
        assertThat(codeCaptor.getValue()).isEqualTo("login_code");
    }

    private <T> PageResult<T> pageOf(T item) {
        PageResult<T> page = new PageResult<>();
        page.setPageNum(1L);
        page.setPageSize(10L);
        page.setTotal(1L);
        page.setPages(1L);
        page.setEmptyFlag(false);
        page.setList(List.of(item));
        return page;
    }
}
