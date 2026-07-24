package com.hunyuan.sa.admin.module.system.message;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.hunyuan.sa.admin.constant.AdminSwaggerTagConst;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.domain.ValidateList;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.module.support.message.api.PlatformMessageFacade;
import com.hunyuan.sa.base.module.support.message.api.PlatformMessagePageQuery;
import com.hunyuan.sa.base.module.support.message.api.PlatformMessageSendCommand;
import com.hunyuan.sa.base.module.support.message.api.PlatformMessageSummary;
import com.hunyuan.sa.base.module.support.message.domain.MessageQueryForm;
import com.hunyuan.sa.base.module.support.message.domain.MessageSendForm;
import com.hunyuan.sa.base.module.support.message.domain.MessageVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 后管 消息路由
 *
 * @author: 卓大
 * @date: 2025/04/09 20:55
 */
@Tag(name = AdminSwaggerTagConst.System.SYSTEM_MESSAGE)
@RestController
public class AdminMessageController {

    @Resource
    private PlatformMessageFacade platformMessageFacade;

    @Operation(summary = "通知消息-新建  @author 卓大")
    @PostMapping("/message/sendMessages")
    @SaCheckPermission("system:message:send")
    public ResponseDTO<String> sendMessages(@RequestBody @Valid ValidateList<MessageSendForm> messageList) {
        List<PlatformMessageSendCommand> commands = messageList.stream().map(message -> {
            PlatformMessageSendCommand command = SmartBeanUtil.copy(
                    message, PlatformMessageSendCommand.class);
            command.setDataId(message.getDataId() == null ? null : String.valueOf(message.getDataId()));
            return command;
        }).toList();
        platformMessageFacade.send(commands);
        return ResponseDTO.ok();
    }

    @Operation(summary = "通知消息-分页查询   @author 卓大")
    @PostMapping("/message/query")
    @SaCheckPermission("system:message:query")
    public ResponseDTO<PageResult<MessageVO>> query(@RequestBody @Valid MessageQueryForm queryForm) {
        PlatformMessagePageQuery query = SmartBeanUtil.copy(queryForm, PlatformMessagePageQuery.class);
        PageResult<PlatformMessageSummary> result = platformMessageFacade.queryPage(query);
        PageResult<MessageVO> legacyResult = new PageResult<>();
        legacyResult.setPageNum(result.getPageNum());
        legacyResult.setPageSize(result.getPageSize());
        legacyResult.setTotal(result.getTotal());
        legacyResult.setPages(result.getPages());
        legacyResult.setEmptyFlag(result.getEmptyFlag());
        legacyResult.setList(SmartBeanUtil.copyList(result.getList(), MessageVO.class));
        return ResponseDTO.ok(legacyResult);
    }

    @Operation(summary = "通知消息-删除   @author 卓大")
    @GetMapping("/message/delete/{messageId}")
    @SaCheckPermission("system:message:delete")
    public ResponseDTO<String> delete(@PathVariable Long messageId) {
        return platformMessageFacade.delete(messageId);
    }

}
