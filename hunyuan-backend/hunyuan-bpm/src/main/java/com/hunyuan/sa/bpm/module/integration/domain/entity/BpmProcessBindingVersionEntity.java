package com.hunyuan.sa.bpm.module.integration.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("t_bpm_process_binding_version")
public class BpmProcessBindingVersionEntity {
    @TableId(type = IdType.AUTO) private Long bindingVersionId;
    private String bindingKey; private Integer bindingVersion; private String businessType;
    private Long organizationId; private String scenario; private String conditionJson; private Integer priority;
    private Long graphDefinitionVersionId; private String status; private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveUntil; private LocalDateTime publishedAt;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
}
