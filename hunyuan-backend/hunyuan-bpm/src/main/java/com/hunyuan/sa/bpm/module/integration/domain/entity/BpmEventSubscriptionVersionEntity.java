package com.hunyuan.sa.bpm.module.integration.domain.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.time.LocalDateTime;
@Data @TableName("t_bpm_event_subscription_version")
public class BpmEventSubscriptionVersionEntity {
 @TableId(type=IdType.AUTO) private Long subscriptionVersionId; private String subscriptionKey; private Integer subscriptionVersion;
 private String sourceSystemCode; private String businessType; private String eventType; private String connectorKey; private Integer connectorVersion; private String endpointOperation;
 private String signingSecretRef; private String retryPolicyJson; private String scopes; private String status; private LocalDateTime publishedAt;
 @TableField(fill=FieldFill.INSERT) private LocalDateTime createTime; @TableField(fill=FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
}
