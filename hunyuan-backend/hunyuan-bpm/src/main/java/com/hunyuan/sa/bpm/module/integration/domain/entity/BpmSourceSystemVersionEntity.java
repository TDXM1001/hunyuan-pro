package com.hunyuan.sa.bpm.module.integration.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("t_bpm_source_system_version")
public class BpmSourceSystemVersionEntity {
    @TableId(type = IdType.AUTO) private Long sourceSystemVersionId;
    private String sourceSystemCode; private Integer systemVersion; private String systemName;
    private String endpointRegistryJson; private String networkPolicyJson; private String status;
    private LocalDateTime publishedAt;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
}
