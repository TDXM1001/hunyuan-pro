package com.hunyuan.sa.base.module.support.message.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.message.application.PlatformMessageApplicationService;
import com.hunyuan.sa.base.module.support.message.domain.MessageQueryForm;
import com.hunyuan.sa.base.module.support.message.domain.MessageSendForm;
import com.hunyuan.sa.base.module.support.message.domain.MessageVO;
import com.hunyuan.sa.base.module.support.message.service.MessageService;
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
 * 锁定稳定消息管理边界与历史消息服务之间的映射。
 */
@ExtendWith(MockitoExtension.class)
class PlatformMessageApplicationServiceTest {

    @Mock
    private MessageService messageService;

    private PlatformMessageApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PlatformMessageApplicationService();
        ReflectionTestUtils.setField(service, "messageService", messageService);
    }

    @Test
    void mapsQueryAndResultToStableContract() {
        MessageVO legacyMessage = new MessageVO();
        legacyMessage.setMessageId(18L);
        legacyMessage.setReceiverUserId(7L);
        legacyMessage.setTitle("系统提醒");
        legacyMessage.setDataId("TASK-18");
        PageResult<MessageVO> legacyResult = new PageResult<>();
        legacyResult.setPageNum(1L);
        legacyResult.setPageSize(20L);
        legacyResult.setTotal(1L);
        legacyResult.setPages(1L);
        legacyResult.setEmptyFlag(false);
        legacyResult.setList(List.of(legacyMessage));
        when(messageService.query(any(MessageQueryForm.class))).thenReturn(legacyResult);

        PlatformMessagePageQuery query = new PlatformMessagePageQuery();
        query.setPageNum(1L);
        query.setPageSize(20L);
        query.setReceiverUserId(7L);
        PageResult<PlatformMessageSummary> result = service.queryPage(query);

        ArgumentCaptor<MessageQueryForm> captor = ArgumentCaptor.forClass(MessageQueryForm.class);
        verify(messageService).query(captor.capture());
        assertThat(captor.getValue().getReceiverUserId()).isEqualTo(7L);
        assertThat(result.getList()).singleElement().satisfies(message -> {
            assertThat(message.getMessageId()).isEqualTo(18L);
            assertThat(message.getTitle()).isEqualTo("系统提醒");
            assertThat(message.getDataId()).isEqualTo("TASK-18");
        });
    }

    @Test
    void mapsStableSendCommandsToLegacyForms() {
        PlatformMessageSendCommand command = new PlatformMessageSendCommand();
        command.setMessageType(1);
        command.setReceiverUserType(1);
        command.setReceiverUserId(7L);
        command.setTitle("系统提醒");
        command.setContent("请处理待办任务");
        command.setDataId("TASK-18");

        service.send(List.of(command));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MessageSendForm>> captor = ArgumentCaptor.forClass(List.class);
        verify(messageService).sendMessage(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(form -> {
            assertThat(form.getReceiverUserId()).isEqualTo(7L);
            assertThat(form.getTitle()).isEqualTo("系统提醒");
            assertThat(form.getDataId()).isEqualTo("TASK-18");
        });
    }

    @Test
    void delegatesDeleteAndKeepsLegacyResponse() {
        when(messageService.delete(18L)).thenReturn(ResponseDTO.userErrorParam("消息不存在"));

        ResponseDTO<String> response = service.delete(18L);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).isEqualTo("消息不存在");
        verify(messageService).delete(18L);
    }
}
