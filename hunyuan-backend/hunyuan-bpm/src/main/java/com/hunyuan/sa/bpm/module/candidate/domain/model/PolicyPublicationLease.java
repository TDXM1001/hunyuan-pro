package com.hunyuan.sa.bpm.module.candidate.domain.model;

/**
 * 发布事务取得的不可变策略内容；运行期只能读取定义版本中保存的该值。
 */
public record PolicyPublicationLease(
        PolicyReference reference,
        Long policyVersionId,
        int schemaVersion,
        String canonicalPayload,
        String digest,
        String publicationRequestId
) {
}
