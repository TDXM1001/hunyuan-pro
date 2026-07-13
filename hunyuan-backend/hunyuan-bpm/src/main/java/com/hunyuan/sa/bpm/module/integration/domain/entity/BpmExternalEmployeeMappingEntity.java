package com.hunyuan.sa.bpm.module.integration.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("t_bpm_external_employee_mapping")
public class BpmExternalEmployeeMappingEntity {
    @TableId(type = IdType.AUTO) private Long mappingId;
    private String sourceSystemCode; private String externalEmployeeId; private Long hunyuanEmployeeId;
    private String status; private LocalDateTime validFrom; private LocalDateTime validUntil;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
}
