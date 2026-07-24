package com.hunyuan.sa.base.module.support.message.application;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.enumeration.UserTypeEnum;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.module.support.message.api.PlatformMessageInboxFacade;
import com.hunyuan.sa.base.module.support.message.api.PlatformMessageInboxPageQuery;
import com.hunyuan.sa.base.module.support.message.api.PlatformMessageSummary;
import com.hunyuan.sa.base.module.support.message.domain.MessageQueryForm;
import com.hunyuan.sa.base.module.support.message.domain.MessageVO;
import com.hunyuan.sa.base.module.support.message.service.MessageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 当前用户消息箱用例实现，统一执行消息所有权约束。
 */
@Service
public class PlatformMessageInboxApplicationService implements PlatformMessageInboxFacade {

    @Resource
    private MessageService messageService;

    @Override
    public PageResult<PlatformMessageSummary> queryPage(
            PlatformMessageInboxPageQuery query, UserTypeEnum userType, Long userId) {
        MessageQueryForm legacyQuery = SmartBeanUtil.copy(query, MessageQueryForm.class);
        legacyQuery.setSearchCount(false);
        legacyQuery.setReceiverUserType(userType.getValue());
        legacyQuery.setReceiverUserId(userId);
        PageResult<MessageVO> legacyResult = messageService.query(legacyQuery);

        PageResult<PlatformMessageSummary> result = new PageResult<>();
        result.setPageNum(legacyResult.getPageNum());
        result.setPageSize(legacyResult.getPageSize());
        result.setTotal(legacyResult.getTotal());
        result.setPages(legacyResult.getPages());
        result.setEmptyFlag(legacyResult.getEmptyFlag());
        result.setList(SmartBeanUtil.copyList(legacyResult.getList(), PlatformMessageSummary.class));
        return result;
    }

    @Override
    public Long getUnreadCount(UserTypeEnum userType, Long userId) {
        return messageService.getUnreadCount(userType, userId);
    }

    @Override
    public void markRead(Long messageId, UserTypeEnum userType, Long userId) {
        messageService.updateReadFlag(messageId, userType, userId);
    }
}
