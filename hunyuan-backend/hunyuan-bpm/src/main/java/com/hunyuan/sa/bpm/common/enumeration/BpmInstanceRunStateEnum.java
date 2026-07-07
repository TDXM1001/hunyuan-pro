package com.hunyuan.sa.bpm.common.enumeration;

import com.hunyuan.sa.base.common.enumeration.BaseEnum;

/**
 * 流程实例运行状态。
 */
public enum BpmInstanceRunStateEnum implements BaseEnum {

    RUNNING(1, "流转中"),
    WAIT_RESUBMIT(2, "待重新提交"),
    FINISHED(3, "已结束"),
    CANCELLED(4, "已取消");

    private final Integer value;

    private final String desc;

    BpmInstanceRunStateEnum(Integer value, String desc) {
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
