package com.hunyuan.sa.bpm.common.enumeration;

import com.hunyuan.sa.base.common.enumeration.BaseEnum;

/**
 * 流程定义生命周期状态。
 */
public enum BpmDefinitionLifecycleStateEnum implements BaseEnum {

    CURRENT(1, "当前版本"),
    HISTORICAL(2, "历史版本");

    private final Integer value;

    private final String desc;

    BpmDefinitionLifecycleStateEnum(Integer value, String desc) {
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
