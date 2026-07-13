package com.hunyuan.sa.bpm.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.module.businesscontract.domain.form.BpmBusinessContractDraftForm;
import com.hunyuan.sa.bpm.module.businesscontract.domain.form.BpmBusinessContractLifecycleForm;
import com.hunyuan.sa.bpm.module.businesscontract.domain.form.BpmBusinessContractReferenceForm;
import com.hunyuan.sa.bpm.module.businesscontract.domain.model.BusinessContractCatalogVersion;
import com.hunyuan.sa.bpm.module.businesscontract.domain.model.BusinessContractDraftCommand;
import com.hunyuan.sa.bpm.module.businesscontract.domain.model.BusinessContractLifecycleCommand;
import com.hunyuan.sa.bpm.module.businesscontract.domain.model.BusinessContractValidationResult;
import com.hunyuan.sa.bpm.module.businesscontract.service.BpmBusinessContractCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "BPM Business Contract Catalog")
public class AdminBpmBusinessContractController {

    @Resource
    private BpmBusinessContractCatalogService catalogService;
    @Resource
    private BpmCurrentActorProvider actorProvider;

    @GetMapping("/bpm/business-contract/list")
    @SaCheckPermission("bpm:business-contract:list")
    public ResponseDTO<List<BusinessContractCatalogVersion>> list(
            @RequestParam(required = false) String contractKey,
            @RequestParam(required = false) String lifecycleState
    ) {
        return ResponseDTO.ok(catalogService.list(contractKey, lifecycleState));
    }

    @GetMapping("/bpm/business-contract/detail/{contractKey}/{contractVersion}")
    @SaCheckPermission("bpm:business-contract:detail")
    public ResponseDTO<BusinessContractCatalogVersion> detail(
            @PathVariable String contractKey,
            @PathVariable Integer contractVersion
    ) {
        return ResponseDTO.ok(catalogService.get(contractKey, contractVersion));
    }

    @PostMapping("/bpm/business-contract/validate")
    @SaCheckPermission("bpm:business-contract:add")
    public ResponseDTO<BusinessContractValidationResult> validate(
            @RequestBody @Valid BpmBusinessContractDraftForm form
    ) {
        return ResponseDTO.ok(catalogService.validate(form.getSchemaVersion(), form.getContractJson()));
    }

    @PostMapping("/bpm/business-contract/draft")
    @SaCheckPermission("bpm:business-contract:add")
    public ResponseDTO<BusinessContractCatalogVersion> draft(
            @RequestBody @Valid BpmBusinessContractDraftForm form
    ) {
        return ResponseDTO.ok(catalogService.createDraft(new BusinessContractDraftCommand(
                form.getContractKey(), form.getSchemaVersion(), form.getContractJson(),
                actorProvider.requireCurrentEmployeeId()
        )));
    }

    @PostMapping("/bpm/business-contract/copy")
    @SaCheckPermission("bpm:business-contract:copy")
    public ResponseDTO<BusinessContractCatalogVersion> copy(
            @RequestBody @Valid BpmBusinessContractReferenceForm form
    ) {
        return ResponseDTO.ok(catalogService.copyAsDraft(
                form.getContractKey(), form.getContractVersion(), actorProvider.requireCurrentEmployeeId()
        ));
    }

    @PostMapping("/bpm/business-contract/activate")
    @SaCheckPermission("bpm:business-contract:activate")
    public ResponseDTO<BusinessContractCatalogVersion> activate(
            @RequestBody @Valid BpmBusinessContractLifecycleForm form
    ) {
        return ResponseDTO.ok(catalogService.activate(lifecycle(form)));
    }

    @PostMapping("/bpm/business-contract/retire")
    @SaCheckPermission("bpm:business-contract:retire")
    public ResponseDTO<BusinessContractCatalogVersion> retire(
            @RequestBody @Valid BpmBusinessContractLifecycleForm form
    ) {
        return ResponseDTO.ok(catalogService.retire(lifecycle(form)));
    }

    private BusinessContractLifecycleCommand lifecycle(BpmBusinessContractLifecycleForm form) {
        return new BusinessContractLifecycleCommand(
                form.getContractKey(), form.getContractVersion(), form.getCatalogRevision(),
                actorProvider.requireCurrentEmployeeId()
        );
    }
}
