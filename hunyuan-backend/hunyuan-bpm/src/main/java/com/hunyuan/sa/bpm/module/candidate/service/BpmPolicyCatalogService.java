package com.hunyuan.sa.bpm.module.candidate.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.hunyuan.sa.bpm.module.candidate.dao.BpmApprovalPolicyVersionDao;
import com.hunyuan.sa.bpm.module.candidate.dao.BpmCandidatePolicyVersionDao;
import com.hunyuan.sa.bpm.module.candidate.dao.BpmStartVisibilityPolicyVersionDao;
import com.hunyuan.sa.bpm.module.candidate.domain.entity.BpmApprovalPolicyVersionEntity;
import com.hunyuan.sa.bpm.module.candidate.domain.entity.BpmCandidatePolicyVersionEntity;
import com.hunyuan.sa.bpm.module.candidate.domain.entity.BpmStartVisibilityPolicyVersionEntity;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyPublicationLease;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyCatalogVersion;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyDraftCommand;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyLifecycleCommand;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyReference;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyType;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyValidationResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.Set;

/**
 * M2 策略版本目录的只读发布冻结入口。
 */
@Service
public class BpmPolicyCatalogService {

    private static final Logger LOGGER = Logger.getLogger(BpmPolicyCatalogService.class.getName());
    private static final String ACTIVE = "ACTIVE";

    private final BpmCandidatePolicyVersionDao candidatePolicyVersionDao;
    private final BpmApprovalPolicyVersionDao approvalPolicyVersionDao;
    private final BpmStartVisibilityPolicyVersionDao startVisibilityPolicyVersionDao;
    private final PolicyCanonicalizer canonicalizer;
    private final PolicyDocumentValidator validator;

    @Autowired
    public BpmPolicyCatalogService(
            BpmCandidatePolicyVersionDao candidatePolicyVersionDao,
            BpmApprovalPolicyVersionDao approvalPolicyVersionDao,
            BpmStartVisibilityPolicyVersionDao startVisibilityPolicyVersionDao
    ) {
        this(
                candidatePolicyVersionDao,
                approvalPolicyVersionDao,
                startVisibilityPolicyVersionDao,
                new PolicyCanonicalizer(),
                new PolicyDocumentValidator()
        );
    }

    BpmPolicyCatalogService(
            BpmCandidatePolicyVersionDao candidatePolicyVersionDao,
            BpmApprovalPolicyVersionDao approvalPolicyVersionDao,
            BpmStartVisibilityPolicyVersionDao startVisibilityPolicyVersionDao,
            PolicyCanonicalizer canonicalizer,
            PolicyDocumentValidator validator
    ) {
        this.candidatePolicyVersionDao = candidatePolicyVersionDao;
        this.approvalPolicyVersionDao = approvalPolicyVersionDao;
        this.startVisibilityPolicyVersionDao = startVisibilityPolicyVersionDao;
        this.canonicalizer = canonicalizer;
        this.validator = validator;
    }

    @Transactional(rollbackFor = Exception.class)
    public PolicyPublicationLease freezeForPublication(PolicyReference reference, String publicationRequestId) {
        if (StringUtils.isBlank(publicationRequestId)) {
            throw new IllegalArgumentException("发布请求标识不能为空");
        }
        return switch (reference.type()) {
            case CANDIDATE -> freezeCandidate(reference, publicationRequestId);
            case APPROVAL -> freezeApproval(reference, publicationRequestId);
            case START_VISIBILITY -> freezeStartVisibility(reference, publicationRequestId);
        };
    }

