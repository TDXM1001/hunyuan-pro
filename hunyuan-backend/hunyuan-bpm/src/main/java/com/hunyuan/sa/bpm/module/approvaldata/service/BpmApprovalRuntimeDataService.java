package com.hunyuan.sa.bpm.module.approvaldata.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmApprovalSubjectSnapshotDao;
import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmProcessWorkingDataDao;
import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmRoutingFactSnapshotDao;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmApprovalSubjectSnapshotEntity;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmProcessWorkingDataEntity;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmRoutingFactSnapshotEntity;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.ApprovalRuntimeBinding;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.RoutingDataSnapshot;
import com.hunyuan.sa.bpm.module.candidate.domain.model.RoutingFactView;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service
public class BpmApprovalRuntimeDataService {

    private final BpmApprovalSubjectSnapshotDao subjectDao;
    private final BpmRoutingFactSnapshotDao routingFactDao;
    private final BpmProcessWorkingDataDao workingDataDao;

    public BpmApprovalRuntimeDataService(
            BpmApprovalSubjectSnapshotDao subjectDao,
            BpmRoutingFactSnapshotDao routingFactDao,
            BpmProcessWorkingDataDao workingDataDao
    ) {
        this.subjectDao = subjectDao;
        this.routingFactDao = routingFactDao;
        this.workingDataDao = workingDataDao;
    }

    public ApprovalRuntimeBinding prepareForStart(Long subjectId, GraphDefinitionVersionEntity graphVersion) {
        if (subjectId == null) {
            throw new IllegalArgumentException("Graph 流程发起必须提供审批对象快照");
        }
        BpmApprovalSubjectSnapshotEntity subject = subjectDao.selectById(subjectId);
        if (subject == null || !"ACTIVE".equals(subject.getSnapshotState())) {
            throw new IllegalArgumentException("审批对象不存在或不可发起");
        }
        JSONObject dependencies = JSON.parseObject(graphVersion.getDependencyVersionsJson());
        JSONObject contract = dependencies == null ? null : dependencies.getJSONObject("businessContract");
        Long publishedContractVersionId = contract == null ? null : contract.getLong("contractVersionId");
        if (publishedContractVersionId == null
                || !publishedContractVersionId.equals(subject.getBusinessContractVersionId())) {
            throw new IllegalArgumentException("审批对象与流程发布的业务契约版本不一致");
        }
        BpmRoutingFactSnapshotEntity routing = routingFactDao.selectLatestBySubject(subjectId);
        BpmProcessWorkingDataEntity working = workingDataDao.selectLatestBySubject(subjectId);
        if (routing == null || working == null) {
            throw new IllegalStateException("审批对象缺少冻结路由事实或流程工作数据");
        }
        return new ApprovalRuntimeBinding(
                subject.getApprovalSubjectSnapshotId(),
                routing.getRoutingFactSnapshotId(),
                working.getProcessWorkingDataId(),
                subject.getBusinessContractVersionId(),
                subject.getSourceSystem(),
                subject.getBusinessType(),
                subject.getBusinessKey(),
                subject.getTitle(),
                subject.getSummary(),
                working.getDataJson(),
                working.getDataVersion()
        );
    }

    public RoutingFactView routingFactView(Long routingFactSnapshotId) {
        BpmRoutingFactSnapshotEntity snapshot = routingFactDao.selectById(routingFactSnapshotId);
        if (snapshot == null) {
            throw new IllegalStateException("实例引用的路由事实快照不存在");
        }
        JSONArray rawAllowedKeys = JSON.parseArray(snapshot.getAllowedFactKeysJson());
        JSONObject facts = JSON.parseObject(snapshot.getFactsJson());
        Set<String> allowedKeys = new LinkedHashSet<>();
        Map<String, Long> employeeFacts = new LinkedHashMap<>();
        if (rawAllowedKeys != null) {
            for (Object rawKey : rawAllowedKeys) {
                String key = String.valueOf(rawKey);
                allowedKeys.add(key);
                Object rawValue = facts == null ? null : facts.get(key);
                Long employeeId = toPositiveLong(rawValue);
                if (employeeId != null) {
                    employeeFacts.put(key, employeeId);
                }
            }
        }
        return new RoutingFactView(
                String.valueOf(snapshot.getBusinessContractVersionId()),
                String.valueOf(snapshot.getRoutingFactVersion()),
                allowedKeys,
                employeeFacts
        );
    }

    public RoutingDataSnapshot routingData(Long routingFactSnapshotId) {
        BpmRoutingFactSnapshotEntity snapshot = routingFactDao.selectById(routingFactSnapshotId);
        if (snapshot == null) {
            throw new IllegalStateException("实例引用的路由事实快照不存在");
        }
        JSONArray rawAllowedKeys = JSON.parseArray(snapshot.getAllowedFactKeysJson());
        JSONObject facts = JSON.parseObject(snapshot.getFactsJson());
        Map<String, Object> allowedFacts = new LinkedHashMap<>();
        if (rawAllowedKeys != null && facts != null) {
            for (Object rawKey : rawAllowedKeys) {
                String key = String.valueOf(rawKey);
                if (facts.containsKey(key)) {
                    allowedFacts.put(key, facts.get(key));
                }
            }
        }
        return new RoutingDataSnapshot(snapshot.getRoutingFactVersion(), Map.copyOf(allowedFacts));
    }

    private Long toPositiveLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            long number = value instanceof Number rawNumber
                    ? rawNumber.longValue()
                    : Long.parseLong(String.valueOf(value));
            return number > 0 ? number : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
