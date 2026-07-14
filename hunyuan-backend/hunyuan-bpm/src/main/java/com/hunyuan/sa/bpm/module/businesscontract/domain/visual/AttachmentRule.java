package com.hunyuan.sa.bpm.module.businesscontract.domain.visual;
import java.util.List;
public record AttachmentRule(Integer maxCount,Integer maxSizeMb,List<String> allowedExtensions,boolean required){}
