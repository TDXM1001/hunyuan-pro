package com.hunyuan.sa.bpm.common.enumeration;

import com.hunyuan.sa.base.common.enumeration.BaseEnum;

/**
 * 流程实例结果状态。
 */
public enum BpmInstanceResultStateEnum implements BaseEnum {

    APPROVED(1, "审批通过"),
    REJECTED(2, "审批拒绝"),
    CANCELLED_BY_START_USER(3, "发起人取消"),
    CANCELLED_BY_ADMIN(4, "管理员取消");

    private final Integer value;

    private final String desc;

    BpmInstanceResultStateEnum(Integer value, String desc) {
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
