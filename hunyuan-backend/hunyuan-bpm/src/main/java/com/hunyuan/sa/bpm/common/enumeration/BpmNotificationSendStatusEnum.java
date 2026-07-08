package com.hunyuan.sa.bpm.common.enumeration;

import com.hunyuan.sa.base.common.enumeration.BaseEnum;

/**
 * BPM 通知投递状态。
 */
public enum BpmNotificationSendStatusEnum implements BaseEnum {

    PENDING(0, "待发送"),
    SUCCESS(1, "发送成功"),
    FAIL(2, "发送失败");

    private final Integer value;

    private final String desc;

    BpmNotificationSendStatusEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}
