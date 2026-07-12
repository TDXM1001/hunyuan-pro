package com.hunyuan.sa.bpm.module.integration.domain.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 登记连接器保存表单。
 */
@Data
public class BpmConnectorSaveForm {
    private Long connectorDefinitionId;
    @NotBlank
    private String connectorKey;
    @NotNull
    @Min(1)
    private Integer connectorVersion;
    @NotBlank
    private String connectorName;
    @NotBlank
    private String baseEndpointRef;
    private String credentialRef;
    @NotBlank
    private String allowedOperationsJson;
    @NotNull
    @Min(100)
    private Integer timeoutMillis;
    @NotBlank
    private String retryPolicyJson;
    private String circuitPolicyJson;
    @NotBlank
    private String requestSchemaJson;
    @NotBlank
    private String responseSchemaJson;
    @NotBlank
    private String enabledState;
}
