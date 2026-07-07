package com.hunyuan.sa.bpm.common.enumeration;

import com.hunyuan.sa.base.common.enumeration.BaseEnum;

/**
 * 流程任务状态。
 */
public enum BpmTaskStateEnum implements BaseEnum {

    PENDING(1, "待处理"),
    COMPLETED(2, "已完成"),
    CANCELLED(3, "已取消");

    private final Integer value;

    private final String desc;

    BpmTaskStateEnum(Integer value, String desc) {
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
