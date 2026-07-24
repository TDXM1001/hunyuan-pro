package com.hunyuan.sa.admin.module.system.message;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.domain.ValidateList;
import com.hunyuan.sa.base.module.support.message.api.PlatformMessageFacade;
import com.hunyuan.sa.base.module.support.message.api.PlatformMessagePageQuery;
import com.hunyuan.sa.base.module.support.message.api.PlatformMessageSendCommand;
import com.hunyuan.sa.base.module.support.message.api.PlatformMessageSummary;
import com.hunyuan.sa.base.module.support.message.domain.MessageQueryForm;
import com.hunyuan.sa.base.module.support.message.domain.MessageSendForm;
import com.hunyuan.sa.base.module.support.message.domain.MessageVO;
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
 * 锁定历史消息管理路由通过稳定 Facade 适配，并保持原有响应结构。
 */
@ExtendWith(MockitoExtension.class)
class AdminMessageControllerFacadeTest {

    @Mock
    private PlatformMessageFacade platformMessageFacade;

    private AdminMessageController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminMessageController();
        ReflectionTestUtils.setField(controller, "platformMessageFacade", platformMessageFacade);
    }

    @Test
    void mapsLegacyQueryAndStableSummary() {
        PlatformMessageSummary summary = new PlatformMessageSummary();
        summary.setMessageId(18L);
        summary.setReceiverUserId(7L);
        summary.setTitle("系统提醒");
        PageResult<PlatformMessageSummary> stableResult = new PageResult<>();
        stableResult.setPageNum(1L);
        stableResult.setPageSize(10L);
        stableResult.setTotal(1L);
        stableResult.setPages(1L);
        stableResult.setEmptyFlag(false);
        stableResult.setList(List.of(summary));
        when(platformMessageFacade.queryPage(any(PlatformMessagePageQuery.class)))
                .thenReturn(stableResult);

        MessageQueryForm query = new MessageQueryForm();
        query.setPageNum(1L);
        query.setPageSize(10L);
        query.setReceiverUserId(7L);
        ResponseDTO<PageResult<MessageVO>> response = controller.query(query);

        assertThat(response.getData().getList()).singleElement().satisfies(message -> {
            assertThat(message.getMessageId()).isEqualTo(18L);
            assertThat(message.getTitle()).isEqualTo("系统提醒");
        });
        verify(platformMessageFacade).queryPage(any(PlatformMessagePageQuery.class));
    }

    @Test
    void mapsLegacyBatchSendAndKeepsBusinessIdentifier() {
        MessageSendForm form = new MessageSendForm();
        form.setMessageType(1);
        form.setReceiverUserType(1);
        form.setReceiverUserId(7L);
        form.setTitle("系统提醒");
        form.setContent("请处理待办任务");
        form.setDataId(18L);
        ValidateList<MessageSendForm> forms = new ValidateList<>();
        forms.add(form);

        controller.sendMessages(forms);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PlatformMessageSendCommand>> captor = ArgumentCaptor.forClass(List.class);
        verify(platformMessageFacade).send(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(command ->
                assertThat(command.getDataId()).isEqualTo("18"));
    }

    @Test
    void delegatesLegacyDeleteToStableFacade() {
        when(platformMessageFacade.delete(18L)).thenReturn(ResponseDTO.ok());

        ResponseDTO<String> response = controller.delete(18L);

        assertThat(response.getOk()).isTrue();
        verify(platformMessageFacade).delete(18L);
    }
}
