package com.hunyuan.sa.admin.module.organization.position.domain;

/**
 * 岗位新增和更新命令。
 */
public record PositionCommand(
        String positionName,
        String positionLevel,
        Integer sort,
        String remark) {
}
