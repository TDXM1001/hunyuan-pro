package com.hunyuan.sa.base.module.support.message.application;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.module.support.message.api.PlatformMessageFacade;
import com.hunyuan.sa.base.module.support.message.api.PlatformMessagePageQuery;
import com.hunyuan.sa.base.module.support.message.api.PlatformMessageSendCommand;
import com.hunyuan.sa.base.module.support.message.api.PlatformMessageSummary;
import com.hunyuan.sa.base.module.support.message.domain.MessageQueryForm;
import com.hunyuan.sa.base.module.support.message.domain.MessageSendForm;
import com.hunyuan.sa.base.module.support.message.domain.MessageVO;
import com.hunyuan.sa.base.module.support.message.service.MessageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 平台消息管理用例实现，负责稳定契约与历史消息服务之间的转换。
 */
@Service
public class PlatformMessageApplicationService implements PlatformMessageFacade {

    @Resource
    private MessageService messageService;

    @Override
    public PageResult<PlatformMessageSummary> queryPage(PlatformMessagePageQuery query) {
        MessageQueryForm legacyQuery = SmartBeanUtil.copy(query, MessageQueryForm.class);
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
    public void send(List<PlatformMessageSendCommand> commands) {
        List<MessageSendForm> legacyCommands = SmartBeanUtil.copyList(commands, MessageSendForm.class);
        messageService.sendMessage(legacyCommands);
    }

    @Override
    public ResponseDTO<String> delete(Long messageId) {
        return messageService.delete(messageId);
    }
}
