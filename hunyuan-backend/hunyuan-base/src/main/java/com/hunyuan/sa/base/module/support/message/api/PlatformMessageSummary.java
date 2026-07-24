package com.hunyuan.sa.base.module.support.message.api;

import com.hunyuan.sa.base.common.enumeration.UserTypeEnum;
import com.hunyuan.sa.base.common.swagger.SchemaEnum;
import com.hunyuan.sa.base.module.support.message.constant.MessageTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台消息摘要，隔离历史消息视图对象。
 */
@Data
public class PlatformMessageSummary {

    @Schema(description = "消息标识")
    private Long messageId;

    @SchemaEnum(value = MessageTypeEnum.class)
    private Integer messageType;

    @SchemaEnum(value = UserTypeEnum.class)
    private Integer receiverUserType;

    @Schema(description = "接收人标识")
    private Long receiverUserId;

    @Schema(description = "关联业务标识")
    private String dataId;

    @Schema(description = "消息标题")
    private String title;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "是否已读")
    private Boolean readFlag;

    @Schema(description = "已读时间")
    private LocalDateTime readTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
