package com.hunyuan.sa.base.module.support.message.api;

import com.hunyuan.sa.base.common.domain.RequestUser;
import com.hunyuan.sa.base.common.enumeration.UserTypeEnum;
import com.hunyuan.sa.base.common.util.SmartRequestUtil;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 稳定个人消息箱路由与当前用户所有权契约。
 */
class PlatformMessageInboxApiContractTest {

    @Test
    void exposesStableInboxRoutes() throws Exception {
        RequestMapping mapping = PlatformMessageInboxController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping.value()).containsExactly("/api/admin/v1/platform/message-inbox");
        assertThat(PlatformMessageInboxController.class
                .getMethod("queryPage", PlatformMessageInboxPageQuery.class)
                .getAnnotation(PostMapping.class).value()).containsExactly("/query");
        assertThat(PlatformMessageInboxController.class
                .getMethod("getUnreadCount")
                .getAnnotation(GetMapping.class).value()).containsExactly("/unread-count");
        assertThat(PlatformMessageInboxController.class
                .getMethod("markRead", Long.class)
                .getAnnotation(PutMapping.class).value()).containsExactly("/{messageId}/read");
    }

    @Test
    void inboxQueryModelDoesNotExposeReceiverScope() {
        assertThat(FieldNames.of(PlatformMessageInboxPageQuery.class))
                .doesNotContain("receiverUserId", "receiverUserType");
    }

    @Test
    void controllerUsesCurrentRequestUserForInboxOperations() {
        PlatformMessageInboxFacade facade = mock(PlatformMessageInboxFacade.class);
        PlatformMessageInboxController controller = new PlatformMessageInboxController();
        ReflectionTestUtils.setField(controller, "platformMessageInboxFacade", facade);
        RequestUser user = mock(RequestUser.class);
        when(user.getUserId()).thenReturn(7L);
        when(user.getUserType()).thenReturn(UserTypeEnum.ADMIN_EMPLOYEE);

        PlatformMessageInboxPageQuery query = new PlatformMessageInboxPageQuery();
        query.setPageNum(1L);
        query.setPageSize(10L);
        when(facade.queryPage(query, UserTypeEnum.ADMIN_EMPLOYEE, 7L))
                .thenReturn(new com.hunyuan.sa.base.common.domain.PageResult<>());

        try (var mocked = org.mockito.Mockito.mockStatic(SmartRequestUtil.class)) {
            mocked.when(SmartRequestUtil::getRequestUser).thenReturn(user);

            controller.queryPage(query);
            controller.getUnreadCount();
            controller.markRead(19L);

            verify(facade).queryPage(query, UserTypeEnum.ADMIN_EMPLOYEE, 7L);
            verify(facade).getUnreadCount(UserTypeEnum.ADMIN_EMPLOYEE, 7L);
            verify(facade).markRead(19L, UserTypeEnum.ADMIN_EMPLOYEE, 7L);
        }
    }

    /**
     * 只读取当前类声明的字段名，防止查询 DTO 后续意外加入接收人范围。
     */
    private static final class FieldNames {

        private static String[] of(Class<?> type) {
            return java.util.Arrays.stream(type.getDeclaredFields())
                    .map(Field::getName)
                    .toArray(String[]::new);
        }
    }
}
