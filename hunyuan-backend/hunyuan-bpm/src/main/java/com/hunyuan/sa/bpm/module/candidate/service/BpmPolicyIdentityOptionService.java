package com.hunyuan.sa.bpm.module.candidate.service;

import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.candidate.domain.vo.BpmIdentityOptionPageVO;
import com.hunyuan.sa.bpm.module.candidate.domain.vo.BpmIdentityOptionVO;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class BpmPolicyIdentityOptionService {

    private static final Set<String> KINDS = Set.of(
            "EMPLOYEE", "ROLE", "DEPARTMENT", "POST", "USER_GROUP"
    );
    private final BpmOrgIdentityGateway identityGateway;

    public BpmPolicyIdentityOptionService(BpmOrgIdentityGateway identityGateway) {
        this.identityGateway = identityGateway;
    }

    public BpmIdentityOptionPageVO query(
            String kind, String keyword, Long departmentId, int pageNum, int pageSize
    ) { return query(kind, keyword, departmentId, null, pageNum, pageSize); }

    public BpmIdentityOptionPageVO query(
            String kind, String keyword, Long departmentId, Long stableId, int pageNum, int pageSize
    ) {
        String normalizedKind = kind == null ? "" : kind.trim().toUpperCase();
        if (!KINDS.contains(normalizedKind)) {
            throw new IllegalArgumentException("身份类型不合法：" + kind);
        }
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("分页参数不合法");
        }
        var exact = stableId == null ? null : identityGateway.findIdentityOption(normalizedKind, stableId);
        var options = exact == null
                ? identityGateway.queryIdentityOptions(normalizedKind, keyword, departmentId)
                : java.util.List.of(exact);
        int from = Math.min((pageNum - 1) * pageSize, options.size());
        int to = Math.min(from + pageSize, options.size());
        var items = options.subList(from, to).stream().map(option -> new BpmIdentityOptionVO(
                option.kind(), option.stableId(), option.displayName(), option.departmentId(),
                option.departmentName(), option.disabled()
        )).toList();
        return new BpmIdentityOptionPageVO(items, options.size(), pageNum, pageSize);
    }
}
