package com.hunyuan.sa.base.module.support.message.api;

import com.hunyuan.sa.base.common.enumeration.UserTypeEnum;
import com.hunyuan.sa.base.common.swagger.SchemaEnum;
import com.hunyuan.sa.base.module.support.message.constant.MessageTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 平台消息发送命令。
 */
@Data
public class PlatformMessageSendCommand {

    @SchemaEnum(value = MessageTypeEnum.class, desc = "消息类型")
    @NotNull(message = "消息类型不能为空")
    private Integer messageType;

    @SchemaEnum(value = UserTypeEnum.class, desc = "接收人类型")
    @NotNull(message = "接收人类型不能为空")
    private Integer receiverUserType;

    @Schema(description = "接收人标识")
    @NotNull(message = "接收人标识不能为空")
    private Long receiverUserId;

    @Schema(description = "消息标题")
    @NotBlank(message = "消息标题不能为空")
    private String title;

    @Schema(description = "消息内容")
    @NotBlank(message = "消息内容不能为空")
    private String content;

    @Schema(description = "关联业务标识")
    private String dataId;
}
