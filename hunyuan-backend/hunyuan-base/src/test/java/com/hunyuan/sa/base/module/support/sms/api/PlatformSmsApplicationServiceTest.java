package com.hunyuan.sa.base.module.support.sms.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.sms.application.PlatformSmsApplicationService;
import com.hunyuan.sa.base.module.support.sms.domain.SmsSendLogQueryForm;
import com.hunyuan.sa.base.module.support.sms.domain.SmsSendLogVO;
import com.hunyuan.sa.base.module.support.sms.domain.SmsTemplateUpdateForm;
import com.hunyuan.sa.base.module.support.sms.domain.SmsTemplateVO;
import com.hunyuan.sa.base.module.support.sms.service.SmsService;
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
 * 锁定平台短信边界与历史短信服务之间的映射。
 */
@ExtendWith(MockitoExtension.class)
class PlatformSmsApplicationServiceTest {

    @Mock
    private SmsService smsService;

    private PlatformSmsApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PlatformSmsApplicationService();
        ReflectionTestUtils.setField(service, "smsService", smsService);
    }

    @Test
    void mapsTemplateQueryAndPageResult() {
        SmsTemplateVO template = new SmsTemplateVO();
        template.setTemplateCode("login_code");
        template.setTemplateName("登录验证码");
        when(smsService.queryTemplate(any())).thenReturn(ResponseDTO.ok(pageOf(template)));

        PlatformSmsTemplatePageQuery query = new PlatformSmsTemplatePageQuery();
        query.setPageNum(1L);
        query.setPageSize(10L);
        query.setTemplateName("登录验证码");
        ResponseDTO<PageResult<PlatformSmsTemplateSummary>> response =
                service.queryTemplates(query);

        assertThat(response.getData().getList()).singleElement().satisfies(item -> {
            assertThat(item.getTemplateCode()).isEqualTo("login_code");
            assertThat(item.getTemplateName()).isEqualTo("登录验证码");
        });
    }

    @Test
    void pathTemplateCodeOverridesUpdateBodyMapping() {
        when(smsService.updateTemplate(any())).thenReturn(ResponseDTO.ok());
        PlatformSmsTemplateUpdateCommand command = new PlatformSmsTemplateUpdateCommand();
        command.setTemplateName("登录验证码");
        command.setTemplateContent("验证码 ${code}");

        service.updateTemplate("login_code", command);

        ArgumentCaptor<SmsTemplateUpdateForm> captor =
                ArgumentCaptor.forClass(SmsTemplateUpdateForm.class);
        verify(smsService).updateTemplate(captor.capture());
        assertThat(captor.getValue().getTemplateCode()).isEqualTo("login_code");
        assertThat(captor.getValue().getTemplateContent()).isEqualTo("验证码 ${code}");
    }

    @Test
    void mapsSendLogQueryAndPageResult() {
        SmsSendLogVO sendLog = new SmsSendLogVO();
        sendLog.setSmsSendLogId(31L);
        sendLog.setPhone("13800138000");
        when(smsService.querySendLog(any(SmsSendLogQueryForm.class)))
                .thenReturn(pageOf(sendLog));

        PlatformSmsSendLogPageQuery query = new PlatformSmsSendLogPageQuery();
        query.setPageNum(1L);
        query.setPageSize(10L);
        query.setPhone("13800138000");
        ResponseDTO<PageResult<PlatformSmsSendLogSummary>> response =
                service.querySendLogs(query);

        assertThat(response.getData().getList()).singleElement().satisfies(item -> {
            assertThat(item.getSmsSendLogId()).isEqualTo(31L);
            assertThat(item.getPhone()).isEqualTo("13800138000");
        });
    }

    /**
     * 构造单条分页结果，避免测试重复分页元数据。
     */
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
