package com.hunyuan.sa.bpm.module.businesscontract.domain.vo;
import java.util.List;
public record BpmBusinessObjectTechnicalDiffVO(String contractKey,Integer leftVersion,Integer rightVersion,List<String> changedFieldKeys){}
