package com.hunyuan.sa.bpm.common.enumeration;

import com.hunyuan.sa.base.common.enumeration.BaseEnum;

/**
 * P0 候选人解析类型。
 */
public enum BpmCandidateResolverTypeEnum implements BaseEnum {

    EMPLOYEE("EMPLOYEE", "指定员工"),
    DEPARTMENT_MANAGER("DEPARTMENT_MANAGER", "部门主管"),
    ROLE("ROLE", "角色成员"),
    START_EMPLOYEE("START_EMPLOYEE", "发起人本人"),
    START_DEPARTMENT_MANAGER("START_DEPARTMENT_MANAGER", "发起人部门主管");

    private final String value;

    private final String desc;

    BpmCandidateResolverTypeEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}
