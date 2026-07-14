package com.hunyuan.sa.bpm.api.operations;

/**
 * BPM 运营治理组织数据范围，由宿主身份模块提供实现。
 */
public interface BpmOperationsAccessScope {

    /**
     * 返回最终允许查询的组织；拥有全域权限时可保留请求值。
     */
    Long requireOrganizationScope(Long requestedOrganizationId);

    /**
     * 校验指定工单组织是否在当前操作者范围内。
     */
    void checkOrganizationAccess(Long organizationId);
}
