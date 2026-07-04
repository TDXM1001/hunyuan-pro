package com.hunyuan.sa.base.module.support.sms.constant;

import com.hunyuan.sa.base.common.enumeration.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SMS send status.
 */
@Getter
@AllArgsConstructor
public enum SmsSendStatusEnum implements BaseEnum {

    PENDING(0, "pending"),

    SUCCESS(1, "success"),

    FAIL(2, "fail"),
    ;

    private final Integer value;

    private final String desc;
}
