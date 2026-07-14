package com.hunyuan.sa.bpm.module.businesscontract.domain.visual;
import java.util.List;
public record BpmBusinessObjectDraft(String contractKey,String objectName,String description,String sourceSystem,String businessType,Long catalogRevision,BusinessKeyRule businessKeyRule,List<BusinessObjectField> fieldSchema,List<BusinessObjectField> routingFacts,List<BusinessObjectField> workingDataSchema,LineItemSchema lineItemSchema,AttachmentRule attachmentRule,DataChangeRule dataChangeRule){
 public BpmBusinessObjectDraft withWorkingDataSchema(List<BusinessObjectField> fields){return new BpmBusinessObjectDraft(contractKey,objectName,description,sourceSystem,businessType,catalogRevision,businessKeyRule,fieldSchema,routingFacts,fields,lineItemSchema,attachmentRule,dataChangeRule);}
}
