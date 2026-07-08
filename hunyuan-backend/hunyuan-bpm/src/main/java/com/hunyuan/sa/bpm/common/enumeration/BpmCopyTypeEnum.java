package com.hunyuan.sa.bpm.common.enumeration;

/**
 * 流程抄送类型。
 */
public enum BpmCopyTypeEnum {

    MANUAL_APPROVE_COPY("审批通过手工抄送"),
    MANUAL_REJECT_COPY("审批拒绝手工抄送"),
    MANUAL_RETURN_COPY("退回发起人手工抄送");

    private final String desc;

    BpmCopyTypeEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