    /**
     * 仅草稿可启用；使用目录 revision 防止同一版本被并发启用或退休。
     */
    @Transactional(rollbackFor = Exception.class)
    public PolicyCatalogVersion activate(PolicyLifecycleCommand command) {
        return transition(command, "DRAFT", "ACTIVE", false, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public PolicyCatalogVersion activateHighRisk(PolicyLifecycleCommand command, String confirmationReason) {
        String normalizedReason = StringUtils.trimToNull(confirmationReason);
        if (normalizedReason == null || normalizedReason.length() > 512) {
            throw new IllegalArgumentException("高风险策略确认原因必须为 1 到 512 个字符");
        }
        return transition(command, "DRAFT", "ACTIVE", true, normalizedReason);
    }

    /**
     * 退休只影响未来发布，已冻结到定义版本的内容不会再读取目录当前值。
     */
    @Transactional(rollbackFor = Exception.class)
    public PolicyCatalogVersion retire(PolicyLifecycleCommand command) {
        return transition(command, "ACTIVE", "RETIRED", true, null);
    }

    /**
     * 每次编辑都创建新草稿版本，已启用或已退休版本从不被原地改写。
     */
    @Transactional(rollbackFor = Exception.class)
    public PolicyCatalogVersion createDraft(PolicyDraftCommand command) {
        validator.validate(command.type(), command.schemaVersion(), command.policyJson());
        String canonicalPayload = canonicalizer.canonicalize(command.policyJson());
        String digest = canonicalizer.sha256(canonicalPayload);
        int nextVersion = nextPolicyVersion(command.type(), command.policyKey());
        PolicyReference reference = new PolicyReference(command.type(), command.policyKey(), nextVersion);
        switch (command.type()) {
            case CANDIDATE -> createCandidateDraft(command, nextVersion, canonicalPayload, digest);
            case APPROVAL -> createApprovalDraft(command, nextVersion, canonicalPayload, digest);
            case START_VISIBILITY -> createStartVisibilityDraft(command, nextVersion, canonicalPayload, digest);
        }
        return new PolicyCatalogVersion(
                reference,
                "DRAFT",
                command.schemaVersion(),
                canonicalPayload,
                digest,
                0L
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public PolicyCatalogVersion copyAsDraft(PolicyReference sourceReference, Long createdByEmployeeId) {
        if (createdByEmployeeId == null || createdByEmployeeId <= 0) {
            throw new IllegalArgumentException("策略创建人员不能为空");
        }
        CatalogRecord source = loadForUpdate(sourceReference);
        if (source == null) {
            throw new IllegalArgumentException("策略版本不存在：" + sourceReference.policyKey() + "@" + sourceReference.policyVersion());
        }
        return createDraft(new PolicyDraftCommand(
                sourceReference.type(),
                sourceReference.policyKey(),
                source.schemaVersion() == null ? 1 : source.schemaVersion(),
                source.policyJson(),
                createdByEmployeeId
        ));
    }

    /**
     * 在不创建目录记录的前提下校验并规范化管理端策略草稿。
     */
    public PolicyValidationResult validate(PolicyType type, Integer schemaVersion, String policyJson) {
        validator.validate(type, schemaVersion, policyJson);
        String canonicalPayload = canonicalizer.canonicalize(policyJson);
        return new PolicyValidationResult(
                type,
                schemaVersion,
                canonicalPayload,
                canonicalizer.sha256(canonicalPayload)
        );
    }

    /**
     * 为管理端和 Graph 设计器读取受控的版本目录。内容始终重新校验并规范化，避免将历史脏数据暴露为可绑定策略。
     */
    public List<PolicyCatalogVersion> list(PolicyType type, String policyKey, String lifecycleState) {
        if (type == null) {
            throw new IllegalArgumentException("策略类型不能为空");
        }
        String normalizedPolicyKey = StringUtils.trimToNull(policyKey);
        String normalizedLifecycleState = normalizeLifecycleState(lifecycleState);
        return switch (type) {
            case CANDIDATE -> candidatePolicyVersionDao.selectList(
                    new LambdaQueryWrapper<BpmCandidatePolicyVersionEntity>()
                            .eq(normalizedPolicyKey != null, BpmCandidatePolicyVersionEntity::getPolicyKey, normalizedPolicyKey)
                            .eq(normalizedLifecycleState != null, BpmCandidatePolicyVersionEntity::getLifecycleState, normalizedLifecycleState)
                            .orderByAsc(BpmCandidatePolicyVersionEntity::getPolicyKey)
                            .orderByDesc(BpmCandidatePolicyVersionEntity::getPolicyVersion)
            ).stream().map(entity -> catalogVersionIfValid(
                    new PolicyReference(type, entity.getPolicyKey(), entity.getPolicyVersion()),
                    candidateRecord(entity)
            )).flatMap(Optional::stream).toList();
            case APPROVAL -> approvalPolicyVersionDao.selectList(
                    new LambdaQueryWrapper<BpmApprovalPolicyVersionEntity>()
                            .eq(normalizedPolicyKey != null, BpmApprovalPolicyVersionEntity::getPolicyKey, normalizedPolicyKey)
                            .eq(normalizedLifecycleState != null, BpmApprovalPolicyVersionEntity::getLifecycleState, normalizedLifecycleState)
                            .orderByAsc(BpmApprovalPolicyVersionEntity::getPolicyKey)
                            .orderByDesc(BpmApprovalPolicyVersionEntity::getPolicyVersion)
            ).stream().map(entity -> catalogVersionIfValid(
                    new PolicyReference(type, entity.getPolicyKey(), entity.getPolicyVersion()),
                    approvalRecord(entity)
            )).flatMap(Optional::stream).toList();
            case START_VISIBILITY -> startVisibilityPolicyVersionDao.selectList(
                    new LambdaQueryWrapper<BpmStartVisibilityPolicyVersionEntity>()
                            .eq(normalizedPolicyKey != null, BpmStartVisibilityPolicyVersionEntity::getPolicyKey, normalizedPolicyKey)
                            .eq(normalizedLifecycleState != null, BpmStartVisibilityPolicyVersionEntity::getLifecycleState, normalizedLifecycleState)
                            .orderByAsc(BpmStartVisibilityPolicyVersionEntity::getPolicyKey)
                            .orderByDesc(BpmStartVisibilityPolicyVersionEntity::getPolicyVersion)
            ).stream().map(entity -> catalogVersionIfValid(
                    new PolicyReference(type, entity.getPolicyKey(), entity.getPolicyVersion()),
                    startVisibilityRecord(entity)
            )).flatMap(Optional::stream).toList();
        };
    }

    public PolicyCatalogVersion get(PolicyReference reference) {
        CatalogRecord record = load(reference);
        if (record == null) {
            throw new IllegalArgumentException("策略版本不存在：" + reference.policyKey() + "@" + reference.policyVersion());
        }
        return catalogVersion(reference, record);
    }

    private PolicyPublicationLease freezeCandidate(PolicyReference reference, String requestId) {
        CatalogRecord record = loadCandidateForUpdate(reference);
        if (record == null || !ACTIVE.equals(record.lifecycleState())) {
            throw new IllegalStateException("候选策略版本不存在或未启用：" + reference.policyKey() + "@" + reference.policyVersion());
        }
        return lease(reference, record.policyVersionId(), record.schemaVersion(), record.policyJson(), requestId);
    }

    private PolicyPublicationLease freezeApproval(PolicyReference reference, String requestId) {
        CatalogRecord record = loadApprovalForUpdate(reference);
        if (record == null || !ACTIVE.equals(record.lifecycleState())) {
            throw new IllegalStateException("审批策略版本不存在或未启用：" + reference.policyKey() + "@" + reference.policyVersion());
        }
        return lease(reference, record.policyVersionId(), record.schemaVersion(), record.policyJson(), requestId);
    }

    private PolicyPublicationLease freezeStartVisibility(PolicyReference reference, String requestId) {
        CatalogRecord record = loadStartVisibilityForUpdate(reference);
        if (record == null || !ACTIVE.equals(record.lifecycleState())) {
            throw new IllegalStateException("发起可见范围策略版本不存在或未启用：" + reference.policyKey() + "@" + reference.policyVersion());
        }
        return lease(reference, record.policyVersionId(), record.schemaVersion(), record.policyJson(), requestId);
    }

    private PolicyPublicationLease lease(
            PolicyReference reference,
            Long policyVersionId,
            Integer schemaVersion,
            String policyJson,
            String requestId
    ) {
        if (policyVersionId == null || StringUtils.isBlank(policyJson)) {
            throw new IllegalStateException("策略版本内容不完整：" + reference.policyKey() + "@" + reference.policyVersion());
        }
        Integer normalizedSchemaVersion = schemaVersion == null ? 1 : schemaVersion;
        validator.validate(reference.type(), normalizedSchemaVersion, policyJson);
        String canonicalPayload = canonicalizer.canonicalize(policyJson);
        return new PolicyPublicationLease(
                reference,
                policyVersionId,
                normalizedSchemaVersion,
                canonicalPayload,
                canonicalizer.sha256(canonicalPayload),
                requestId
        );
    }

    private PolicyCatalogVersion catalogVersion(PolicyReference reference, CatalogRecord record) {
        if (record == null || StringUtils.isBlank(record.policyJson())) {
            throw new IllegalStateException("策略版本内容不完整：" + reference.policyKey() + "@" + reference.policyVersion());
        }
        Integer schemaVersion = record.schemaVersion() == null ? 1 : record.schemaVersion();
        validator.validate(reference.type(), schemaVersion, record.policyJson());
        String canonicalPayload = canonicalizer.canonicalize(record.policyJson());
        return new PolicyCatalogVersion(
                reference,
                record.lifecycleState(),
                schemaVersion,
                canonicalPayload,
                canonicalizer.sha256(canonicalPayload),
                normalizeRevision(record.catalogRevision())
        );
    }

    private Optional<PolicyCatalogVersion> catalogVersionIfValid(PolicyReference reference, CatalogRecord record) {
        try {
            return Optional.of(catalogVersion(reference, record));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            LOGGER.warning(() -> "忽略不符合当前 M2 schema 的历史策略版本："
                    + reference.policyKey() + "@" + reference.policyVersion() + "，原因：" + ex.getMessage());
            return Optional.empty();
        }
    }

    private PolicyCatalogVersion transition(
            PolicyLifecycleCommand command,
            String expectedState,
            String targetState,
            boolean allowHighRisk,
            String highRiskConfirmationReason
    ) {
        CatalogRecord record = loadForUpdate(command.reference());
        if (record == null) {
            throw new IllegalArgumentException("策略版本不存在：" + command.reference().policyKey() + "@" + command.reference().policyVersion());
        }
        if (!expectedState.equals(record.lifecycleState())) {
            throw new IllegalStateException("策略版本当前状态不允许变更：" + record.lifecycleState());
        }
        Long catalogRevision = normalizeRevision(record.catalogRevision());
        if (!Objects.equals(catalogRevision, command.expectedCatalogRevision())) {
            throw new IllegalStateException("策略目录版本已变更，请刷新后重试");
        }
        boolean highRisk = isHighRisk(command.reference().type(), record.policyJson());
        if (!allowHighRisk && highRisk) {
            throw new IllegalStateException("高风险策略必须通过独立确认入口启用");
        }
        if (highRiskConfirmationReason != null) {
            if (!highRisk) {
                throw new IllegalStateException("当前策略不是高风险策略，无需独立确认");
            }
            if (Objects.equals(record.createdByEmployeeId(), command.operatedByEmployeeId())) {
                throw new IllegalStateException("高风险策略确认人不能是策略创建人");
            }
        }
        PolicyPublicationLease validated = lease(
                command.reference(),
                record.policyVersionId(),
                record.schemaVersion(),
                record.policyJson(),
                "policy-lifecycle-" + targetState.toLowerCase()
        );
        LocalDateTime operatedAt = LocalDateTime.now();
        long nextRevision = catalogRevision + 1;
        int affected = updateLifecycle(
                command, record, expectedState, targetState, validated, nextRevision, operatedAt,
                highRisk ? "HIGH" : "LOW", highRiskConfirmationReason
        );
        if (affected != 1) {
            throw new IllegalStateException("策略目录版本已变更，请刷新后重试");
        }
        return new PolicyCatalogVersion(
                command.reference(),
                targetState,
                validated.schemaVersion(),
                validated.canonicalPayload(),
                validated.digest(),
                nextRevision
        );
    }

    private boolean isHighRisk(PolicyType type, String policyJson) {
        JSONObject document = JSON.parseObject(policyJson);
        if (type == PolicyType.CANDIDATE) {
            return "ALLOW".equals(document.getString("selfApprovalPolicy"))
                    || Set.of("AUTO_APPROVE", "AUTO_REJECT").contains(document.getString("emptyCandidatePolicy"))
                    || document.containsKey("fallbackIdentityReference");
        }
        if (type == PolicyType.APPROVAL) {
            return "RETURN_ANCESTOR".equals(document.getString("returnRule"));
        }
        return containsHighRiskScope(document.getJSONObject("startScope"))
                || containsHighRiskScope(document.getJSONObject("visibilityScope"));
    }

    private boolean containsHighRiskScope(JSONObject scope) {
        if (scope == null) {
            return false;
        }
        return Set.of("ALL", "ROLE_IDS", "DEPARTMENT_IDS").contains(scope.getString("type"));
    }

    private int updateLifecycle(
            PolicyLifecycleCommand command,
            CatalogRecord record,
            String expectedState,
            String targetState,
            PolicyPublicationLease validated,
            long nextRevision,
            LocalDateTime operatedAt,
            String effectiveRisk,
            String highRiskConfirmationReason
    ) {
        return switch (command.reference().type()) {
            case CANDIDATE -> updateCandidateLifecycle(command, record, expectedState, targetState, validated, nextRevision, operatedAt, effectiveRisk, highRiskConfirmationReason);
            case APPROVAL -> updateApprovalLifecycle(command, record, expectedState, targetState, validated, nextRevision, operatedAt, effectiveRisk, highRiskConfirmationReason);
            case START_VISIBILITY -> updateStartVisibilityLifecycle(command, record, expectedState, targetState, validated, nextRevision, operatedAt, effectiveRisk, highRiskConfirmationReason);
        };
    }

    private int updateCandidateLifecycle(
            PolicyLifecycleCommand command,
            CatalogRecord record,
            String expectedState,
            String targetState,
            PolicyPublicationLease validated,
            long nextRevision,
            LocalDateTime operatedAt,
            String effectiveRisk,
            String highRiskConfirmationReason
    ) {
        UpdateWrapper<BpmCandidatePolicyVersionEntity> update = new UpdateWrapper<BpmCandidatePolicyVersionEntity>()
                .set("lifecycle_state", targetState)
                .set("policy_json", validated.canonicalPayload())
                .set("policy_digest", validated.digest())
                .set("catalog_revision", nextRevision)
                .set("update_time", operatedAt)
                .eq("candidate_policy_version_id", record.policyVersionId())
                .eq("lifecycle_state", expectedState)
                .eq("catalog_revision", normalizeRevision(record.catalogRevision()));
        if ("ACTIVE".equals(targetState)) {
            update.set("activated_by_employee_id", command.operatedByEmployeeId())
                    .set("activated_at", operatedAt)
                    .set("effective_risk", effectiveRisk);
            applyHighRiskConfirmation(update, command, validated, operatedAt, highRiskConfirmationReason);
        } else {
            update.set("retired_by_employee_id", command.operatedByEmployeeId())
                    .set("retired_at", operatedAt);
        }
        return candidatePolicyVersionDao.update(null, update);
    }

    private int updateApprovalLifecycle(
            PolicyLifecycleCommand command,
            CatalogRecord record,
            String expectedState,
            String targetState,
            PolicyPublicationLease validated,
            long nextRevision,
            LocalDateTime operatedAt,
            String effectiveRisk,
            String highRiskConfirmationReason
    ) {
        UpdateWrapper<BpmApprovalPolicyVersionEntity> update = new UpdateWrapper<BpmApprovalPolicyVersionEntity>()
                .set("lifecycle_state", targetState)
                .set("policy_json", validated.canonicalPayload())
                .set("policy_digest", validated.digest())
                .set("catalog_revision", nextRevision)
                .set("update_time", operatedAt)
                .eq("approval_policy_version_id", record.policyVersionId())
                .eq("lifecycle_state", expectedState)
                .eq("catalog_revision", normalizeRevision(record.catalogRevision()));
        if ("ACTIVE".equals(targetState)) {
            update.set("activated_by_employee_id", command.operatedByEmployeeId())
                    .set("activated_at", operatedAt)
                    .set("effective_risk", effectiveRisk);
            applyHighRiskConfirmation(update, command, validated, operatedAt, highRiskConfirmationReason);
        } else {
            update.set("retired_by_employee_id", command.operatedByEmployeeId())
                    .set("retired_at", operatedAt);
        }
        return approvalPolicyVersionDao.update(null, update);
    }

    private int updateStartVisibilityLifecycle(
            PolicyLifecycleCommand command,
            CatalogRecord record,
            String expectedState,
            String targetState,
            PolicyPublicationLease validated,
            long nextRevision,
            LocalDateTime operatedAt,
            String effectiveRisk,
            String highRiskConfirmationReason
    ) {
        UpdateWrapper<BpmStartVisibilityPolicyVersionEntity> update = new UpdateWrapper<BpmStartVisibilityPolicyVersionEntity>()
                .set("lifecycle_state", targetState)
                .set("policy_json", validated.canonicalPayload())
                .set("policy_digest", validated.digest())
                .set("catalog_revision", nextRevision)
                .set("update_time", operatedAt)
                .eq("start_visibility_policy_version_id", record.policyVersionId())
                .eq("lifecycle_state", expectedState)
                .eq("catalog_revision", normalizeRevision(record.catalogRevision()));
        if ("ACTIVE".equals(targetState)) {
            update.set("activated_by_employee_id", command.operatedByEmployeeId())
                    .set("activated_at", operatedAt)
                    .set("effective_risk", effectiveRisk);
            applyHighRiskConfirmation(update, command, validated, operatedAt, highRiskConfirmationReason);
        } else {
            update.set("retired_by_employee_id", command.operatedByEmployeeId())
                    .set("retired_at", operatedAt);
        }
        return startVisibilityPolicyVersionDao.update(null, update);
    }

    private void applyHighRiskConfirmation(
            UpdateWrapper<?> update,
            PolicyLifecycleCommand command,
            PolicyPublicationLease validated,
            LocalDateTime operatedAt,
            String confirmationReason
    ) {
        if (confirmationReason != null) {
            update.set("high_risk_confirmed_by_employee_id", command.operatedByEmployeeId())
                    .set("high_risk_confirmation_reason", confirmationReason)
                    .set("high_risk_confirmed_at", operatedAt)
                    .set("high_risk_confirmed_digest", validated.digest());
        }
    }

    private int nextPolicyVersion(PolicyType type, String policyKey) {
        Integer maxVersion = switch (type) {
            case CANDIDATE -> candidatePolicyVersionDao.selectMaxPolicyVersionByPolicyKey(policyKey);
            case APPROVAL -> approvalPolicyVersionDao.selectMaxPolicyVersionByPolicyKey(policyKey);
            case START_VISIBILITY -> startVisibilityPolicyVersionDao.selectMaxPolicyVersionByPolicyKey(policyKey);
        };
        if (maxVersion == null) {
            return 1;
        }
        if (maxVersion == Integer.MAX_VALUE) {
            throw new IllegalStateException("策略版本号已达上限：" + policyKey);
        }
        return maxVersion + 1;
    }

    private void createCandidateDraft(PolicyDraftCommand command, int version, String payload, String digest) {
        BpmCandidatePolicyVersionEntity entity = new BpmCandidatePolicyVersionEntity();
        entity.setPolicyKey(command.policyKey());
        entity.setPolicyVersion(version);
        entity.setLifecycleState("DRAFT");
        entity.setSchemaVersion(command.schemaVersion());
        entity.setPolicyJson(payload);
        entity.setPolicyDigest(digest);
        entity.setCatalogRevision(0L);
        entity.setCreatedByEmployeeId(command.createdByEmployeeId());
        candidatePolicyVersionDao.insert(entity);
    }

    private void createApprovalDraft(PolicyDraftCommand command, int version, String payload, String digest) {
        BpmApprovalPolicyVersionEntity entity = new BpmApprovalPolicyVersionEntity();
        entity.setPolicyKey(command.policyKey());
        entity.setPolicyVersion(version);
        entity.setLifecycleState("DRAFT");
        entity.setSchemaVersion(command.schemaVersion());
        entity.setPolicyJson(payload);
        entity.setPolicyDigest(digest);
        entity.setCatalogRevision(0L);
        entity.setCreatedByEmployeeId(command.createdByEmployeeId());
        approvalPolicyVersionDao.insert(entity);
    }

    private void createStartVisibilityDraft(PolicyDraftCommand command, int version, String payload, String digest) {
        BpmStartVisibilityPolicyVersionEntity entity = new BpmStartVisibilityPolicyVersionEntity();
        entity.setPolicyKey(command.policyKey());
        entity.setPolicyVersion(version);
        entity.setLifecycleState("DRAFT");
        entity.setSchemaVersion(command.schemaVersion());
        entity.setPolicyJson(payload);
        entity.setPolicyDigest(digest);
        entity.setCatalogRevision(0L);
        entity.setCreatedByEmployeeId(command.createdByEmployeeId());
        startVisibilityPolicyVersionDao.insert(entity);
    }

    private CatalogRecord loadForUpdate(PolicyReference reference) {
        return switch (reference.type()) {
            case CANDIDATE -> loadCandidateForUpdate(reference);
            case APPROVAL -> loadApprovalForUpdate(reference);
            case START_VISIBILITY -> loadStartVisibilityForUpdate(reference);
        };
    }

    private CatalogRecord load(PolicyReference reference) {
        return switch (reference.type()) {
            case CANDIDATE -> {
                BpmCandidatePolicyVersionEntity entity = candidatePolicyVersionDao.selectOne(
                        new LambdaQueryWrapper<BpmCandidatePolicyVersionEntity>()
                                .eq(BpmCandidatePolicyVersionEntity::getPolicyKey, reference.policyKey())
                                .eq(BpmCandidatePolicyVersionEntity::getPolicyVersion, reference.policyVersion())
                );
                yield entity == null ? null : candidateRecord(entity);
            }
            case APPROVAL -> {
                BpmApprovalPolicyVersionEntity entity = approvalPolicyVersionDao.selectOne(
                        new LambdaQueryWrapper<BpmApprovalPolicyVersionEntity>()
                                .eq(BpmApprovalPolicyVersionEntity::getPolicyKey, reference.policyKey())
                                .eq(BpmApprovalPolicyVersionEntity::getPolicyVersion, reference.policyVersion())
                );
                yield entity == null ? null : approvalRecord(entity);
            }
            case START_VISIBILITY -> {
                BpmStartVisibilityPolicyVersionEntity entity = startVisibilityPolicyVersionDao.selectOne(
                        new LambdaQueryWrapper<BpmStartVisibilityPolicyVersionEntity>()
                                .eq(BpmStartVisibilityPolicyVersionEntity::getPolicyKey, reference.policyKey())
                                .eq(BpmStartVisibilityPolicyVersionEntity::getPolicyVersion, reference.policyVersion())
                );
                yield entity == null ? null : startVisibilityRecord(entity);
            }
        };
    }

    private CatalogRecord loadCandidateForUpdate(PolicyReference reference) {
        BpmCandidatePolicyVersionEntity entity = candidatePolicyVersionDao.selectByPolicyKeyAndVersionForUpdate(
                reference.policyKey(), reference.policyVersion()
        );
        return entity == null ? null : candidateRecord(entity);
    }

    private CatalogRecord loadApprovalForUpdate(PolicyReference reference) {
        BpmApprovalPolicyVersionEntity entity = approvalPolicyVersionDao.selectByPolicyKeyAndVersionForUpdate(
                reference.policyKey(), reference.policyVersion()
        );
        return entity == null ? null : approvalRecord(entity);
    }

    private CatalogRecord loadStartVisibilityForUpdate(PolicyReference reference) {
        BpmStartVisibilityPolicyVersionEntity entity = startVisibilityPolicyVersionDao.selectByPolicyKeyAndVersionForUpdate(
                reference.policyKey(), reference.policyVersion()
        );
        return entity == null ? null : startVisibilityRecord(entity);
    }

    private CatalogRecord candidateRecord(BpmCandidatePolicyVersionEntity entity) {
        return new CatalogRecord(
                entity.getCandidatePolicyVersionId(), entity.getLifecycleState(), entity.getSchemaVersion(),
                entity.getPolicyJson(), entity.getCatalogRevision(), entity.getCreatedByEmployeeId()
        );
    }

    private CatalogRecord approvalRecord(BpmApprovalPolicyVersionEntity entity) {
        return new CatalogRecord(
                entity.getApprovalPolicyVersionId(), entity.getLifecycleState(), entity.getSchemaVersion(),
                entity.getPolicyJson(), entity.getCatalogRevision(), entity.getCreatedByEmployeeId()
        );
    }

    private CatalogRecord startVisibilityRecord(BpmStartVisibilityPolicyVersionEntity entity) {
        return new CatalogRecord(
                entity.getStartVisibilityPolicyVersionId(), entity.getLifecycleState(), entity.getSchemaVersion(),
                entity.getPolicyJson(), entity.getCatalogRevision(), entity.getCreatedByEmployeeId()
        );
    }

    private String normalizeLifecycleState(String lifecycleState) {
        String normalized = StringUtils.trimToNull(lifecycleState);
        if (normalized == null) {
            return null;
        }
        if (!Set.of("DRAFT", "ACTIVE", "RETIRED").contains(normalized)) {
            throw new IllegalArgumentException("策略生命周期状态不合法：" + lifecycleState);
        }
        return normalized;
    }

    private Long normalizeRevision(Long revision) {
        return revision == null ? 0L : revision;
    }

    private record CatalogRecord(
            Long policyVersionId,
            String lifecycleState,
            Integer schemaVersion,
            String policyJson,
            Long catalogRevision,
            Long createdByEmployeeId
    ) {
    }
}
