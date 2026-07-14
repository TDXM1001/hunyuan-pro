package com.hunyuan.sa.bpm.module.businesscontract.domain.vo;
public record BpmBusinessObjectTechnicalDetailVO(String contractKey,Integer contractVersion,Integer schemaVersion,String canonicalPayload,String digest){}
