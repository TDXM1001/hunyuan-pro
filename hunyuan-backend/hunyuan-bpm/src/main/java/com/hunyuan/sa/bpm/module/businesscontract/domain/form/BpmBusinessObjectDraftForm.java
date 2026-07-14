package com.hunyuan.sa.bpm.module.businesscontract.domain.form;
import com.hunyuan.sa.bpm.module.businesscontract.domain.visual.*; import jakarta.validation.constraints.*; import lombok.Data; import java.util.List;
@Data public class BpmBusinessObjectDraftForm {
 @NotBlank private String contractKey; @NotNull @Min(1) private Integer contractVersion; @NotBlank private String objectName; private String description; @NotBlank private String sourceSystem; @NotBlank private String businessType; @NotNull private Long catalogRevision; @NotNull private BusinessKeyRule businessKeyRule; private List<BusinessObjectField> fieldSchema; private List<BusinessObjectField> routingFacts; private List<BusinessObjectField> workingDataSchema; private LineItemSchema lineItemSchema; @NotNull private AttachmentRule attachmentRule; @NotNull private DataChangeRule dataChangeRule;
 public BpmBusinessObjectDraft toDraft(){return new BpmBusinessObjectDraft(contractKey,objectName,description,sourceSystem,businessType,catalogRevision,businessKeyRule,fieldSchema,routingFacts,workingDataSchema,lineItemSchema,attachmentRule,dataChangeRule);}
}
