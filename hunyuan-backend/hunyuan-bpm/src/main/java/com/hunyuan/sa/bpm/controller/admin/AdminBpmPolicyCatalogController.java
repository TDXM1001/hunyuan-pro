package com.hunyuan.sa.bpm.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.module.candidate.domain.form.BpmPolicyCatalogDraftForm;
import com.hunyuan.sa.bpm.module.candidate.domain.form.BpmPolicyCatalogLifecycleForm;
import com.hunyuan.sa.bpm.module.candidate.domain.form.BpmPolicyCatalogHighRiskActivationForm;
import com.hunyuan.sa.bpm.module.candidate.domain.form.BpmPolicyCatalogReferenceForm;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyCatalogVersion;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyDraftCommand;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyLifecycleCommand;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyReference;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyType;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyValidationResult;
import com.hunyuan.sa.bpm.module.candidate.service.BpmPolicyCatalogService;
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

/**
 * M2 策略目录的管理端入口。所有生命周期操作由当前服务端员工身份审计。
 */
@RestController
@Tag(name = "BPM Policy Catalog")
public class AdminBpmPolicyCatalogController {

    @Resource
    private BpmPolicyCatalogService bpmPolicyCatalogService;

    @Resource
    private BpmCurrentActorProvider bpmCurrentActorProvider;

    @Operation(summary = "查询策略目录")
    @GetMapping("/bpm/policy-catalog/list")
    @SaCheckPermission("bpm:policy-catalog:list")
    public ResponseDTO<List<PolicyCatalogVersion>> list(
            @RequestParam PolicyType type,
            @RequestParam(required = false) String policyKey,
            @RequestParam(required = false) String lifecycleState
    ) {
        return ResponseDTO.ok(bpmPolicyCatalogService.list(type, policyKey, lifecycleState));
    }

    @Operation(summary = "读取精确策略版本")
    @GetMapping("/bpm/policy-catalog/detail/{type}/{policyKey}/{policyVersion}")
    @SaCheckPermission("bpm:policy-catalog:detail")
    public ResponseDTO<PolicyCatalogVersion> detail(
            @PathVariable PolicyType type,
            @PathVariable String policyKey,
            @PathVariable Integer policyVersion
    ) {
        return ResponseDTO.ok(bpmPolicyCatalogService.get(new PolicyReference(type, policyKey, policyVersion)));
    }

    @Operation(summary = "校验策略草稿")
    @PostMapping("/bpm/policy-catalog/validate")
    @SaCheckPermission("bpm:policy-catalog:add")
    public ResponseDTO<PolicyValidationResult> validate(@RequestBody @Valid BpmPolicyCatalogDraftForm form) {
        return ResponseDTO.ok(bpmPolicyCatalogService.validate(
                form.getType(), form.getSchemaVersion(), form.getPolicyJson()
        ));
    }

    @Operation(summary = "新建策略草稿版本")
    @PostMapping("/bpm/policy-catalog/draft")
    @SaCheckPermission("bpm:policy-catalog:add")
    public ResponseDTO<PolicyCatalogVersion> createDraft(@RequestBody @Valid BpmPolicyCatalogDraftForm form) {
        return ResponseDTO.ok(bpmPolicyCatalogService.createDraft(new PolicyDraftCommand(
                form.getType(),
                form.getPolicyKey(),
                form.getSchemaVersion(),
                form.getPolicyJson(),
                bpmCurrentActorProvider.requireCurrentEmployeeId()
        )));
    }

    @Operation(summary = "复制策略为新草稿版本")
    @PostMapping("/bpm/policy-catalog/copy")
    @SaCheckPermission("bpm:policy-catalog:copy")
    public ResponseDTO<PolicyCatalogVersion> copy(@RequestBody @Valid BpmPolicyCatalogReferenceForm form) {
        return ResponseDTO.ok(bpmPolicyCatalogService.copyAsDraft(
                reference(form), bpmCurrentActorProvider.requireCurrentEmployeeId()
        ));
    }

    @Operation(summary = "启用策略草稿版本")
    @PostMapping("/bpm/policy-catalog/activate")
    @SaCheckPermission("bpm:policy-catalog:activate")
    public ResponseDTO<PolicyCatalogVersion> activate(@RequestBody @Valid BpmPolicyCatalogLifecycleForm form) {
        return ResponseDTO.ok(bpmPolicyCatalogService.activate(lifecycleCommand(form)));
    }

    @Operation(summary = "独立确认并启用高风险策略版本")
    @PostMapping("/bpm/policy-catalog/activate-high-risk")
    @SaCheckPermission("bpm:policy-catalog:activate-high-risk")
    public ResponseDTO<PolicyCatalogVersion> activateHighRisk(
            @RequestBody @Valid BpmPolicyCatalogHighRiskActivationForm form
    ) {
        return ResponseDTO.ok(bpmPolicyCatalogService.activateHighRisk(
                lifecycleCommand(form), form.getConfirmationReason()
        ));
    }

    @Operation(summary = "退休已启用策略版本")
    @PostMapping("/bpm/policy-catalog/retire")
    @SaCheckPermission("bpm:policy-catalog:retire")
    public ResponseDTO<PolicyCatalogVersion> retire(@RequestBody @Valid BpmPolicyCatalogLifecycleForm form) {
        return ResponseDTO.ok(bpmPolicyCatalogService.retire(lifecycleCommand(form)));
    }

    private PolicyReference reference(BpmPolicyCatalogReferenceForm form) {
        return new PolicyReference(form.getType(), form.getPolicyKey(), form.getPolicyVersion());
    }

    private PolicyLifecycleCommand lifecycleCommand(BpmPolicyCatalogLifecycleForm form) {
        return new PolicyLifecycleCommand(
                reference(form),
                form.getCatalogRevision(),
                bpmCurrentActorProvider.requireCurrentEmployeeId()
        );
    }
}
