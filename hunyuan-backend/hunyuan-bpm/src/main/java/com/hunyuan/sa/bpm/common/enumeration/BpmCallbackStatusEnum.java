package com.hunyuan.sa.bpm.common.enumeration;

import com.hunyuan.sa.base.common.enumeration.BaseEnum;

/**
 * BPM 业务回调状态。
 */
public enum BpmCallbackStatusEnum implements BaseEnum {

    PENDING(0, "待处理"),
    SUCCEEDED(1, "成功"),
    FAILED(2, "失败"),
    NEEDS_COMPENSATION(3, "需人工补偿"),
    COMPENSATED(4, "已补偿");

    private final Integer value;

    private final String desc;

    BpmCallbackStatusEnum(Integer value, String desc) {
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

    public boolean equalsValue(Integer value) {
        return this.value.equals(value);
    }
}
