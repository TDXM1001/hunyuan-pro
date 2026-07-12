package com.hunyuan.sa.bpm.module.definition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hunyuan.sa.bpm.engine.graph.GraphNode;
import com.hunyuan.sa.bpm.engine.graph.GraphNodeType;
import com.hunyuan.sa.bpm.engine.graph.HunyuanProcessDefinitionGraph;
import com.hunyuan.sa.bpm.module.businesscontract.dao.BpmBusinessContractVersionDao;
import com.hunyuan.sa.bpm.module.businesscontract.domain.entity.BpmBusinessContractVersionEntity;
import com.hunyuan.sa.bpm.module.candidate.dao.BpmCandidatePolicyVersionDao;
import com.hunyuan.sa.bpm.module.candidate.domain.entity.BpmCandidatePolicyVersionEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * M1 生产发布解析器。只信任 M2/M3 已启用的不可变目录版本，缺失时在部署前失败关闭。
 */
@Component
public class M2M3GraphPublicationDependencyResolver implements GraphPublicationDependencyResolver {

    private static final String ACTIVE = "ACTIVE";

    private final BpmCandidatePolicyVersionDao candidatePolicyVersionDao;
    private final BpmBusinessContractVersionDao businessContractVersionDao;

    public M2M3GraphPublicationDependencyResolver(
            BpmCandidatePolicyVersionDao candidatePolicyVersionDao,
            BpmBusinessContractVersionDao businessContractVersionDao
    ) {
        this.candidatePolicyVersionDao = candidatePolicyVersionDao;
        this.businessContractVersionDao = businessContractVersionDao;
    }

    @Override
    public GraphPublicationDependencySnapshot resolve(HunyuanProcessDefinitionGraph graph) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessContract", resolveBusinessContract(graph));

        Map<String, Object> candidatePolicies = new LinkedHashMap<>();
        for (GraphNode node : graph.nodes()) {
            if (node.type() != GraphNodeType.APPROVAL && node.type() != GraphNodeType.HANDLE) {
                continue;
            }
            candidatePolicies.put(node.nodeId(), resolveCandidatePolicy(node));
        }
        result.put("candidatePolicies", Map.copyOf(candidatePolicies));
        return new GraphPublicationDependencySnapshot(result);
    }

    private Map<String, Object> resolveBusinessContract(HunyuanProcessDefinitionGraph graph) {
        Map<String, Object> reference = readReference(graph.policies().get("businessContract"), "业务契约");
        String contractKey = requiredText(reference.get("contractKey"), "业务契约 contractKey 不能为空");
        int contractVersion = requiredVersion(reference.get("contractVersion"), "业务契约 contractVersion 必须为正整数");
        BpmBusinessContractVersionEntity entity = businessContractVersionDao.selectOne(
                new LambdaQueryWrapper<BpmBusinessContractVersionEntity>()
                        .eq(BpmBusinessContractVersionEntity::getContractKey, contractKey)
                        .eq(BpmBusinessContractVersionEntity::getContractVersion, contractVersion)
                        .eq(BpmBusinessContractVersionEntity::getLifecycleState, ACTIVE)
        );
        if (entity == null) {
            throw new GraphPublicationDependencyException(
                    "业务契约版本不存在或未启用：" + contractKey + "@" + contractVersion
            );
        }
        return Map.of(
                "contractKey", entity.getContractKey(),
                "contractVersion", entity.getContractVersion(),
                "contractVersionId", entity.getBusinessContractVersionId()
        );
    }

    private Map<String, Object> resolveCandidatePolicy(GraphNode node) {
        Map<String, Object> reference = readReference(node.properties().get("candidatePolicy"), "节点 " + node.nodeId() + " 的候选策略");
        String policyKey = requiredText(reference.get("policyKey"), "节点 " + node.nodeId() + " 的候选策略 policyKey 不能为空");
        int policyVersion = requiredVersion(reference.get("policyVersion"), "节点 " + node.nodeId() + " 的候选策略 policyVersion 必须为正整数");
        BpmCandidatePolicyVersionEntity entity = candidatePolicyVersionDao.selectOne(
                new LambdaQueryWrapper<BpmCandidatePolicyVersionEntity>()
                        .eq(BpmCandidatePolicyVersionEntity::getPolicyKey, policyKey)
                        .eq(BpmCandidatePolicyVersionEntity::getPolicyVersion, policyVersion)
                        .eq(BpmCandidatePolicyVersionEntity::getLifecycleState, ACTIVE)
        );
        if (entity == null) {
            throw new GraphPublicationDependencyException(
                    "候选策略版本不存在或未启用：" + policyKey + "@" + policyVersion
            );
        }
        return Map.of(
                "policyKey", entity.getPolicyKey(),
                "policyVersion", entity.getPolicyVersion(),
                "policyVersionId", entity.getCandidatePolicyVersionId()
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
