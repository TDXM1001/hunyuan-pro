package com.hunyuan.sa.base.module.support.message.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.enumeration.UserTypeEnum;
import com.hunyuan.sa.base.module.support.message.application.PlatformMessageInboxApplicationService;
import com.hunyuan.sa.base.module.support.message.domain.MessageQueryForm;
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
 * 锁定当前用户消息箱的所有权注入和历史服务适配。
 */
@ExtendWith(MockitoExtension.class)
class PlatformMessageInboxApplicationServiceTest {

    @Mock
    private MessageService messageService;

    private PlatformMessageInboxApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PlatformMessageInboxApplicationService();
        ReflectionTestUtils.setField(service, "messageService", messageService);
    }

    @Test
    void forcesCurrentUserScopeWhenQueryingInbox() {
        MessageVO legacyMessage = new MessageVO();
        legacyMessage.setMessageId(19L);
        legacyMessage.setReceiverUserId(7L);
        legacyMessage.setTitle("个人提醒");
        PageResult<MessageVO> legacyResult = new PageResult<>();
        legacyResult.setPageNum(1L);
        legacyResult.setPageSize(10L);
        legacyResult.setTotal(1L);
        legacyResult.setPages(1L);
        legacyResult.setEmptyFlag(false);
        legacyResult.setList(List.of(legacyMessage));
        when(messageService.query(any(MessageQueryForm.class))).thenReturn(legacyResult);

        PlatformMessageInboxPageQuery query = new PlatformMessageInboxPageQuery();
        query.setPageNum(1L);
        query.setPageSize(10L);
        PageResult<PlatformMessageSummary> result = service.queryPage(
                query, UserTypeEnum.ADMIN_EMPLOYEE, 7L);

        ArgumentCaptor<MessageQueryForm> captor = ArgumentCaptor.forClass(MessageQueryForm.class);
        verify(messageService).query(captor.capture());
        assertThat(captor.getValue().getReceiverUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getReceiverUserType())
                .isEqualTo(UserTypeEnum.ADMIN_EMPLOYEE.getValue());
        assertThat(captor.getValue().getSearchCount()).isFalse();
        assertThat(result.getList()).singleElement().satisfies(message ->
                assertThat(message.getMessageId()).isEqualTo(19L));
    }

    @Test
    void delegatesUnreadCountWithCurrentUserScope() {
        when(messageService.getUnreadCount(UserTypeEnum.ADMIN_EMPLOYEE, 7L)).thenReturn(3L);

        Long unreadCount = service.getUnreadCount(UserTypeEnum.ADMIN_EMPLOYEE, 7L);

        assertThat(unreadCount).isEqualTo(3L);
        verify(messageService).getUnreadCount(UserTypeEnum.ADMIN_EMPLOYEE, 7L);
    }

    @Test
    void delegatesMarkReadWithCurrentUserScope() {
        service.markRead(19L, UserTypeEnum.ADMIN_EMPLOYEE, 7L);

        verify(messageService).updateReadFlag(19L, UserTypeEnum.ADMIN_EMPLOYEE, 7L);
    }
}
