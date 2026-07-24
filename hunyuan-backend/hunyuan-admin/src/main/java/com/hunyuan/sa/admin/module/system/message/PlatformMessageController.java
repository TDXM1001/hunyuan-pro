package com.hunyuan.sa.admin.module.system.message;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.domain.ValidateList;
import com.hunyuan.sa.base.module.support.message.api.PlatformMessageFacade;
import com.hunyuan.sa.base.module.support.message.api.PlatformMessagePageQuery;
import com.hunyuan.sa.base.module.support.message.api.PlatformMessageSendCommand;
import com.hunyuan.sa.base.module.support.message.api.PlatformMessageSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台消息管理稳定 HTTP 接口。
 */
@RestController
@RequestMapping("/api/admin/v1/platform/messages")
@Tag(name = "平台能力 - 消息管理")
public class PlatformMessageController {

    @Resource
    private PlatformMessageFacade platformMessageFacade;

    @PostMapping("/query")
    @Operation(operationId = "platformMessageQuery", summary = "分页查询消息")
    @SaCheckPermission("system:message:query")
    public ResponseDTO<PageResult<PlatformMessageSummary>> queryPage(
            @RequestBody @Valid PlatformMessagePageQuery query) {
        return ResponseDTO.ok(platformMessageFacade.queryPage(query));
    }

    @PostMapping
    @Operation(operationId = "platformMessageSend", summary = "批量发送消息")
    @SaCheckPermission("system:message:send")
    public ResponseDTO<String> send(
            @RequestBody @Valid ValidateList<PlatformMessageSendCommand> commands) {
        platformMessageFacade.send(commands);
        return ResponseDTO.ok();
    }

    @DeleteMapping("/{messageId}")
    @Operation(operationId = "platformMessageDelete", summary = "删除消息")
    @SaCheckPermission("system:message:delete")
    public ResponseDTO<String> delete(@PathVariable Long messageId) {
        return platformMessageFacade.delete(messageId);
    }
}
