package com.hunyuan.sa.base.module.support.sms.domain;

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
 * SMS send log query form.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SmsSendLogQueryForm extends PageParam {

    @Schema(description = "Phone number")
    @Length(max = 30, message = "phone max length is 30")
    private String phone;

    @Schema(description = "Template code")
    @Length(max = 100, message = "template code max length is 100")
    private String templateCode;

    @SchemaEnum(value = SmsSendStatusEnum.class)
    @CheckEnum(value = SmsSendStatusEnum.class, message = "sms send status")
    private Integer sendStatus;

    @Schema(description = "Query start date")
    private LocalDate startDate;

    @Schema(description = "Query end date")
    private LocalDate endDate;
}
