package com.hunyuan.sa.bpm.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.module.candidate.domain.form.BpmPolicyCatalogDraftForm;
import com.hunyuan.sa.bpm.module.candidate.domain.form.BpmPolicyCatalogLifecycleForm;
import com.hunyuan.sa.bpm.module.candidate.domain.form.BpmPolicyCatalogHighRiskActivationForm;
import com.hunyuan.sa.bpm.module.candidate.domain.form.BpmPolicyCatalogReferenceForm;
import com.hunyuan.sa.bpm.module.candidate.domain.form.BpmPolicyTechnicalDiffForm;
import com.hunyuan.sa.bpm.module.candidate.domain.form.BpmPolicyVisualDraftForm;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyCatalogVersion;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyDraftCommand;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyLifecycleCommand;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyReference;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyType;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyValidationResult;
import com.hunyuan.sa.bpm.module.candidate.domain.vo.BpmPolicyBusinessDetailVO;
import com.hunyuan.sa.bpm.module.candidate.domain.vo.BpmPolicyTechnicalDetailVO;
import com.hunyuan.sa.bpm.module.candidate.domain.vo.BpmPolicyTechnicalDiffVO;
import com.hunyuan.sa.bpm.module.candidate.domain.vo.BpmPolicyCatalogSummaryVO;
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
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
    public ResponseDTO<List<BpmPolicyCatalogSummaryVO>> list(
            @RequestParam PolicyType type,
            @RequestParam(required = false) String policyKey,
            @RequestParam(required = false) String lifecycleState
    ) {
        return ResponseDTO.ok(bpmPolicyCatalogService.listBusiness(type, policyKey, lifecycleState));
    }

    @Operation(summary = "读取精确策略版本")
    @GetMapping("/bpm/policy-catalog/detail/{type}/{policyKey}/{policyVersion}")
    @SaCheckPermission("bpm:policy-catalog:detail")
    public ResponseDTO<BpmPolicyBusinessDetailVO> detail(
            @PathVariable PolicyType type,
            @PathVariable String policyKey,
            @PathVariable Integer policyVersion
    ) {
        return ResponseDTO.ok(bpmPolicyCatalogService.getBusinessDetail(
                new PolicyReference(type, policyKey, policyVersion)));
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

    @Operation(summary = "保存可视化规则草稿")
    @PostMapping("/bpm/policy-catalog/visual-draft/save")
    @SaCheckPermission("bpm:policy-catalog:save")
    public ResponseDTO<BpmPolicyBusinessDetailVO> saveVisualDraft(
            @RequestBody @Valid BpmPolicyVisualDraftForm form
    ) {
        PolicyReference reference = new PolicyReference(
                form.getType(), form.getPolicyKey(), form.getPolicyVersion());
        return ResponseDTO.ok(bpmPolicyCatalogService.saveVisualDraft(
                reference, form.getCatalogRevision(), form.toVisualDraft(),
                bpmCurrentActorProvider.requireCurrentEmployeeId()
        ));
    }

    @Operation(summary = "读取只读技术协议")
    @GetMapping("/bpm/policy-catalog/technical-detail/{type}/{policyKey}/{policyVersion}")
    @SaCheckPermission("bpm:policy-catalog:technical")
    public ResponseDTO<BpmPolicyTechnicalDetailVO> technicalDetail(
            @PathVariable PolicyType type,
            @PathVariable String policyKey,
            @PathVariable Integer policyVersion
    ) {
        return ResponseDTO.ok(bpmPolicyCatalogService.technicalDetail(
                new PolicyReference(type, policyKey, policyVersion)));
    }

    @Operation(summary = "比较只读技术协议")
    @PostMapping("/bpm/policy-catalog/technical-diff")
    @SaCheckPermission("bpm:policy-catalog:technical")
    public ResponseDTO<BpmPolicyTechnicalDiffVO> technicalDiff(
            @RequestBody @Valid BpmPolicyTechnicalDiffForm form
    ) {
        return ResponseDTO.ok(bpmPolicyCatalogService.technicalDiff(
                form.toLeftReference(), form.toRightReference()));
    }

    @Operation(summary = "导出只读技术协议")
    @GetMapping("/bpm/policy-catalog/technical-export/{type}/{policyKey}/{policyVersion}")
    @SaCheckPermission("bpm:policy-catalog:technical")
    public void technicalExport(
            @PathVariable PolicyType type,
            @PathVariable String policyKey,
            @PathVariable Integer policyVersion,
            HttpServletResponse response
    ) throws IOException {
        PolicyReference reference = new PolicyReference(type, policyKey, policyVersion);
        byte[] content = bpmPolicyCatalogService.exportCanonical(reference).getBytes(StandardCharsets.UTF_8);
        String fileName = URLEncoder.encode(policyKey + "-v" + policyVersion + ".json", StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
        response.setContentLength(content.length);
        response.getOutputStream().write(content);
    }

    @Operation(summary = "删除未引用规则草稿")
    @PostMapping("/bpm/policy-catalog/draft/delete")
    @SaCheckPermission("bpm:policy-catalog:delete")
    public ResponseDTO<String> deleteDraft(@RequestBody @Valid BpmPolicyCatalogLifecycleForm form) {
        bpmPolicyCatalogService.deleteDraft(reference(form), form.getCatalogRevision());
        return ResponseDTO.ok();
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
