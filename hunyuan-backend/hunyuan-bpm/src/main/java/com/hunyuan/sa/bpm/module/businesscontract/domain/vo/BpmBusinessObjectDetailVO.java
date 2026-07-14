package com.hunyuan.sa.bpm.module.businesscontract.domain.vo;
import com.hunyuan.sa.bpm.module.businesscontract.domain.visual.*; import java.util.List;
public record BpmBusinessObjectDetailVO(String contractKey,Integer contractVersion,String lifecycleState,Integer schemaVersion,Long catalogRevision,String objectName,String description,String businessSummary,long referenceCount,BpmBusinessObjectDraft configuration,List<BusinessObjectValidationFinding> findings){}
