package com.hunyuan.sa.bpm.common.enumeration;

import com.hunyuan.sa.base.common.enumeration.BaseEnum;

/**
 * 流程任务处理结果。
 */
public enum BpmTaskResultEnum implements BaseEnum {

    APPROVED(1, "审批通过"),
    REJECTED(2, "审批拒绝"),
    RETURNED(3, "退回发起人"),
    INSTANCE_CANCELLED(4, "实例已取消"),
    ADD_SIGN_REDUCED(5, "加签已减签"),
    RECALLED(6, "发起人撤回");

    private final Integer value;

    private final String desc;

    BpmTaskResultEnum(Integer value, String desc) {
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
