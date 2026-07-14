package com.hunyuan.sa.bpm.module.businesscontract.service;
import com.hunyuan.sa.bpm.module.businesscontract.domain.visual.BpmBusinessObjectDraft;
public class BusinessObjectBusinessSummaryService {
 public String summarize(BpmBusinessObjectDraft draft){String fields=draft.fieldSchema()==null?"":draft.fieldSchema().stream().map(field->field.label()).limit(3).reduce((a,b)->a+"、"+b).orElse("");String line=draft.lineItemSchema()==null?"无明细":"包含“"+draft.lineItemSchema().name()+"”";return "“"+draft.objectName()+"”包含"+fields+"等申请信息；"+line+"；最多上传"+draft.attachmentRule().maxCount()+"个附件。";}
}
