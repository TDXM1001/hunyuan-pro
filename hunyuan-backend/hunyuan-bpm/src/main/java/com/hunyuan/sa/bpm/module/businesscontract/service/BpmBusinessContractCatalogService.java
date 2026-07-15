package com.hunyuan.sa.bpm.module.businesscontract.service;

import com.hunyuan.sa.bpm.module.businesscontract.dao.BpmBusinessContractVersionDao;
import com.hunyuan.sa.bpm.module.businesscontract.domain.entity.BpmBusinessContractVersionEntity;
import com.hunyuan.sa.bpm.module.businesscontract.domain.model.BusinessContractCatalogVersion;
import com.hunyuan.sa.bpm.module.businesscontract.domain.model.BusinessContractDraftCommand;
import com.hunyuan.sa.bpm.module.businesscontract.domain.model.BusinessContractLifecycleCommand;
import com.hunyuan.sa.bpm.module.businesscontract.domain.model.BusinessContractValidationResult;
import com.hunyuan.sa.bpm.module.businesscontract.domain.visual.*;
import com.hunyuan.sa.bpm.module.businesscontract.domain.vo.*;
import com.hunyuan.sa.bpm.module.definition.service.BpmDefinitionReferenceQueryService;
import com.hunyuan.sa.bpm.module.candidate.service.PolicyCanonicalizer;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final BusinessObjectV2DocumentMapper v2Mapper = new BusinessObjectV2DocumentMapper();
    private final BpmDefinitionReferenceQueryService referenceQueryService;

    public BpmBusinessContractCatalogService(BpmBusinessContractVersionDao contractDao) {
        this(contractDao, null);
    }

    @Autowired
    public BpmBusinessContractCatalogService(
            BpmBusinessContractVersionDao contractDao,
            BpmDefinitionReferenceQueryService referenceQueryService
    ) {
        this.contractDao = contractDao;
        this.referenceQueryService = referenceQueryService;
    }

    public BusinessContractValidationResult validate(Integer schemaVersion, String contractJson) {
        validator.validate(schemaVersion, contractJson);
        String canonical = canonicalizer.canonicalize(contractJson);
        return new BusinessContractValidationResult(schemaVersion, canonical, canonicalizer.sha256(canonical));
    }

    public BusinessObjectValidationResult validateVisualDraft(BpmBusinessObjectDraft draft) { return v2Mapper.compile(draft); }

    @Transactional(rollbackFor = Exception.class)
    public BpmBusinessObjectDetailVO createVisualDraft(BpmBusinessObjectDraft draft, long actorEmployeeId) {
        BusinessObjectValidationResult compiled=requireValid(draft);
        BusinessContractCatalogVersion created=createDraft(new BusinessContractDraftCommand(draft.contractKey(),2,compiled.canonicalPayload(),actorEmployeeId));
        return saveVisualDraft(created.contractVersion(),draft,actorEmployeeId);
    }

    @Transactional(rollbackFor = Exception.class)
    public BpmBusinessObjectDetailVO saveVisualDraft(int contractVersion,BpmBusinessObjectDraft draft,long actorEmployeeId) {
        if(actorEmployeeId<=0)throw new IllegalArgumentException("当前操作员工不能为空"); BusinessObjectValidationResult compiled=requireValid(draft);long expected=draft.catalogRevision()==null?0:draft.catalogRevision();
        if(contractDao.saveVisualDraft(draft.contractKey(),contractVersion,expected,draft.objectName(),draft.description(),compiled.businessSummary(),compiled.canonicalPayload(),compiled.digest())!=1)throw new IllegalStateException("CATALOG_REVISION_CONFLICT：草稿已变化或当前版本不可编辑");
        return new BpmBusinessObjectDetailVO(draft.contractKey(),contractVersion,"DRAFT",2,expected+1,draft.objectName(),draft.description(),compiled.businessSummary(),0L,draft,compiled.findings());
    }

    @Transactional(rollbackFor = Exception.class)
    public BpmBusinessObjectDetailVO upgradeV1AsV2Draft(String key,int version,long actorEmployeeId) {
        BpmBusinessContractVersionEntity source=requireEntity(key,version); int schema=source.getSchemaVersion()==null?1:source.getSchemaVersion(); if(schema!=1)throw new IllegalStateException("只有 Schema v1 可以升级");
        BpmBusinessObjectDraft draft=v2Mapper.upgradeV1(key,StringUtils.defaultIfBlank(source.getObjectName(),key),StringUtils.defaultIfBlank(source.getDescription(),"从 v1 升级生成"),0L,source.getContractJson());
        return createVisualDraft(draft,actorEmployeeId);
    }

    public List<BpmBusinessObjectSummaryVO> listBusiness(String key,String state){QueryWrapper<BpmBusinessContractVersionEntity> query=new QueryWrapper<>();if(StringUtils.isNotBlank(key))query.eq("contract_key",key.trim());if(StringUtils.isNotBlank(state))query.eq("lifecycle_state",normalizeState(state));query.orderByAsc("contract_key").orderByDesc("contract_version");return contractDao.selectList(query).stream().map(this::summary).toList();}
    public BpmBusinessObjectDetailVO getBusinessDetail(String key,int version){BpmBusinessContractVersionEntity entity=requireEntity(key,version);int schema=entity.getSchemaVersion()==null?1:entity.getSchemaVersion();long revision=entity.getCatalogRevision()==null?0:entity.getCatalogRevision();String name=StringUtils.defaultIfBlank(entity.getObjectName(),key);return new BpmBusinessObjectDetailVO(key,version,entity.getLifecycleState(),schema,revision,name,entity.getDescription(),summaryText(entity,schema),referenceCount(key,version),v2Mapper.restore(key,name,entity.getDescription(),revision,entity.getContractJson()),List.of());}
    public BpmBusinessObjectTechnicalDetailVO technicalDetail(String key,int version){BpmBusinessContractVersionEntity entity=requireEntity(key,version);String canonical=canonicalizer.canonicalize(entity.getContractJson());return new BpmBusinessObjectTechnicalDetailVO(key,version,entity.getSchemaVersion(),canonical,entity.getContractDigest()==null?canonicalizer.sha256(canonical):entity.getContractDigest());}
    public BpmBusinessObjectTechnicalDiffVO technicalDiff(String key,int left,int right){var a=com.alibaba.fastjson.JSON.parseObject(technicalDetail(key,left).canonicalPayload());var b=com.alibaba.fastjson.JSON.parseObject(technicalDetail(key,right).canonicalPayload());java.util.Set<String> leftKeys=fieldKeys(a),rightKeys=fieldKeys(b);java.util.Set<String> changed=new java.util.TreeSet<>(leftKeys);changed.addAll(rightKeys);changed.removeIf(k->leftKeys.contains(k)&&rightKeys.contains(k));return new BpmBusinessObjectTechnicalDiffVO(key,left,right,List.copyOf(changed));}
    public String exportCanonical(String key,int version){return technicalDetail(key,version).canonicalPayload();}
    public List<BpmBusinessObjectReferenceVO> references(String key,int version){if(referenceQueryService==null)return List.of();return referenceQueryService.findBusinessContractReferences(key,version).stream().map(ref->new BpmBusinessObjectReferenceVO(ref.graphDefinitionVersionId(),ref.draftId(),ref.referenceSource(),ref.processKey(),ref.processName(),ref.definitionVersion(),ref.lifecycleState())).toList();}
    @Transactional(rollbackFor = Exception.class) public void deleteDraft(String key,int version,long revision){if(referenceCount(key,version)>0)throw new IllegalStateException("草稿仍被 Graph 引用");if(contractDao.deleteDraft(key,version,revision)!=1)throw new IllegalStateException("只有未引用草稿可以删除，或目录版本已变化");}

    private BusinessObjectValidationResult requireValid(BpmBusinessObjectDraft draft){BusinessObjectValidationResult result=v2Mapper.compile(draft);if(!result.valid())throw new IllegalArgumentException(result.findings().get(0).message());return result;}
    private BpmBusinessContractVersionEntity requireEntity(String key,int version){BpmBusinessContractVersionEntity entity=contractDao.selectOne(new QueryWrapper<BpmBusinessContractVersionEntity>().eq("contract_key",key).eq("contract_version",version));if(entity==null)throw new IllegalArgumentException("业务对象版本不存在");return entity;}
    private long referenceCount(String key,int version){return referenceQueryService==null?0:referenceQueryService.findBusinessContractReferences(key,version).size();}
    private BpmBusinessObjectSummaryVO summary(BpmBusinessContractVersionEntity e){int schema=e.getSchemaVersion()==null?1:e.getSchemaVersion();return new BpmBusinessObjectSummaryVO(e.getContractKey(),e.getContractVersion(),StringUtils.defaultIfBlank(e.getObjectName(),e.getContractKey()),e.getDescription(),e.getLifecycleState(),schema,e.getCatalogRevision()==null?0:e.getCatalogRevision(),summaryText(e,schema),referenceCount(e.getContractKey(),e.getContractVersion()));}
    private String summaryText(BpmBusinessContractVersionEntity e,int schema){return StringUtils.defaultIfBlank(e.getBusinessSummary(),schema==1?"旧版业务契约，可升级为可视化草稿。":"业务对象摘要待重新校验。");}
    private java.util.Set<String> fieldKeys(com.alibaba.fastjson.JSONObject d){java.util.Set<String> result=new java.util.HashSet<>();for(String zone:List.of("fieldSchema","routingFacts","workingDataSchema")){var a=d.getJSONArray(zone);if(a!=null)a.forEach(v->result.add(((com.alibaba.fastjson.JSONObject)v).getString("key")));}return result;}

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
                result.add(toVersion(entity, validated));
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
        return toVersion(entity, validated);
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

    private BusinessContractCatalogVersion toVersion(
            BpmBusinessContractVersionEntity entity,
            BusinessContractValidationResult validated
    ) {
        return new BusinessContractCatalogVersion(
                entity.getBusinessContractVersionId(), entity.getContractKey(), entity.getContractVersion(),
                entity.getLifecycleState(), entity.getSchemaVersion(), validated.canonicalContractJson(),
                validated.contractDigest(), entity.getCatalogRevision()
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
