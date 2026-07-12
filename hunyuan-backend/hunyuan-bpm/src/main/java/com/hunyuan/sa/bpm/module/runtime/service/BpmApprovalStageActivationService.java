package com.hunyuan.sa.bpm.module.runtime.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalCompletionMode;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalPolicyDocument;
import com.hunyuan.sa.bpm.module.candidate.domain.model.CandidateResolutionContext;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyPublicationLease;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyReference;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyType;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ResolvedCandidateSnapshot;
import com.hunyuan.sa.bpm.module.candidate.service.CandidateResolutionService;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionElementMappingDao;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionElementMappingEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Graph receive task 到 M4 冻结阶段的唯一激活入口。
 *
 * authored 节点只从已发布的 compiled mapping 取得，不从转义后的 BPMN activity ID 反推。
 */
@Service
public class BpmApprovalStageActivationService {

    private static final long CURRENT_TENANT_ID = 1L;

    private final BpmInstanceDao bpmInstanceDao;
    private final GraphDefinitionVersionDao graphDefinitionVersionDao;
    private final GraphDefinitionElementMappingDao graphDefinitionElementMappingDao;
    private final BpmApprovalStageDao bpmApprovalStageDao;
    private final CandidateResolutionService candidateResolutionService;
    private final BpmApprovalStageService bpmApprovalStageService;
    private final BpmTaskProjectionService bpmTaskProjectionService;

    public BpmApprovalStageActivationService(
            BpmInstanceDao bpmInstanceDao,
            GraphDefinitionVersionDao graphDefinitionVersionDao,
            GraphDefinitionElementMappingDao graphDefinitionElementMappingDao,
            BpmApprovalStageDao bpmApprovalStageDao,
            CandidateResolutionService candidateResolutionService,
            BpmApprovalStageService bpmApprovalStageService,
            BpmTaskProjectionService bpmTaskProjectionService
    ) {
        this.bpmInstanceDao = bpmInstanceDao;
        this.graphDefinitionVersionDao = graphDefinitionVersionDao;
        this.graphDefinitionElementMappingDao = graphDefinitionElementMappingDao;
        this.bpmApprovalStageDao = bpmApprovalStageDao;
        this.candidateResolutionService = candidateResolutionService;
        this.bpmApprovalStageService = bpmApprovalStageService;
        this.bpmTaskProjectionService = bpmTaskProjectionService;
    }

    @Transactional(rollbackFor = Exception.class)
    public BpmApprovalStageEntity activate(ActivateApprovalStageCommand command) {
        validate(command);
        // 同一实例内的等待点激活串行化，确保不同 execution 取得不同 generation，
        // 同一 execution 的重试则在锁内看到既有阶段。
        BpmInstanceEntity instance = bpmInstanceDao.selectByIdForUpdate(command.instanceId());
        if (instance == null) {
            throw new IllegalStateException("Graph 审批等待点对应的 Hunyuan 实例不存在");
        }
        BpmApprovalStageEntity existing = bpmApprovalStageDao.selectByStageInvocationId(command.engineExecutionId());
        if (existing != null) {
            verifyExistingBinding(existing, command);
            bpmTaskProjectionService.projectActiveApprovalStageMembers(existing);
            return existing;
        }
        GraphDefinitionVersionEntity version = graphDefinitionVersionDao.selectById(command.graphDefinitionVersionId());
        if (version == null) {
            throw new IllegalStateException("Graph 审批等待点对应的定义版本不存在");
        }
        GraphDefinitionElementMappingEntity mapping = graphDefinitionElementMappingDao
                .selectByGraphDefinitionVersionIdAndCompiledElementId(
                        command.graphDefinitionVersionId(),
                        command.compiledActivityId()
                );
        validateApprovalMapping(mapping, command);

        JSONObject dependencies = parseDependencies(version);
        JSONObject candidatePolicy = requiredNodePolicy(
                dependencies,
                "candidatePolicies",
                mapping.getAuthoredElementId()
        );
        JSONObject approvalPolicy = requiredNodePolicy(
                dependencies,
                "approvalPolicies",
                mapping.getAuthoredElementId()
        );
        String stageInvocationId = command.engineExecutionId();
        PolicyPublicationLease candidateLease = frozenPolicy(
                candidatePolicy,
                PolicyType.CANDIDATE,
                stageInvocationId
        );
        PolicyPublicationLease approvalLease = frozenPolicy(
                approvalPolicy,
                PolicyType.APPROVAL,
                stageInvocationId
        );
        ApprovalPolicyDocument approvalDocument = approvalPolicyDocument(approvalLease.canonicalPayload());
        ResolvedCandidateSnapshot candidates = candidateResolutionService.resolve(
                candidateLease,
                new CandidateResolutionContext(
                        CURRENT_TENANT_ID,
                        command.graphDefinitionVersionId(),
                        mapping.getAuthoredElementId(),
                        stageInvocationId,
                        startEmployee(instance),
                        LocalDateTime.now()
                )
        );
        Integer nextGeneration = bpmApprovalStageDao.selectNextGeneration(
                command.instanceId(),
                mapping.getAuthoredElementId()
        );
        BpmApprovalStageEntity stage = bpmApprovalStageService.open(
                new BpmApprovalStageService.OpenApprovalStageCommand(
                        command.instanceId(),
                        CURRENT_TENANT_ID,
                        command.graphDefinitionVersionId(),
                        mapping.getAuthoredElementId(),
                        nextGeneration == null ? 0 : nextGeneration,
                        stageInvocationId,
                        candidateLease.policyVersionId(),
                        candidateLease.digest(),
                        approvalLease.policyVersionId(),
                        approvalLease.digest(),
                        approvalDocument,
                        candidates,
                        command.engineProcessInstanceId(),
                        command.engineExecutionId()
                )
        );
        bpmTaskProjectionService.projectActiveApprovalStageMembers(stage);
        return stage;
    }

