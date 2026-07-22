package com.hunyuan.sa.admin.module.identity.employee.api;

import com.hunyuan.sa.base.common.constant.StringConst;

public final class EmployeePasswordSalt {

    private EmployeePasswordSalt() {
    }

    public static String apply(String password, String employeeUid) {
        return password + StringConst.UNDERLINE
                + employeeUid.toUpperCase()
                + StringConst.UNDERLINE
                + employeeUid.toLowerCase();
    }
}
