package com.hunyuan.sa.admin.module.organization.position.domain;

import java.time.LocalDateTime;

/**
 * 岗位目录领域模型。
 */
public record Position(
        Long positionId,
        String positionName,
        String positionLevel,
        Integer sort,
        String remark,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
