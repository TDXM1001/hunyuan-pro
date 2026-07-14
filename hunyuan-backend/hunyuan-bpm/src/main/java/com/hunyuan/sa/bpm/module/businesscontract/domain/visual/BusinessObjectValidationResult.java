package com.hunyuan.sa.bpm.module.businesscontract.domain.visual;
import java.util.List;
public record BusinessObjectValidationResult(boolean valid,String canonicalPayload,String digest,String businessSummary,List<BusinessObjectValidationFinding> findings){}
