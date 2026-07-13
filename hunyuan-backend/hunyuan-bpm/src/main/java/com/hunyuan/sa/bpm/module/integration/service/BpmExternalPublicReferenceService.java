package com.hunyuan.sa.bpm.module.integration.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.bpm.module.integration.dao.BpmExternalPublicReferenceDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmExternalPublicReferenceEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BpmExternalPublicReferenceService {
    private final BpmExternalPublicReferenceDao references;

    public BpmExternalPublicReferenceService(BpmExternalPublicReferenceDao references) {
        this.references = references;
    }

    @Transactional(rollbackFor = Exception.class)
    public String getOrCreate(String sourceSystemCode, String objectType, Long internalId) {
        BpmExternalPublicReferenceEntity existing = references.selectOne(Wrappers.<BpmExternalPublicReferenceEntity>lambdaQuery()
                .eq(BpmExternalPublicReferenceEntity::getSourceSystemCode, sourceSystemCode)
                .eq(BpmExternalPublicReferenceEntity::getObjectType, objectType)
                .eq(BpmExternalPublicReferenceEntity::getInternalId, internalId).last("LIMIT 1"));
        if (existing != null) return existing.getPublicId();
        BpmExternalPublicReferenceEntity entity = new BpmExternalPublicReferenceEntity();
        entity.setPublicId(prefix(objectType) + UUID.randomUUID());
        entity.setSourceSystemCode(sourceSystemCode);
        entity.setObjectType(objectType);
        entity.setInternalId(internalId);
        references.insert(entity);
        return entity.getPublicId();
    }

    public Long resolve(String sourceSystemCode, String objectType, String publicId) {
        if (publicId == null || !publicId.startsWith(prefix(objectType))) throw new IllegalArgumentException("公共标识格式无效");
        BpmExternalPublicReferenceEntity entity = references.selectOne(Wrappers.<BpmExternalPublicReferenceEntity>lambdaQuery()
                .eq(BpmExternalPublicReferenceEntity::getSourceSystemCode, sourceSystemCode)
                .eq(BpmExternalPublicReferenceEntity::getObjectType, objectType)
                .eq(BpmExternalPublicReferenceEntity::getPublicId, publicId).last("LIMIT 1"));
        if (entity == null) throw new SecurityException("公共标识不存在或无权访问");
        return entity.getInternalId();
    }

    private String prefix(String objectType) {
        return "INSTANCE".equals(objectType) ? "BP-" : "BT-";
    }
}
