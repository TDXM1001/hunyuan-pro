package com.hunyuan.sa.bpm.module.definition.service;

import com.alibaba.fastjson.JSON;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyReference;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionReferenceVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class BpmDefinitionReferenceQueryService {

    @Resource
    private GraphDefinitionVersionDao graphDefinitionVersionDao;

    public List<BpmDefinitionReferenceVO> findPolicyReferences(PolicyReference reference) {
        return graphDefinitionVersionDao.selectList(null).stream()
                .filter(version -> containsReference(parse(version.getDependencyVersionsJson()), reference))
                .map(this::toVO)
                .toList();
    }

    private Object parse(String json) {
        return json == null || json.isBlank() ? Map.of() : JSON.parse(json);
    }

    private boolean containsReference(Object value, PolicyReference reference) {
        if (value instanceof Map<?, ?> map) {
            if (reference.type().name().equals(String.valueOf(map.get("type")))
                    && reference.policyKey().equals(String.valueOf(map.get("policyKey")))
                    && String.valueOf(reference.policyVersion()).equals(String.valueOf(map.get("policyVersion")))) {
                return true;
            }
            return map.values().stream().anyMatch(nested -> containsReference(nested, reference));
        }
        return value instanceof Collection<?> collection
                && collection.stream().anyMatch(nested -> containsReference(nested, reference));
    }

    private BpmDefinitionReferenceVO toVO(GraphDefinitionVersionEntity entity) {
        return new BpmDefinitionReferenceVO(
                entity.getGraphDefinitionVersionId(), entity.getProcessKey(), entity.getProcessNameSnapshot(),
                entity.getDefinitionVersion(), entity.getLifecycleState()
        );
    }
}
