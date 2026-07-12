package com.hunyuan.sa.bpm.module.integration.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BPM 登记连接器定义。
 */
@Data
@TableName("t_bpm_connector_definition")
public class BpmConnectorDefinitionEntity {

    @TableId(type = IdType.AUTO)
    private Long connectorDefinitionId;
    private String connectorKey;
    private Integer connectorVersion;
    private String connectorName;
    private String baseEndpointRef;
    private String credentialRef;
    private String allowedOperationsJson;
    private Integer timeoutMillis;
    private String retryPolicyJson;
    private String circuitPolicyJson;
    private String requestSchemaJson;
    private String responseSchemaJson;
    private String enabledState;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
