package com.hunyuan.sa.admin.module.organization.position.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 岗位目录持久化实体。
 */
@Data
@TableName("t_position")
public class PositionPersistenceEntity {

    @TableId(value = "position_id", type = IdType.AUTO)
    private Long positionId;
    private String positionName;
    private String positionLevel;
    private Integer sort;
    private String remark;
    private Boolean deletedFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
