package com.hunyuan.sa.base.module.support.message.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.RequestUser;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartRequestUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户消息箱稳定 HTTP 接口。
 */
@RestController
@RequestMapping("/api/admin/v1/platform/message-inbox")
@Tag(name = "平台能力 - 当前用户消息箱")
public class PlatformMessageInboxController {

    @Resource
    private PlatformMessageInboxFacade platformMessageInboxFacade;

    @PostMapping("/query")
    @Operation(operationId = "platformMessageInboxQuery", summary = "分页查询当前用户消息")
    public ResponseDTO<PageResult<PlatformMessageSummary>> queryPage(
            @RequestBody @Valid PlatformMessageInboxPageQuery query) {
        RequestUser user = SmartRequestUtil.getRequestUser();
        if (user == null) {
            return ResponseDTO.userErrorParam("用户未登录");
        }
        return ResponseDTO.ok(platformMessageInboxFacade.queryPage(
                query, user.getUserType(), user.getUserId()));
    }

    @GetMapping("/unread-count")
    @Operation(operationId = "platformMessageInboxUnreadCount", summary = "查询当前用户未读消息数")
    public ResponseDTO<Long> getUnreadCount() {
        RequestUser user = SmartRequestUtil.getRequestUser();
        if (user == null) {
            return ResponseDTO.userErrorParam("用户未登录");
        }
        return ResponseDTO.ok(platformMessageInboxFacade.getUnreadCount(
                user.getUserType(), user.getUserId()));
    }

    @PutMapping("/{messageId}/read")
    @Operation(operationId = "platformMessageInboxMarkRead", summary = "标记当前用户消息为已读")
    public ResponseDTO<String> markRead(@PathVariable Long messageId) {
        RequestUser user = SmartRequestUtil.getRequestUser();
        if (user == null) {
            return ResponseDTO.userErrorParam("用户未登录");
        }
        platformMessageInboxFacade.markRead(messageId, user.getUserType(), user.getUserId());
        return ResponseDTO.ok();
    }
}
