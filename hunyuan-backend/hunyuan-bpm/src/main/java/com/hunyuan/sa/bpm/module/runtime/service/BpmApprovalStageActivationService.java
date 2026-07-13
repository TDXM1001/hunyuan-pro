package com.hunyuan.sa.bpm.module.runtime.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalCompletionMode;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalPolicyDocument;
import com.hunyuan.sa.bpm.module.candidate.domain.model.CandidateAutomaticOutcome;
import com.hunyuan.sa.bpm.module.candidate.domain.model.EngineEffect;
import com.hunyuan.sa.bpm.module.candidate.domain.model.CandidateResolutionContext;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyPublicationLease;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyReference;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyType;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ResolvedCandidateSnapshot;
import com.hunyuan.sa.bpm.module.candidate.domain.model.RoutingFactView;
import com.hunyuan.sa.bpm.module.candidate.service.CandidateResolutionService;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionElementMappingDao;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionElementMappingEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.event.BpmApprovalStageEngineEffectRequestedEvent;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceResultStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceRunStateEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

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

    private final ApplicationEventPublisher applicationEventPublisher;

    public BpmApprovalStageActivationService(
            BpmInstanceDao bpmInstanceDao,
            GraphDefinitionVersionDao graphDefinitionVersionDao,
            GraphDefinitionElementMappingDao graphDefinitionElementMappingDao,
            BpmApprovalStageDao bpmApprovalStageDao,
            CandidateResolutionService candidateResolutionService,
            BpmApprovalStageService bpmApprovalStageService,
            BpmTaskProjectionService bpmTaskProjectionService
    ) {
        this(
                bpmInstanceDao,
                graphDefinitionVersionDao,
                graphDefinitionElementMappingDao,
                bpmApprovalStageDao,
                candidateResolutionService,
                bpmApprovalStageService,
                bpmTaskProjectionService,
                event -> { }
        );
    }

    @Autowired
    public BpmApprovalStageActivationService(
            BpmInstanceDao bpmInstanceDao,
            GraphDefinitionVersionDao graphDefinitionVersionDao,
            GraphDefinitionElementMappingDao graphDefinitionElementMappingDao,
            BpmApprovalStageDao bpmApprovalStageDao,
            CandidateResolutionService candidateResolutionService,
            BpmApprovalStageService bpmApprovalStageService,
            BpmTaskProjectionService bpmTaskProjectionService,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.bpmInstanceDao = bpmInstanceDao;
        this.graphDefinitionVersionDao = graphDefinitionVersionDao;
        this.graphDefinitionElementMappingDao = graphDefinitionElementMappingDao;
        this.bpmApprovalStageDao = bpmApprovalStageDao;
        this.candidateResolutionService = candidateResolutionService;
        this.bpmApprovalStageService = bpmApprovalStageService;
        this.bpmTaskProjectionService = bpmTaskProjectionService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public BpmApprovalStageEntity activate(ActivateApprovalStageCommand command) {
        validate(command);
        // 同一实例内的等待点激活串行化，确保跨节点复用 execution 时仍取得独立 generation。
        BpmInstanceEntity instance = bpmInstanceDao.selectByIdForUpdate(command.instanceId());
        if (instance == null) {
            throw new IllegalStateException("Graph 审批等待点对应的 Hunyuan 实例不存在");
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
        BpmApprovalStageEntity existing = bpmApprovalStageDao.selectLatestByEngineBinding(
                command.instanceId(), mapping.getAuthoredElementId(), command.engineExecutionId()
        );
        if (existing != null) {
            verifyExistingBinding(existing, command);
            if (!isCompletedInvocation(existing)) {
                dispatchStage(existing);
            }
            return existing;
        }
        Integer nextGeneration = bpmApprovalStageDao.selectNextGeneration(
                command.instanceId(),
                mapping.getAuthoredElementId()
        );
        int generation = nextGeneration == null ? 0 : nextGeneration;

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
        String stageInvocationId = stageInvocationId(command, mapping.getAuthoredElementId(), generation);
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
                        routingFactView(dependencies, instance),
                        LocalDateTime.now()
                )
        );
        BpmApprovalStageEntity stage = bpmApprovalStageService.open(
                new BpmApprovalStageService.OpenApprovalStageCommand(
                        command.instanceId(),
                        CURRENT_TENANT_ID,
                        command.graphDefinitionVersionId(),
                        mapping.getAuthoredElementId(),
                        generation,
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
        applyAutomaticInstanceTerminal(instance, candidates.automaticOutcome());
        dispatchStage(stage);
        return stage;
    }

    private RoutingFactView routingFactView(JSONObject dependencies, BpmInstanceEntity instance) {
        JSONObject contractSnapshot = dependencies.getJSONObject("businessContract");
        if (contractSnapshot == null) {
            return new RoutingFactView("unknown", routeFactVersion(instance), Set.of(), Map.of());
        }
        JSONObject contract = JSON.parseObject(contractSnapshot.getString("canonicalPayload"));
        JSONObject declarations = contract == null ? null : contract.getJSONObject("routingFacts");
        if (declarations == null || declarations.isEmpty()) {
            return new RoutingFactView(businessContractVersion(contractSnapshot), routeFactVersion(instance), Set.of(), Map.of());
        }
        JSONObject formData = JSON.parseObject(instance.getCurrentFormDataSnapshotJson());
        Set<String> allowedKeys = new HashSet<>();
        Map<String, Long> employeeFacts = new LinkedHashMap<>();
        for (String factKey : declarations.keySet()) {
            JSONObject declaration = declarations.getJSONObject(factKey);
            if (declaration == null || !declaration.getBooleanValue("candidateAllowed")
                    || !"EMPLOYEE_ID".equals(declaration.getString("type"))) {
                continue;
            }
            allowedKeys.add(factKey);
            String sourceField = declaration.getString("sourceField");
            Object rawValue = formData == null || sourceField == null ? null : formData.get(sourceField);
            Long employeeId = toPositiveLong(rawValue, factKey);
            if (employeeId != null) {
                employeeFacts.put(factKey, employeeId);
            }
        }
        return new RoutingFactView(
                businessContractVersion(contractSnapshot), routeFactVersion(instance), allowedKeys, employeeFacts
        );
    }

    private Long toPositiveLong(Object rawValue, String factKey) {
        if (rawValue == null) {
            return null;
        }
        try {
            long value = rawValue instanceof Number number
                    ? number.longValue()
                    : Long.parseLong(String.valueOf(rawValue));
            if (value <= 0) {
                throw new IllegalArgumentException("人员路由事实必须为正整数：" + factKey);
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("人员路由事实类型错误：" + factKey, ex);
        }
    }

    private String businessContractVersion(JSONObject snapshot) {
        return snapshot.getString("contractKey") + "@" + snapshot.getInteger("contractVersion");
    }

    private String routeFactVersion(BpmInstanceEntity instance) {
        return "form-data@" + (instance.getFormDataVersion() == null ? 1L : instance.getFormDataVersion());
    }

    private void dispatchStage(BpmApprovalStageEntity stage) {
        if ("APPROVED".equals(stage.getStageState()) || "REJECTED".equals(stage.getStageState())) {
            applicationEventPublisher.publishEvent(new BpmApprovalStageEngineEffectRequestedEvent(
                    stage.getStageInvocationId(),
                    "APPROVED".equals(stage.getStageState()) ? EngineEffect.COMPLETE_ONCE : EngineEffect.CLOSE_ONCE,
                    stage.getTerminalReason()
            ));
            return;
        }
        bpmTaskProjectionService.projectActiveApprovalStageMembers(stage);
    }

    private void applyAutomaticInstanceTerminal(
            BpmInstanceEntity instance,
            CandidateAutomaticOutcome automaticOutcome
    ) {
        if (automaticOutcome != CandidateAutomaticOutcome.AUTO_REJECT) {
            return;
        }
        BpmInstanceEntity update = new BpmInstanceEntity();
        update.setInstanceId(instance.getInstanceId());
        update.setRunState(BpmInstanceRunStateEnum.FINISHED.getValue());
        update.setResultState(BpmInstanceResultStateEnum.REJECTED.getValue());
        update.setActiveTaskCount(0);
        update.setCurrentNodeSummaryJson(null);
        update.setLastActionAt(LocalDateTime.now());
        update.setFinishedAt(LocalDateTime.now());
        bpmInstanceDao.updateById(update);
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

    private boolean isCompletedInvocation(BpmApprovalStageEntity stage) {
        return Set.of("APPROVED", "REJECTED", "RETURNED", "CANCELLED").contains(stage.getStageState())
                && "COMPLETED".equals(stage.getEngineEffectState());
    }

    private String stageInvocationId(
            ActivateApprovalStageCommand command,
            String authoredNodeId,
            int generation
    ) {
        String binding = command.instanceId() + "|" + command.graphDefinitionVersionId()
                + "|" + authoredNodeId + "|" + generation
                + "|" + command.engineProcessInstanceId() + "|" + command.engineExecutionId();
        return "stage-" + UUID.nameUUIDFromBytes(binding.getBytes(StandardCharsets.UTF_8));
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
        if (isBlank(rawMode)) {
            throw new IllegalStateException("冻结审批策略内容不完整");
        }
        if (ratioPercent == null) {
            if ("RATIO".equalsIgnoreCase(rawMode)) {
                throw new IllegalStateException("RATIO 冻结审批策略 ratioPercent 不能为空");
            }
            ratioPercent = 100;
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
