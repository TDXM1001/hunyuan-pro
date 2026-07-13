package com.hunyuan.sa.bpm.module.integration.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("t_bpm_source_application")
public class BpmSourceApplicationEntity {
    @TableId(type = IdType.AUTO) private Long applicationId;
    private Long sourceSystemVersionId; private String sourceSystemCode; private String applicationCode;
    private String secretRef; private String scopes; private String status; private LocalDateTime expiresAt;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
}
