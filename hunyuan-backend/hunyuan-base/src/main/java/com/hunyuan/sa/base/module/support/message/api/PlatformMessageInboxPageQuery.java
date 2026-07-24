package com.hunyuan.sa.base.module.support.message.api;

import com.hunyuan.sa.base.common.domain.PageParam;
import com.hunyuan.sa.base.common.swagger.SchemaEnum;
import com.hunyuan.sa.base.common.validator.enumeration.CheckEnum;
import com.hunyuan.sa.base.module.support.message.constant.MessageTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

/**
 * 当前用户消息箱分页查询条件。
 *
 * <p>接收人信息由服务端根据登录态强制注入，客户端不能指定或覆盖。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformMessageInboxPageQuery extends PageParam {

    @Schema(description = "搜索词")
    @Length(max = 50, message = "搜索词最多50字符")
    private String searchWord;

    @SchemaEnum(value = MessageTypeEnum.class)
    @CheckEnum(value = MessageTypeEnum.class, message = "消息类型")
    private Integer messageType;

    @Schema(description = "是否已读")
    private Boolean readFlag;

    @Schema(description = "查询开始时间")
    private LocalDate startDate;

    @Schema(description = "查询结束时间")
    private LocalDate endDate;
}
