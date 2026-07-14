package com.hunyuan.sa.bpm.module.approvaldata.domain.vo;

import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmFieldPermissionVO;
import lombok.Data;

import java.util.List;
import com.hunyuan.sa.bpm.module.businesscontract.domain.visual.BpmBusinessObjectDraft;

@Data
public class BpmApprovalSubjectContextVO {

    private String viewState;
    private String diagnosticMessage;
    private Long approvalSubjectSnapshotId;
    private String title;
    private String summary;
    private String fieldsJson;
    private String lineItemsJson;
    private String attachmentsJson;
    private String workingDataJson;
    private Long workingDataVersion;
    private List<BpmFieldPermissionVO> fieldPermissions;
    private BpmBusinessObjectDraft businessObjectConfiguration;
}
