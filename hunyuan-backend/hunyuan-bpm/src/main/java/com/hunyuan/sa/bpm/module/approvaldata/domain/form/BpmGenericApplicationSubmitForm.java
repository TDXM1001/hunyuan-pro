package com.hunyuan.sa.bpm.module.approvaldata.domain.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BpmGenericApplicationSubmitForm {
    @NotNull private Long graphDefinitionVersionId;
    @NotBlank private String contractKey;
    @NotNull @Min(1) private Integer contractVersion;
    @NotBlank private String sourceSystem;
    @NotBlank private String businessType;
    @NotBlank private String businessKey;
    @NotBlank private String title;
    private String summary;
    @NotBlank private String fieldsJson;
    @NotBlank private String lineItemsJson;
    @NotBlank private String attachmentsJson;
    @NotBlank private String routingFactsJson;
    @NotBlank private String workingDataJson;
}
