package com.hunyuan.sa.base.module.support.message.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;

import java.util.List;

/**
 * 平台消息管理公开边界。
 *
 * <p>该边界只承载管理员查询、发送和删除能力，不包含当前用户个人消息箱。</p>
 */
public interface PlatformMessageFacade {

    PageResult<PlatformMessageSummary> queryPage(PlatformMessagePageQuery query);

    void send(List<PlatformMessageSendCommand> commands);

    ResponseDTO<String> delete(Long messageId);
}
