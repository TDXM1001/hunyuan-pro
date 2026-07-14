package com.hunyuan.sa.bpm.module.approvaldata.domain.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.alibaba.fastjson.JSON;
import java.util.List;
import java.util.Map;

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
    private Map<String, Object> fields;
    private List<Map<String, Object>> lineItems;
    private List<Map<String, Object>> attachments;
    private Map<String, Object> routingFacts;
    private Map<String, Object> workingData;
    private String fieldsJson;
    private String lineItemsJson;
    private String attachmentsJson;
    private String routingFactsJson;
    private String workingDataJson;

    public String fieldsPayload() { return fields == null ? defaultPayload(fieldsJson, "{}") : JSON.toJSONString(fields); }
    public String lineItemsPayload() { return lineItems == null ? defaultPayload(lineItemsJson, "[]") : JSON.toJSONString(lineItems); }
    public String attachmentsPayload() { return attachments == null ? defaultPayload(attachmentsJson, "[]") : JSON.toJSONString(attachments); }
    public String routingFactsPayload() { return routingFacts == null ? defaultPayload(routingFactsJson, "{}") : JSON.toJSONString(routingFacts); }
    public String workingDataPayload() { return workingData == null ? defaultPayload(workingDataJson, "{}") : JSON.toJSONString(workingData); }
    private String defaultPayload(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
}
