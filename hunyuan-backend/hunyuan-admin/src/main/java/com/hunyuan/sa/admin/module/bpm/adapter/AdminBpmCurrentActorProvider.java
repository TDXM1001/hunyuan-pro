package com.hunyuan.sa.admin.module.bpm.adapter;

import com.hunyuan.sa.base.common.util.SmartRequestUtil;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import org.springframework.stereotype.Component;

/**
 * 从当前请求线程中解析 BPM 当前员工。
 */
@Component
public class AdminBpmCurrentActorProvider implements BpmCurrentActorProvider {

    @Override
    public Long requireCurrentEmployeeId() {
        Long employeeId = SmartRequestUtil.getRequestUserId();
        if (employeeId == null) {
            throw new IllegalStateException("当前请求未解析到员工身份");
        }
        return employeeId;
    }
}
