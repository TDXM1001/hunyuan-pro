package com.hunyuan.sa.bpm.module.definition.service;

import com.hunyuan.sa.bpm.engine.graph.GraphNode;
import com.hunyuan.sa.bpm.engine.graph.GraphNodeType;
import com.hunyuan.sa.bpm.engine.graph.HunyuanProcessDefinitionGraph;
import com.hunyuan.sa.bpm.module.businesscontract.domain.model.BusinessContractCatalogVersion;
import com.hunyuan.sa.bpm.module.businesscontract.service.BpmBusinessContractCatalogService;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyPublicationLease;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyReference;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyType;
import com.hunyuan.sa.bpm.module.candidate.domain.vo.BpmPolicyBusinessDetailVO;
import com.hunyuan.sa.bpm.module.candidate.service.BpmPolicyCatalogService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * M1 生产发布解析器。只信任 M2/M3 已启用的不可变目录版本，缺失时在部署前失败关闭。
 */
@Component
public class M2M3GraphPublicationDependencyResolver implements GraphPublicationDependencyResolver {

    private final BpmPolicyCatalogService policyCatalogService;
    private final BpmBusinessContractCatalogService businessContractCatalogService;

    public M2M3GraphPublicationDependencyResolver(
            BpmPolicyCatalogService policyCatalogService,
            BpmBusinessContractCatalogService businessContractCatalogService
    ) {
        this.policyCatalogService = policyCatalogService;
        this.businessContractCatalogService = businessContractCatalogService;
    }

    @Override
    public GraphPublicationDependencySnapshot resolve(HunyuanProcessDefinitionGraph graph) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessContract", resolveBusinessContract(graph));
        result.put("startVisibilityPolicy", resolvePolicy(
                graph.policies().get("startVisibilityPolicy"),
                "流程发起可见范围策略",
                PolicyType.START_VISIBILITY
        ));

        Map<String, Object> candidatePolicies = new LinkedHashMap<>();
        Map<String, Object> approvalPolicies = new LinkedHashMap<>();
        for (GraphNode node : graph.nodes()) {
            if (node.type() != GraphNodeType.APPROVAL && node.type() != GraphNodeType.HANDLE) {
                continue;
            }
            candidatePolicies.put(node.nodeId(), resolveCandidatePolicy(node));
            if (node.type() == GraphNodeType.APPROVAL) {
                approvalPolicies.put(node.nodeId(), resolvePolicy(
                        node.properties().get("approvalPolicy"),
                        "节点 " + node.nodeId() + " 的审批策略",
                        PolicyType.APPROVAL
                ));
            }
        }
        result.put("candidatePolicies", Map.copyOf(candidatePolicies));
        result.put("approvalPolicies", Map.copyOf(approvalPolicies));
        return new GraphPublicationDependencySnapshot(result);
    }

    private Map<String, Object> resolveBusinessContract(HunyuanProcessDefinitionGraph graph) {
        Map<String, Object> reference = readReference(graph.policies().get("businessContract"), "业务契约");
        String contractKey = requiredText(reference.get("contractKey"), "业务契约 contractKey 不能为空");
        int contractVersion = requiredVersion(reference.get("contractVersion"), "业务契约 contractVersion 必须为正整数");
        BusinessContractCatalogVersion contract;
        try {
            contract = businessContractCatalogService.freezeForPublication(contractKey, contractVersion);
        } catch (RuntimeException ex) {
            throw new GraphPublicationDependencyException(
                    "业务契约版本不存在、未启用或内容不合法："
                            + contractKey + "@" + contractVersion + "；" + ex.getMessage()
            );
        }
        return Map.of(
                "contractKey", contract.contractKey(),
                "contractVersion", contract.contractVersion(),
                "contractVersionId", contract.businessContractVersionId(),
                "schemaVersion", contract.schemaVersion(),
                "canonicalPayload", contract.canonicalContractJson(),
                "digest", contract.contractDigest()
        );
    }

    private Map<String, Object> resolveCandidatePolicy(GraphNode node) {
        return resolvePolicy(
                node.properties().get("candidatePolicy"),
                "节点 " + node.nodeId() + " 的候选策略",
                PolicyType.CANDIDATE
        );
    }

    private Map<String, Object> resolvePolicy(Object rawReference, String label, PolicyType type) {
        Map<String, Object> reference = readReference(rawReference, label);
        String policyKey = requiredText(reference.get("policyKey"), label + " policyKey 不能为空");
        int policyVersion = requiredVersion(reference.get("policyVersion"), label + " policyVersion 必须为正整数");
        PolicyPublicationLease lease;
        try {
            lease = policyCatalogService.freezeForPublication(
                    new PolicyReference(type, policyKey, policyVersion),
                    "graph-publication"
            );
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new GraphPublicationDependencyException(label + "版本不存在或未启用：" + policyKey + "@" + policyVersion, ex);
        }
        BpmPolicyBusinessDetailVO business;
        try {
            business = policyCatalogService.getBusinessDetail(lease.reference());
        } catch (RuntimeException ignored) {
            business = null;
        }
        return Map.of(
                "type", type.name(),
                "policyKey", lease.reference().policyKey(),
                "policyVersion", lease.reference().policyVersion(),
                "policyVersionId", lease.policyVersionId(),
                "schemaVersion", lease.schemaVersion(),
                "canonicalPayload", lease.canonicalPayload(),
                "digest", lease.digest(),
                "policyName", business == null ? policyKey : business.policyName(),
                "businessSummary", business == null ? "" : business.businessSummary(),
                "calculatedRiskLevel", business == null ? "UNKNOWN" : business.calculatedRiskLevel()
        );
    }

    private Map<String, Object> readReference(Object value, String label) {
        if (!(value instanceof Map<?, ?> rawReference)) {
            throw new GraphPublicationDependencyException(label + "不能为空");
        }
        Map<String, Object> reference = new LinkedHashMap<>();
        rawReference.forEach((key, nestedValue) -> reference.put(String.valueOf(key), nestedValue));
        return reference;
    }

    private String requiredText(Object value, String message) {
        String text = value == null ? null : String.valueOf(value).trim();
        if (StringUtils.isBlank(text)) {
            throw new GraphPublicationDependencyException(message);
        }
        return text;
    }

    private int requiredVersion(Object value, String message) {
        if (value instanceof Number number) {
            int version = number.intValue();
            if (number.doubleValue() == version && version > 0) {
                return version;
            }
        }
        if (value != null) {
            try {
                int version = Integer.parseInt(String.valueOf(value));
                if (version > 0) {
                    return version;
                }
            } catch (NumberFormatException ignored) {
                // 统一通过下方消息说明引用版本非法。
            }
        }
        throw new GraphPublicationDependencyException(message);
    }
}
