package com.hunyuan.sa.base.module.support.message.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.RequestUser;
import com.hunyuan.sa.base.common.enumeration.UserTypeEnum;
import com.hunyuan.sa.base.common.util.SmartRequestUtil;
import com.hunyuan.sa.base.module.support.message.controller.MessageController;
import com.hunyuan.sa.base.module.support.message.domain.MessageQueryForm;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定历史个人消息路由通过稳定消息箱 Facade 适配。
 */
class MessageControllerInboxFacadeTest {

    @Test
    void legacyRoutesKeepResponseShapeAndCurrentUserScope() {
        PlatformMessageInboxFacade facade = mock(PlatformMessageInboxFacade.class);
        MessageController controller = new MessageController();
        ReflectionTestUtils.setField(controller, "platformMessageInboxFacade", facade);
        RequestUser user = mock(RequestUser.class);
        when(user.getUserId()).thenReturn(7L);
        when(user.getUserType()).thenReturn(UserTypeEnum.ADMIN_EMPLOYEE);

        PlatformMessageSummary summary = new PlatformMessageSummary();
        summary.setMessageId(19L);
        summary.setTitle("个人提醒");
        PageResult<PlatformMessageSummary> result = new PageResult<>();
        result.setPageNum(1L);
        result.setPageSize(10L);
        result.setTotal(1L);
        result.setPages(1L);
        result.setEmptyFlag(false);
        result.setList(List.of(summary));
        when(facade.queryPage(any(), any(), any())).thenReturn(result);
        when(facade.getUnreadCount(UserTypeEnum.ADMIN_EMPLOYEE, 7L)).thenReturn(2L);

        MessageQueryForm query = new MessageQueryForm();
        query.setPageNum(1L);
        query.setPageSize(10L);
        try (var mocked = org.mockito.Mockito.mockStatic(SmartRequestUtil.class)) {
            mocked.when(SmartRequestUtil::getRequestUser).thenReturn(user);

            var queryResponse = controller.query(query);
            var unreadResponse = controller.getUnreadCount();
            controller.updateReadFlag(19L);

            assertThat(queryResponse.getData().getList()).singleElement().satisfies(message ->
                    assertThat(message.getMessageId()).isEqualTo(19L));
            assertThat(unreadResponse.getData()).isEqualTo(2L);
            verify(facade).markRead(19L, UserTypeEnum.ADMIN_EMPLOYEE, 7L);
        }
    }
}
