package com.hunyuan.sa.admin.module.platform.audit.application;

import com.hunyuan.sa.admin.module.platform.audit.operatelog.core.OperateLogAspect;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 业务代码继续使用历史注解路径，因此迁移切面实现时必须锁定该 pointcut 契约。
 */
class OperateLogAspectPointcutTest {

    @Test
    void keepsStableOperateLogAnnotationPointcut() throws Exception {
        Pointcut pointcut = OperateLogAspect.class.getMethod("logPointCut")
                .getAnnotation(Pointcut.class);

        assertThat(pointcut.value()).contains(
                "com.hunyuan.sa.base.module.support.operatelog.annotation.OperateLog");
    }
}
