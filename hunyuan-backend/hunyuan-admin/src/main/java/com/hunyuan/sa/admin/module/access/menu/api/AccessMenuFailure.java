package com.hunyuan.sa.admin.module.access.menu.api;

/**
 * 菜单目录用例的稳定失败原因。
 */
public enum AccessMenuFailure {
    MENU_NOT_FOUND,
    MENU_DELETED,
    MENU_NAME_DUPLICATED,
    WEB_PERMISSION_DUPLICATED,
    ROUTE_ID_REQUIRED,
    ROUTE_ID_DUPLICATED,
    MENU_PARENT_SELF,
    MENU_IDS_EMPTY
}
