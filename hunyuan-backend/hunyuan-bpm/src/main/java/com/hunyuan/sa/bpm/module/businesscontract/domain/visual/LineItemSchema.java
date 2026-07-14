package com.hunyuan.sa.bpm.module.businesscontract.domain.visual;
import java.util.List;
public record LineItemSchema(String name,Integer minRows,Integer maxRows,List<BusinessObjectField> fields){}
