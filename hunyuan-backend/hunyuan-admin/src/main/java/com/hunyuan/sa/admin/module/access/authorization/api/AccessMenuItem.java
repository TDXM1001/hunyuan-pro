package com.hunyuan.sa.admin.module.access.authorization.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AccessMenuItem {

    @Schema(description = "菜单ID")
    private Long menuId;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "类型")
    private Integer menuType;

    @Schema(description = "父菜单ID")
    private Long parentId;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "路由地址")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "是否为外链")
    private Boolean frameFlag;

    @Schema(description = "外链地址")
    private String frameUrl;

    @Schema(description = "是否缓存")
    private Boolean cacheFlag;

    @Schema(description = "显示状态")
    private Boolean visibleFlag;

    @Schema(description = "禁用状态")
    private Boolean disabledFlag;

    @Schema(description = "权限类型")
    private Integer permsType;

    @Schema(description = "前端权限字符串")
    private String webPerms;

    @Schema(description = "后端权限字符串")
    private String apiPerms;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "功能点关联菜单ID")
    private Long contextMenuId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人")
    private Long createUserId;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "更新人")
    private Long updateUserId;
}
