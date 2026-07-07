package com.hunyuan.sa.bpm.common.enumeration;

import com.hunyuan.sa.base.common.enumeration.BaseEnum;

/**
 * 流程定义发起状态。
 */
public enum BpmDefinitionStartStateEnum implements BaseEnum {

    STARTABLE(1, "可发起"),
    SUSPENDED(2, "已停用");

    private final Integer value;

    private final String desc;

    BpmDefinitionStartStateEnum(Integer value, String desc) {
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
