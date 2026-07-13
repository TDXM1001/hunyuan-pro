package com.hunyuan.sa.bpm.controller.app;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.approvaldata.domain.form.BpmGenericApplicationSubmitForm;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.GenericApplicationSubmitCommand;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.GenericApplicationSubmitResult;
import com.hunyuan.sa.bpm.module.approvaldata.service.BpmGenericApplicationService;
import com.hunyuan.sa.bpm.module.businesscontract.domain.model.BusinessContractCatalogVersion;
import com.hunyuan.sa.bpm.module.businesscontract.service.BpmBusinessContractCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "App BPM Generic Application")
public class AppBpmGenericApplicationController {
    @Resource
    private BpmGenericApplicationService genericApplicationService;
    @Resource
    private BpmBusinessContractCatalogService contractCatalogService;

    @Operation(summary = "查询内置通用申请可用业务契约")
    @GetMapping("/app/bpm/generic-application/contracts")
    public ResponseDTO<java.util.List<BusinessContractCatalogVersion>> contracts() {
        return ResponseDTO.ok(contractCatalogService.list(null, "ACTIVE"));
    }

    @Operation(summary = "提交内置通用申请并发起 Graph 流程")
    @PostMapping("/app/bpm/generic-application/submit")
    public ResponseDTO<GenericApplicationSubmitResult> submit(
            @RequestBody @Valid BpmGenericApplicationSubmitForm form
    ) {
        return ResponseDTO.ok(genericApplicationService.submit(new GenericApplicationSubmitCommand(
                form.getGraphDefinitionVersionId(), form.getContractKey(), form.getContractVersion(),
                form.getSourceSystem(), form.getBusinessType(), form.getBusinessKey(), form.getTitle(),
                form.getSummary(), form.getFieldsJson(), form.getLineItemsJson(), form.getAttachmentsJson(),
                form.getRoutingFactsJson(), form.getWorkingDataJson()
        )));
    }
}
