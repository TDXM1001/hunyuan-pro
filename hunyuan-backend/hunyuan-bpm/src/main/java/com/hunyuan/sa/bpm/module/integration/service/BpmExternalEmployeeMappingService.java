package com.hunyuan.sa.bpm.module.integration.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.bpm.module.integration.dao.BpmExternalEmployeeMappingDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmExternalEmployeeMappingEntity;
import com.hunyuan.sa.bpm.module.integration.domain.model.BpmSourceApplicationPrincipal;
import org.springframework.stereotype.Service;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class BpmExternalEmployeeMappingService {
    private final BpmExternalEmployeeMappingDao dao; private final Clock clock;
    @Autowired public BpmExternalEmployeeMappingService(BpmExternalEmployeeMappingDao dao) { this(dao, Clock.systemUTC()); }
    public BpmExternalEmployeeMappingService(BpmExternalEmployeeMappingDao dao, Clock clock) { this.dao=dao; this.clock=clock; }
    public Long requireEmployee(BpmSourceApplicationPrincipal principal, String externalEmployeeId) {
        BpmExternalEmployeeMappingEntity mapping=dao.selectOne(Wrappers.<BpmExternalEmployeeMappingEntity>lambdaQuery()
                .eq(BpmExternalEmployeeMappingEntity::getSourceSystemCode, principal.sourceSystemCode())
                .eq(BpmExternalEmployeeMappingEntity::getExternalEmployeeId, externalEmployeeId)
                .eq(BpmExternalEmployeeMappingEntity::getStatus, "ACTIVE").last("LIMIT 1"));
        java.time.LocalDateTime now=java.time.LocalDateTime.now(clock);
        if(mapping==null || (mapping.getValidFrom()!=null && mapping.getValidFrom().isAfter(now))
                || (mapping.getValidUntil()!=null && !mapping.getValidUntil().isAfter(now))) throw new SecurityException("外部员工映射不存在或已失效");
        return mapping.getHunyuanEmployeeId();
    }
}
