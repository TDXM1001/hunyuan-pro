package com.hunyuan.sa.base.module.support.sms.api;

import com.hunyuan.sa.base.common.domain.PageParam;
import com.hunyuan.sa.base.common.swagger.SchemaEnum;
import com.hunyuan.sa.base.common.validator.enumeration.CheckEnum;
import com.hunyuan.sa.base.module.support.sms.constant.SmsSendStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

/**
 * 平台短信发送记录分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformSmsSendLogPageQuery extends PageParam {

    @Schema(description = "手机号码")
    @Length(max = 30, message = "手机号码最多 30 个字符")
    private String phone;

    @Schema(description = "模板编码")
    @Length(max = 100, message = "模板编码最多 100 个字符")
    private String templateCode;

    @SchemaEnum(value = SmsSendStatusEnum.class)
    @CheckEnum(value = SmsSendStatusEnum.class, message = "短信发送状态")
    private Integer sendStatus;

    @Schema(description = "查询开始日期")
    private LocalDate startDate;

    @Schema(description = "查询结束日期")
    private LocalDate endDate;
}
