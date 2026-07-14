package com.hunyuan.sa.bpm.module.candidate.service;

import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.candidate.domain.form.BpmPolicySimulationForm;
import com.hunyuan.sa.bpm.module.candidate.domain.model.CandidateResolutionContext;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyPublicationLease;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyReference;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.PolicyValidationFinding;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.PolicyVisualCompilation;
import com.hunyuan.sa.bpm.module.candidate.domain.vo.BpmPolicySimulationMemberVO;
import com.hunyuan.sa.bpm.module.candidate.domain.vo.BpmPolicySimulationVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BpmPolicySimulationService {

    private final CandidateResolutionService candidateResolutionService;
    private final BpmOrgIdentityGateway identityGateway;
    private final PolicyVisualDocumentMapper mapper;

    public BpmPolicySimulationService(
            CandidateResolutionService candidateResolutionService,
            BpmOrgIdentityGateway identityGateway
    ) {
        this.candidateResolutionService = candidateResolutionService;
        this.identityGateway = identityGateway;
        PolicyCanonicalizer canonicalizer = new PolicyCanonicalizer();
        this.mapper = new PolicyVisualDocumentMapper(
                canonicalizer, new PolicyDocumentValidator(),
                new PolicyBusinessSummaryService(), new PolicyRiskAssessmentService()
        );
    }

    public BpmPolicySimulationVO simulate(BpmPolicySimulationForm form, long actorEmployeeId) {
        if (form == null || form.getDraft() == null || actorEmployeeId <= 0) {
            throw new IllegalArgumentException("模拟请求或当前操作员工不合法");
        }
        PolicyVisualCompilation compilation = mapper.compile(form.getDraft());
        if (!compilation.valid()) {
            return empty(compilation, compilation.findings());
        }
        if (form.getDraft().type() != com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyType.CANDIDATE) {
            return empty(compilation, List.of(new PolicyValidationFinding(
                    "SIMULATION_TYPE_UNSUPPORTED", "BLOCKING", "type",
                    "当前模拟仅支持审批人规则", "请选择审批人规则"
            )));
        }
        try {
            BpmEmployeeSnapshot starter = identityGateway.requireEmployee(form.getStarterEmployeeId());
            PolicyReference reference = new PolicyReference(
                    form.getDraft().type(), form.getDraft().policyKey(), 1);
            var resolved = candidateResolutionService.resolve(
                    new PolicyPublicationLease(reference, 0L, 2, compilation.canonicalPayload(),
                            compilation.digest(), "simulation"),
                    new CandidateResolutionContext(
                            null, null, "simulation", "simulation", starter, LocalDateTime.now())
            );
            var members = resolved.members().stream().map(member -> new BpmPolicySimulationMemberVO(
                    member.employeeId(), member.displayName(), member.departmentId(), member.departmentName()
            )).toList();
            return new BpmPolicySimulationVO(
                    members,
                    resolved.diagnostics().stream().map(diagnostic -> diagnostic.message()).toList(),
                    resolved.automaticOutcome() == null ? null : resolved.automaticOutcome().name(),
                    compilation.businessSummary(),
                    List.of()
            );
        } catch (RuntimeException ex) {
            String identityName = form.getDraft().candidate() == null
                    || form.getDraft().candidate().identityReference() == null
                    ? "已配置身份" : form.getDraft().candidate().identityReference().displayName();
            return empty(compilation, List.of(new PolicyValidationFinding(
                    "POLICY_SIMULATION_BLOCKED", "BLOCKING", "candidate.identityReference",
                    "“" + identityName + "”模拟失败：" + ex.getMessage(),
                    "请重新选择有效身份或调整兜底规则"
            )));
        }
    }

    private BpmPolicySimulationVO empty(
            PolicyVisualCompilation compilation, List<PolicyValidationFinding> findings
    ) {
        return new BpmPolicySimulationVO(
                List.of(), List.of(), null, compilation.businessSummary(), findings
        );
    }
}
