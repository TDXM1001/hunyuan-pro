package com.hunyuan.sa.bpm.module.businesscontract.service;

import com.hunyuan.sa.bpm.module.businesscontract.dao.BpmBusinessContractVersionDao;
import com.hunyuan.sa.bpm.module.businesscontract.domain.entity.BpmBusinessContractVersionEntity;
import com.hunyuan.sa.bpm.module.businesscontract.domain.model.BusinessContractCatalogVersion;
import com.hunyuan.sa.bpm.module.businesscontract.domain.model.BusinessContractDraftCommand;
import com.hunyuan.sa.bpm.module.businesscontract.domain.model.BusinessContractLifecycleCommand;
import com.hunyuan.sa.bpm.module.businesscontract.domain.model.BusinessContractValidationResult;
import com.hunyuan.sa.bpm.module.candidate.service.PolicyCanonicalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BpmBusinessContractCatalogService {

    private final BpmBusinessContractVersionDao contractDao;
    private final PolicyCanonicalizer canonicalizer = new PolicyCanonicalizer();
    private final BusinessContractDocumentValidator validator = new BusinessContractDocumentValidator();

    public BpmBusinessContractCatalogService(BpmBusinessContractVersionDao contractDao) {
        this.contractDao = contractDao;
    }

    public BusinessContractValidationResult validate(Integer schemaVersion, String contractJson) {
        validator.validate(schemaVersion, contractJson);
        String canonical = canonicalizer.canonicalize(contractJson);
        return new BusinessContractValidationResult(schemaVersion, canonical, canonicalizer.sha256(canonical));
    }

    @Transactional(rollbackFor = Exception.class)
    public BusinessContractCatalogVersion createDraft(BusinessContractDraftCommand command) {
        BusinessContractValidationResult validated = validate(command.schemaVersion(), command.contractJson());
        Integer currentMax = contractDao.selectMaxContractVersion(command.contractKey());
        int nextVersion = currentMax == null ? 1 : currentMax + 1;
        BpmBusinessContractVersionEntity entity = new BpmBusinessContractVersionEntity();
        entity.setContractKey(command.contractKey());
        entity.setContractVersion(nextVersion);
        entity.setLifecycleState("DRAFT");
        entity.setSchemaVersion(command.schemaVersion());
        entity.setContractJson(validated.canonicalContractJson());
        entity.setContractDigest(validated.contractDigest());
        entity.setCatalogRevision(0L);
        entity.setCreatedByEmployeeId(command.createdByEmployeeId());
        contractDao.insert(entity);
        return toVersion(entity);
    }

    public List<BusinessContractCatalogVersion> list(String contractKey, String lifecycleState) {
        QueryWrapper<BpmBusinessContractVersionEntity> query = new QueryWrapper<>();
        if (StringUtils.isNotBlank(contractKey)) {
            query.eq("contract_key", contractKey.trim());
        }
        if (StringUtils.isNotBlank(lifecycleState)) {
            query.eq("lifecycle_state", normalizeState(lifecycleState));
        }
        query.orderByAsc("contract_key").orderByDesc("contract_version");
        List<BusinessContractCatalogVersion> result = new ArrayList<>();
        for (BpmBusinessContractVersionEntity entity : contractDao.selectList(query)) {
            try {
                BusinessContractValidationResult validated = validate(entity.getSchemaVersion(), entity.getContractJson());
                entity.setContractJson(validated.canonicalContractJson());
                entity.setContractDigest(validated.contractDigest());
                result.add(toVersion(entity));
            } catch (RuntimeException ignored) {
                // 历史脏版本不暴露为可绑定契约。
            }
        }
        return result;
    }

    public BusinessContractCatalogVersion get(String contractKey, Integer contractVersion) {
        BpmBusinessContractVersionEntity entity = contractDao.selectOne(new QueryWrapper<BpmBusinessContractVersionEntity>()
                .eq("contract_key", contractKey)
                .eq("contract_version", contractVersion));
        if (entity == null) {
            throw new IllegalArgumentException("业务契约版本不存在");
        }
        BusinessContractValidationResult validated = validate(entity.getSchemaVersion(), entity.getContractJson());
        entity.setContractJson(validated.canonicalContractJson());
        entity.setContractDigest(validated.contractDigest());
        return toVersion(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public BusinessContractCatalogVersion freezeForPublication(String contractKey, Integer contractVersion) {
        BpmBusinessContractVersionEntity entity = contractDao.selectByKeyAndVersionForUpdate(
                contractKey, contractVersion
        );
        if (entity == null) {
            throw new IllegalArgumentException("业务契约版本不存在：" + contractKey + "@" + contractVersion);
        }
        if (!"ACTIVE".equals(entity.getLifecycleState())) {
            throw new IllegalStateException("业务契约版本未启用：" + contractKey + "@" + contractVersion);
        }
        BusinessContractValidationResult validated = validate(entity.getSchemaVersion(), entity.getContractJson());
        entity.setContractJson(validated.canonicalContractJson());
        entity.setContractDigest(validated.contractDigest());
        return toVersion(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public BusinessContractCatalogVersion copyAsDraft(
            String contractKey,
            Integer contractVersion,
            Long actorEmployeeId
    ) {
        BusinessContractCatalogVersion source = get(contractKey, contractVersion);
        return createDraft(new BusinessContractDraftCommand(
                source.contractKey(), source.schemaVersion(), source.canonicalContractJson(), actorEmployeeId
        ));
    }

    @Transactional(rollbackFor = Exception.class)
    public BusinessContractCatalogVersion activate(BusinessContractLifecycleCommand command) {
        return transition(command, "DRAFT", "ACTIVE");
    }

    @Transactional(rollbackFor = Exception.class)
    public BusinessContractCatalogVersion retire(BusinessContractLifecycleCommand command) {
        return transition(command, "ACTIVE", "RETIRED");
    }

    private BusinessContractCatalogVersion transition(
            BusinessContractLifecycleCommand command,
            String expectedState,
            String targetState
    ) {
        BpmBusinessContractVersionEntity entity = contractDao.selectByKeyAndVersionForUpdate(
                command.contractKey(), command.contractVersion()
        );
        if (entity == null) {
            throw new IllegalArgumentException("业务契约版本不存在");
        }
        if (!expectedState.equals(entity.getLifecycleState())) {
            throw new IllegalStateException("业务契约当前状态不允许此操作");
        }
        if (!command.expectedCatalogRevision().equals(entity.getCatalogRevision())) {
            throw new IllegalStateException("业务契约版本已变更，请刷新后重试");
        }
        BusinessContractValidationResult validated = validate(entity.getSchemaVersion(), entity.getContractJson());
        LocalDateTime now = LocalDateTime.now();
        long nextRevision = entity.getCatalogRevision() + 1;
        if (contractDao.transitionState(
                entity.getBusinessContractVersionId(), expectedState, targetState,
                entity.getCatalogRevision(), nextRevision,
                validated.canonicalContractJson(), validated.contractDigest(),
                command.actorEmployeeId(), now
        ) != 1) {
            throw new IllegalStateException("业务契约版本已变更，请刷新后重试");
        }
        entity.setLifecycleState(targetState);
        entity.setContractJson(validated.canonicalContractJson());
        entity.setContractDigest(validated.contractDigest());
        entity.setCatalogRevision(nextRevision);
        return toVersion(entity);
    }

    private BusinessContractCatalogVersion toVersion(BpmBusinessContractVersionEntity entity) {
        return new BusinessContractCatalogVersion(
                entity.getBusinessContractVersionId(), entity.getContractKey(), entity.getContractVersion(),
                entity.getLifecycleState(), entity.getSchemaVersion(), entity.getContractJson(),
                entity.getContractDigest(), entity.getCatalogRevision()
        );
    }

    private String normalizeState(String state) {
        String normalized = state.trim().toUpperCase(java.util.Locale.ROOT);
        if (!("DRAFT".equals(normalized) || "ACTIVE".equals(normalized) || "RETIRED".equals(normalized))) {
            throw new IllegalArgumentException("业务契约生命周期状态不支持：" + state);
        }
        return normalized;
    }
}
