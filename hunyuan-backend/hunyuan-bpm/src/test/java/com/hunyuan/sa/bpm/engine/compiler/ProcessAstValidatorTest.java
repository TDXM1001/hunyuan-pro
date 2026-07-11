package com.hunyuan.sa.bpm.engine.compiler;

import com.hunyuan.sa.bpm.engine.route.BpmRouteExpressionRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessAstValidatorTest {

    private final ProcessAstParser parser = new ProcessAstParser();

    private final ProcessAstValidator validator = new ProcessAstValidator();

    private final BpmRouteExpressionRegistry expressionRegistry = new BpmRouteExpressionRegistry(List.of());

    @Test
    void validateShouldRejectDuplicateNodeKeyAcrossNestedBranches() {
        List<ProcessValidationFinding> findings = validator.validate(
                parser.parse("""
                        {"schemaVersion":2,"nodes":[
                          {"nodeKey":"review","name":"顶层审批","type":"USER_TASK"},
                          {"nodeKey":"route","name":"路由","type":"EXCLUSIVE_BRANCH","branches":[
                            {"branchKey":"a","name":"A","nodes":[{"nodeKey":"review","name":"嵌套审批","type":"USER_TASK"}]},
                            {"branchKey":"default","name":"默认","isDefault":true,"nodes":[]}
                          ]}
                        ]}
                        """),
                "{\"fields\":[]}",
                expressionRegistry
        );

        assertThat(findings).extracting(ProcessValidationFinding::code)
                .contains("NODE_KEY_DUPLICATE");
    }

    @Test
    void validateShouldRejectEditableFieldInsideInclusiveBranch() {
        List<ProcessValidationFinding> findings = validator.validate(
                parser.parse("""
                        {"schemaVersion":2,"nodes":[
                          {"nodeKey":"route","name":"包容路由","type":"INCLUSIVE_BRANCH","branches":[
                            {"branchKey":"finance","name":"财务","condition":{"sourceType":"FORM_FIELD","fieldKey":"amount","valueType":"NUMBER","operator":"GT","compareValue":0},"nodes":[
                              {"nodeKey":"finance_review","name":"财务审批","type":"USER_TASK","fieldPermissions":[{"fieldKey":"approvedAmount","permission":"EDITABLE","required":true}]}
                            ]},
                            {"branchKey":"default","name":"默认","isDefault":true,"nodes":[]}
                          ]}
                        ]}
                        """),
                "{\"fields\":[{\"field\":\"amount\",\"type\":\"number\"},{\"field\":\"approvedAmount\",\"type\":\"number\"}]}",
                expressionRegistry
        );

        assertThat(findings).anySatisfy(finding -> {
            assertThat(finding.code()).isEqualTo("CONCURRENT_BRANCH_EDITABLE_FORBIDDEN");
            assertThat(finding.nodeKey()).isEqualTo("finance_review");
            assertThat(finding.fieldKey()).isEqualTo("approvedAmount");
        });
    }

    @Test
    void validateShouldRejectExclusiveBranchWithoutDefault() {
        List<ProcessValidationFinding> findings = validator.validate(
                parser.parse("""
                        {"schemaVersion":2,"nodes":[
                          {"nodeKey":"route","name":"排他路由","type":"EXCLUSIVE_BRANCH","branches":[
                            {"branchKey":"a","name":"A","condition":{"sourceType":"FORM_FIELD","fieldKey":"amount","valueType":"NUMBER","operator":"GT","compareValue":0},"nodes":[]},
                            {"branchKey":"b","name":"B","condition":{"sourceType":"FORM_FIELD","fieldKey":"amount","valueType":"NUMBER","operator":"LTE","compareValue":0},"nodes":[]}
                          ]}
                        ]}
                        """),
                "{\"fields\":[{\"field\":\"amount\",\"type\":\"number\"}]}",
                expressionRegistry
        );

        assertThat(findings).extracting(ProcessValidationFinding::code)
                .contains("ROUTE_DEFAULT_BRANCH_MISSING");
    }

    @Test
    void validateShouldRejectUnregisteredExpressionVersion() {
        List<ProcessValidationFinding> findings = validator.validate(
                parser.parse("""
                        {"schemaVersion":2,"nodes":[
                          {"nodeKey":"route","name":"登记路由","type":"EXCLUSIVE_BRANCH","branches":[
                            {"branchKey":"risk","name":"风险","condition":{"sourceType":"REGISTERED_EXPRESSION","expressionKey":"risk_check","version":2,"parameters":{}},"nodes":[]},
                            {"branchKey":"default","name":"默认","isDefault":true,"nodes":[]}
                          ]}
                        ]}
                        """),
                "{\"fields\":[]}",
                expressionRegistry
        );

        assertThat(findings).extracting(ProcessValidationFinding::code)
                .contains("ROUTE_EXPRESSION_NOT_REGISTERED");
    }
}
