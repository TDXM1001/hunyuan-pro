package com.hunyuan.sa.bpm.module.integration.domain.command;
public record BpmExternalTaskActionCommand(String requestId,String externalEmployeeId,String action,Long taskVersion,String commentText,Long formDataVersion,String formDataPatchJson) {}
