package com.hunyuan.sa.bpm.module.businesscontract.domain.vo;
public record BpmBusinessObjectReferenceVO(Long graphDefinitionVersionId,Long draftId,String referenceSource,String processKey,String processName,Integer version,String lifecycleState){}
