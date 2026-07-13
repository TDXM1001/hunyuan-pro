package com.hunyuan.sa.bpm.api.identity;

import java.util.function.Supplier;

public final class BpmActorScope {
    private static final ThreadLocal<Long> ACTOR = new ThreadLocal<>();
    private BpmActorScope() {}
    public static Long currentEmployeeId() { return ACTOR.get(); }
    public static <T> T runAs(Long employeeId, Supplier<T> action) {
        if (employeeId == null) throw new IllegalArgumentException("employeeId不能为空");
        Long previous = ACTOR.get();
        ACTOR.set(employeeId);
        try { return action.get(); } finally { if (previous == null) ACTOR.remove(); else ACTOR.set(previous); }
    }
}
