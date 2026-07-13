package com.hunyuan.sa.bpm.candidate;

import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyType;
import com.hunyuan.sa.bpm.module.candidate.service.PolicyDocumentValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyDocumentValidatorTest {

    @Test
    void managementChainShouldRequireTypeSeedAndDepthBeforeActivation() {
        PolicyDocumentValidator validator = new PolicyDocumentValidator();

        assertThatThrownBy(() -> validator.validate(
                PolicyType.CANDIDATE,
                1,
                "{\"resolverType\":\"MANAGEMENT_CHAIN\",\"resolverParameters\":{},"
                        + "\"emptyCandidatePolicy\":\"BLOCK\",\"selfApprovalPolicy\":\"BLOCK\"}"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chainType");
    }

    @Test
    void namedFallbackShouldRequireMatchingTypedIdentityReference() {
        PolicyDocumentValidator validator = new PolicyDocumentValidator();

        assertThatThrownBy(() -> validator.validate(
                PolicyType.CANDIDATE,
                1,
                "{\"resolverType\":\"EMPLOYEE\",\"resolverParameters\":{\"employeeIds\":[1]},"
                        + "\"emptyCandidatePolicy\":\"ASSIGN_NAMED_EMPLOYEE\",\"selfApprovalPolicy\":\"BLOCK\"}"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fallbackIdentityReference");
    }

    @Test
    void approvalRatioShouldRejectOutOfRangeThreshold() {
        PolicyDocumentValidator validator = new PolicyDocumentValidator();

        assertThatThrownBy(() -> validator.validate(
                PolicyType.APPROVAL,
                1,
                "{\"completionMode\":\"RATIO\",\"ratioPercent\":0,\"rejectionRule\":\"WHEN_APPROVAL_UNREACHABLE\"}"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ratioPercent");
    }

    @Test
    void approvalPolicyShouldRequireAtLeastOneExplicitAllowedAction() {
        PolicyDocumentValidator validator = new PolicyDocumentValidator();

        assertThatThrownBy(() -> validator.validate(
                PolicyType.APPROVAL,
                1,
                "{\"completionMode\":\"ALL\",\"ratioPercent\":100,\"rejectionRule\":\"IMMEDIATE\"}"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("allowedActions");
    }

    @Test
    void startVisibilityShouldRejectFreeFormExpression() {
        PolicyDocumentValidator validator = new PolicyDocumentValidator();

        assertThatThrownBy(() -> validator.validate(
                PolicyType.START_VISIBILITY,
                1,
                "{\"startScope\":{\"type\":\"EXPRESSION\",\"value\":\"#{allUsers()}\"},\"visibilityScope\":{\"type\":\"ALL\"}}"
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
