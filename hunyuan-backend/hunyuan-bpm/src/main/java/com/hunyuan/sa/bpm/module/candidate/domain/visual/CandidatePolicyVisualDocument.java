package com.hunyuan.sa.bpm.module.candidate.domain.visual;

public record CandidatePolicyVisualDocument(
        String resolverType,
        PolicyIdentityReference identityReference,
        String resolutionPhase,
        String memberOrder,
        String emptyCandidatePolicy,
        String selfApprovalPolicy,
        PolicyIdentityReference fallbackIdentityReference,
        String clientRiskLevel
) {

    public CandidatePolicyVisualDocument withEmptyCandidatePolicy(String policy) {
        return new CandidatePolicyVisualDocument(
                resolverType, identityReference, resolutionPhase, memberOrder, policy,
                selfApprovalPolicy, fallbackIdentityReference, clientRiskLevel
        );
    }
}
