package com.hunyuan.sa.admin.module.access.datascope.api;

import com.hunyuan.sa.base.common.enumeration.BaseEnum;

/**
 * access 统一拥有的数据范围类型。
 */
public enum AccessDataScopeType implements BaseEnum {

    /**
     * 系统通知。
     */
    NOTICE(1, 20, "系统通知", "系统通知数据范围"),

    /**
     * 组织目录。
     */
    ORGANIZATION_DIRECTORY(2, 30, "组织目录", "组织目录部门数据范围");

    private final Integer value;

    private final Integer sort;

    private final String name;

    private final String desc;

    AccessDataScopeType(Integer value, Integer sort, String name, String desc) {
        this.value = value;
        this.sort = sort;
        this.name = name;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    public Integer getSort() {
        return sort;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}
