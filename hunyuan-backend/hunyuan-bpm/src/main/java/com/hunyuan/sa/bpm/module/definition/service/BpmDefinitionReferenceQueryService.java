package com.hunyuan.sa.bpm.module.definition.service;

import com.alibaba.fastjson.JSON;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyReference;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionReferenceVO;
import com.hunyuan.sa.bpm.module.model.dao.BpmProcessDraftDao;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class BpmDefinitionReferenceQueryService {

    @Resource
    private GraphDefinitionVersionDao graphDefinitionVersionDao;
    @Resource
    private BpmProcessDraftDao bpmProcessDraftDao;

    public List<BpmDefinitionReferenceVO> findPolicyReferences(PolicyReference reference) {
        List<BpmDefinitionReferenceVO> published = graphDefinitionVersionDao.selectList(null).stream()
                .filter(version -> containsReference(parse(version.getDependencyVersionsJson()), reference))
                .map(this::toVO)
                .toList();
        List<BpmDefinitionReferenceVO> drafts = bpmProcessDraftDao.selectList(null).stream()
                .filter(draft -> containsReference(parse(draft.getGraphJson()), reference, false))
                .map(draft -> new BpmDefinitionReferenceVO(null, draft.getDraftId(), "DRAFT",
                        draft.getProcessKey(), draft.getProcessName(), draft.getRevision(), draft.getDraftStatus()))
                .toList();
        return java.util.stream.Stream.concat(published.stream(), drafts.stream()).toList();
    }

    private Object parse(String json) {
        return json == null || json.isBlank() ? Map.of() : JSON.parse(json);
    }

    private boolean containsReference(Object value, PolicyReference reference) {
        return containsReference(value, reference, true);
    }

    private boolean containsReference(Object value, PolicyReference reference, boolean requireType) {
        if (value instanceof Map<?, ?> map) {
            boolean typeMatches = !requireType || map.get("type") == null
                    || reference.type().name().equals(String.valueOf(map.get("type")));
            if (typeMatches
                    && reference.policyKey().equals(String.valueOf(map.get("policyKey")))
                    && String.valueOf(reference.policyVersion()).equals(String.valueOf(map.get("policyVersion")))) {
                return true;
            }
            return map.values().stream().anyMatch(nested -> containsReference(nested, reference, requireType));
        }
        return value instanceof Collection<?> collection
                && collection.stream().anyMatch(nested -> containsReference(nested, reference, requireType));
    }

    private BpmDefinitionReferenceVO toVO(GraphDefinitionVersionEntity entity) {
        return new BpmDefinitionReferenceVO(
                entity.getGraphDefinitionVersionId(), entity.getDraftId(), "PUBLISHED",
                entity.getProcessKey(), entity.getProcessNameSnapshot(),
                entity.getDefinitionVersion(), entity.getLifecycleState()
        );
    }
}
