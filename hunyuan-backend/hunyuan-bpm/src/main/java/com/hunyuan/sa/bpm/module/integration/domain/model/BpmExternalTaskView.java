package com.hunyuan.sa.bpm.module.integration.domain.model;

import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmFieldPermissionVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskFormContextVO;

import java.util.List;

public record BpmExternalTaskView(String taskNo, String instanceNo, String title, String taskName,
                                  Long taskVersion, List<String> availableActions,
                                  BpmTaskFormContextVO formContext, ApprovalSubjectView approvalSubject) {
    public record ApprovalSubjectView(String viewState, String diagnosticMessage, String title, String summary,
                                      String fieldsJson, String lineItemsJson, String attachmentsJson,
                                      String workingDataJson, Long workingDataVersion,
                                      List<BpmFieldPermissionVO> fieldPermissions) {
    }
}
