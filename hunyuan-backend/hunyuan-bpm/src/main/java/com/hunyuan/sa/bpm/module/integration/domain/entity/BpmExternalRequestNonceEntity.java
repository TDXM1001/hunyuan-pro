package com.hunyuan.sa.bpm.module.integration.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("t_bpm_external_request_nonce")
public class BpmExternalRequestNonceEntity {
    @TableId(type = IdType.AUTO) private Long nonceId;
    private Long applicationId; private String nonceValue; private LocalDateTime expiresAt;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
}
