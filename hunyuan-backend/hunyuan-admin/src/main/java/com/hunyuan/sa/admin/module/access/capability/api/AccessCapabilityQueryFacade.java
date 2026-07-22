package com.hunyuan.sa.admin.module.access.capability.api;

import com.hunyuan.sa.admin.module.access.authorization.api.AccessMenuItem;

import java.util.List;

/**
 * 登录授权场景使用的能力与菜单查询边界。
 */
public interface AccessCapabilityQueryFacade {

    List<AccessMenuItem> listAuthorizationItems(
            List<Long> roleIds,
            Boolean administratorFlag);
}
