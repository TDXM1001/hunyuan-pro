package com.hunyuan.sa.bpm.common.enumeration;

import com.hunyuan.sa.base.common.enumeration.BaseEnum;

/**
 * 流程抄送已读状态。
 */
public enum BpmCopyReadStateEnum implements BaseEnum {

    UNREAD(0, "未读"),
    READ(1, "已读");

    private final Integer value;

    private final String desc;

    BpmCopyReadStateEnum(Integer value, String desc) {
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
