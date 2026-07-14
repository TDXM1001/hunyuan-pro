package com.hunyuan.sa.bpm.module.businesscontract.domain.vo;
public record BpmBusinessObjectSummaryVO(String contractKey,Integer contractVersion,String objectName,String description,String lifecycleState,Integer schemaVersion,Long catalogRevision,String businessSummary,long referenceCount){}
