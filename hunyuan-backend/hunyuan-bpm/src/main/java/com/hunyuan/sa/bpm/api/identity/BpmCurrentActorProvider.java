package com.hunyuan.sa.bpm.api.identity;

/**
 * 提供当前请求上下文中的审批人/发起人员工 id。
 */
public interface BpmCurrentActorProvider {

    /**
     * 读取当前员工 id，不允许返回空。
     */
    Long requireCurrentEmployeeId();
}