    private void validate(ActivateApprovalStageCommand command) {
        if (command == null
                || command.instanceId() == null || command.instanceId() <= 0
                || command.graphDefinitionVersionId() == null || command.graphDefinitionVersionId() <= 0
                || isBlank(command.compiledActivityId())
                || isBlank(command.engineProcessInstanceId())
                || isBlank(command.engineExecutionId())) {
            throw new IllegalArgumentException("Graph 审批阶段激活参数不完整");
        }
    }

    private void verifyExistingBinding(BpmApprovalStageEntity stage, ActivateApprovalStageCommand command) {
        if (!command.engineProcessInstanceId().equals(stage.getEngineProcessInstanceId())
                || !command.engineExecutionId().equals(stage.getEngineExecutionId())
                || !command.graphDefinitionVersionId().equals(stage.getDefinitionVersionId())) {
            throw new IllegalStateException("Graph 审批等待点的引擎绑定与已冻结阶段不一致");
        }
    }

    private void validateApprovalMapping(
            GraphDefinitionElementMappingEntity mapping,
            ActivateApprovalStageCommand command
    ) {
        if (mapping == null
                || !command.graphDefinitionVersionId().equals(mapping.getGraphDefinitionVersionId())
                || !"NODE".equals(mapping.getAuthoredElementKind())
                || !"receiveTask".equals(mapping.getCompiledElementType())
                || isBlank(mapping.getAuthoredElementId())) {
            throw new IllegalStateException("Graph 审批等待点缺少已发布 authored/compiled 映射");
        }
    }

    private JSONObject parseDependencies(GraphDefinitionVersionEntity version) {
        JSONObject dependencies = JSON.parseObject(version.getDependencyVersionsJson());
        if (dependencies == null) {
            throw new IllegalStateException("Graph 定义版本缺少冻结策略依赖");
        }
        return dependencies;
    }

    private JSONObject requiredNodePolicy(
            JSONObject dependencies,
            String category,
            String authoredNodeId
    ) {
        JSONObject policies = dependencies.getJSONObject(category);
        JSONObject policy = policies == null ? null : policies.getJSONObject(authoredNodeId);
        if (policy == null) {
            throw new IllegalStateException("Graph 审批节点缺少冻结策略：" + authoredNodeId + "/" + category);
        }
        return policy;
    }

    private PolicyPublicationLease frozenPolicy(
            JSONObject snapshot,
            PolicyType type,
            String stageInvocationId
    ) {
        String policyKey = snapshot.getString("policyKey");
        Integer policyVersion = snapshot.getInteger("policyVersion");
        Long policyVersionId = snapshot.getLong("policyVersionId");
        Integer schemaVersion = snapshot.getInteger("schemaVersion");
        String canonicalPayload = snapshot.getString("canonicalPayload");
        String digest = snapshot.getString("digest");
        if (isBlank(policyKey)
                || policyVersion == null || policyVersion <= 0
                || policyVersionId == null || policyVersionId <= 0
                || schemaVersion == null || schemaVersion <= 0
                || isBlank(canonicalPayload)
                || isBlank(digest)) {
            throw new IllegalStateException("Graph 定义版本的冻结策略内容不完整");
        }
        return new PolicyPublicationLease(
                new PolicyReference(type, policyKey, policyVersion),
                policyVersionId,
                schemaVersion,
                canonicalPayload,
                digest,
                stageInvocationId
        );
    }

    private ApprovalPolicyDocument approvalPolicyDocument(String canonicalPayload) {
        JSONObject payload = JSON.parseObject(canonicalPayload);
        if (payload == null) {
            throw new IllegalStateException("冻结审批策略内容非法");
        }
        String rawMode = payload.getString("completionMode");
        Integer ratioPercent = payload.getInteger("ratioPercent");
        String rejectionRule = payload.getString("rejectionRule");
        if (isBlank(rawMode) || ratioPercent == null) {
            throw new IllegalStateException("冻结审批策略内容不完整");
        }
        if (isBlank(rejectionRule)) {
            throw new IllegalStateException("冻结审批策略 rejectionRule 不能为空");
        }
        JSONArray rawAllowedActions = payload.getJSONArray("allowedActions");
        if (rawAllowedActions == null || rawAllowedActions.isEmpty()) {
            throw new IllegalStateException("冻结审批策略 allowedActions 不能为空");
        }
        Set<String> allowedActions = new HashSet<>();
        for (Object rawAction : rawAllowedActions) {
            if (!(rawAction instanceof String action)) {
                throw new IllegalStateException("冻结审批策略 allowedActions 非法");
            }
            allowedActions.add(action);
        }
        try {
            return new ApprovalPolicyDocument(
                    ApprovalCompletionMode.valueOf(rawMode.toUpperCase(Locale.ROOT)),
                    ratioPercent,
                    rejectionRule,
                    allowedActions
            );
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("冻结审批策略完成模式非法", ex);
        }
    }

    private BpmEmployeeSnapshot startEmployee(BpmInstanceEntity instance) {
        return new BpmEmployeeSnapshot(
                instance.getStartEmployeeId(),
                instance.getStartEmployeeNameSnapshot(),
                instance.getStartDepartmentIdSnapshot(),
                instance.getStartDepartmentNameSnapshot(),
                null,
                null
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record ActivateApprovalStageCommand(
            Long instanceId,
            Long graphDefinitionVersionId,
            String compiledActivityId,
            String engineProcessInstanceId,
            String engineExecutionId
    ) {
    }
}
