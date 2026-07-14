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
import com.hunyuan.sa.bpm.module.businesscontract.domain.form.BpmBusinessObjectDraftForm;
import com.hunyuan.sa.bpm.module.businesscontract.domain.form.BpmBusinessObjectTechnicalDiffForm;
import com.hunyuan.sa.bpm.module.businesscontract.domain.vo.*;
import com.hunyuan.sa.bpm.module.businesscontract.domain.visual.BusinessObjectValidationResult;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException; import java.net.URLEncoder; import java.nio.charset.StandardCharsets;
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
    public ResponseDTO<List<BpmBusinessObjectSummaryVO>> list(
            @RequestParam(required = false) String contractKey,
            @RequestParam(required = false) String lifecycleState
    ) {
        return ResponseDTO.ok(catalogService.listBusiness(contractKey, lifecycleState));
    }

    @GetMapping("/bpm/business-contract/detail/{contractKey}/{contractVersion}")
    @SaCheckPermission("bpm:business-contract:detail")
    public ResponseDTO<BpmBusinessObjectDetailVO> detail(
            @PathVariable String contractKey,
            @PathVariable Integer contractVersion
    ) {
        return ResponseDTO.ok(catalogService.getBusinessDetail(contractKey, contractVersion));
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

    @PostMapping("/bpm/business-contract/visual-draft/create") @SaCheckPermission("bpm:business-contract:save")
    public ResponseDTO<BpmBusinessObjectDetailVO> createVisual(@RequestBody @Valid BpmBusinessObjectDraftForm form){return ResponseDTO.ok(catalogService.createVisualDraft(form.toDraft(),actorProvider.requireCurrentEmployeeId()));}
    @PostMapping("/bpm/business-contract/visual-draft/save") @SaCheckPermission("bpm:business-contract:save")
    public ResponseDTO<BpmBusinessObjectDetailVO> saveVisual(@RequestBody @Valid BpmBusinessObjectDraftForm form){return ResponseDTO.ok(catalogService.saveVisualDraft(form.getContractVersion(),form.toDraft(),actorProvider.requireCurrentEmployeeId()));}
    @PostMapping("/bpm/business-contract/visual-draft/validate") @SaCheckPermission("bpm:business-contract:save")
    public ResponseDTO<BusinessObjectValidationResult> validateVisual(@RequestBody @Valid BpmBusinessObjectDraftForm form){return ResponseDTO.ok(catalogService.validateVisualDraft(form.toDraft()));}
    @PostMapping("/bpm/business-contract/upgrade-v2") @SaCheckPermission("bpm:business-contract:upgrade")
    public ResponseDTO<BpmBusinessObjectDetailVO> upgrade(@RequestBody @Valid BpmBusinessContractReferenceForm form){return ResponseDTO.ok(catalogService.upgradeV1AsV2Draft(form.getContractKey(),form.getContractVersion(),actorProvider.requireCurrentEmployeeId()));}
    @PostMapping("/bpm/business-contract/draft/delete") @SaCheckPermission("bpm:business-contract:delete")
    public ResponseDTO<String> deleteDraft(@RequestBody @Valid BpmBusinessContractLifecycleForm form){catalogService.deleteDraft(form.getContractKey(),form.getContractVersion(),form.getCatalogRevision());return ResponseDTO.ok();}
    @GetMapping("/bpm/business-contract/references/{contractKey}/{contractVersion}") @SaCheckPermission("bpm:business-contract:detail")
    public ResponseDTO<List<BpmBusinessObjectReferenceVO>> references(@PathVariable String contractKey,@PathVariable Integer contractVersion){return ResponseDTO.ok(catalogService.references(contractKey,contractVersion));}
    @GetMapping("/bpm/business-contract/technical-detail/{contractKey}/{contractVersion}") @SaCheckPermission("bpm:business-contract:technical")
    public ResponseDTO<BpmBusinessObjectTechnicalDetailVO> technicalDetail(@PathVariable String contractKey,@PathVariable Integer contractVersion){return ResponseDTO.ok(catalogService.technicalDetail(contractKey,contractVersion));}
    @PostMapping("/bpm/business-contract/technical-diff") @SaCheckPermission("bpm:business-contract:technical")
    public ResponseDTO<BpmBusinessObjectTechnicalDiffVO> technicalDiff(@RequestBody @Valid BpmBusinessObjectTechnicalDiffForm form){return ResponseDTO.ok(catalogService.technicalDiff(form.getContractKey(),form.getLeftVersion(),form.getRightVersion()));}
    @GetMapping("/bpm/business-contract/technical-export/{contractKey}/{contractVersion}") @SaCheckPermission("bpm:business-contract:technical")
    public void technicalExport(@PathVariable String contractKey,@PathVariable Integer contractVersion,HttpServletResponse response)throws IOException{byte[] content=catalogService.exportCanonical(contractKey,contractVersion).getBytes(StandardCharsets.UTF_8);String name=URLEncoder.encode(contractKey+"-v"+contractVersion+".json",StandardCharsets.UTF_8).replace("+","%20");response.setContentType("application/json;charset=UTF-8");response.setHeader("Content-Disposition","attachment; filename*=UTF-8''"+name);response.setContentLength(content.length);response.getOutputStream().write(content);}

    private BusinessContractLifecycleCommand lifecycle(BpmBusinessContractLifecycleForm form) {
        return new BusinessContractLifecycleCommand(
                form.getContractKey(), form.getContractVersion(), form.getCatalogRevision(),
                actorProvider.requireCurrentEmployeeId()
        );
    }
}
