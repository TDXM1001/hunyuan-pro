package com.hunyuan.sa.base.module.support.message.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.enumeration.UserTypeEnum;

/**
 * 当前用户消息箱公开边界。
 *
 * <p>该边界独立于管理员消息管理权限，所有操作都必须绑定当前登录用户。</p>
 */
public interface PlatformMessageInboxFacade {

    PageResult<PlatformMessageSummary> queryPage(
            PlatformMessageInboxPageQuery query, UserTypeEnum userType, Long userId);

    Long getUnreadCount(UserTypeEnum userType, Long userId);

    void markRead(Long messageId, UserTypeEnum userType, Long userId);
}
