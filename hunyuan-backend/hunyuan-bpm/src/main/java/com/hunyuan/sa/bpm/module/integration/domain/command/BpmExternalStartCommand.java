package com.hunyuan.sa.bpm.module.integration.domain.command;

public record BpmExternalStartCommand(String requestId, String contractKey, Integer contractVersion,
                                      String businessType, String businessKey,
                                      String scenario, String externalEmployeeId, String title,
                                      String formDataJson) {
}
