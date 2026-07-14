package com.hunyuan.sa.bpm.module.evolution.service;

import com.hunyuan.sa.bpm.module.evolution.domain.model.MigrationSafetyAssessment;
import com.hunyuan.sa.bpm.module.evolution.domain.model.MigrationSafetyFacts;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MigrationSafetyEvaluator {

    public MigrationSafetyAssessment evaluate(MigrationSafetyFacts facts) {
        List<MigrationSafetyAssessment.Blocker> blockers = new ArrayList<>();
        add(blockers, !facts.running(), "INSTANCE_NOT_RUNNING", "实例不处于可迁移的运行状态");
        add(blockers, facts.activeHumanTaskCount() > 0, "ACTIVE_HUMAN_TASK", "实例存在活动人工任务");
        add(blockers, facts.additionalActiveExecutionCount() > 0, "ACTIVE_PARALLEL_PATH", "实例存在并行或包容活动路径");
        add(blockers, facts.pendingTimerCount() > 0, "PENDING_TIMER", "实例存在待执行时间事件");
        add(blockers, facts.activeExternalWaitCount() > 0, "EXTERNAL_WAIT", "实例存在外部等待");
        add(blockers, facts.activeSubProcessCount() > 0, "ACTIVE_SUB_PROCESS", "实例存在活动子流程");
        add(blockers, facts.irreversibleSideEffectCount() > 0, "IRREVERSIBLE_SIDE_EFFECT", "实例已产生不可逆外部副作用");
        add(blockers, !facts.nodeMappingComplete(), "NODE_MAPPING_INCOMPLETE", " authored 节点映射不完整");
        add(blockers, !facts.dataMappingValid(), "DATA_MAPPING_INVALID", "数据或变量映射未通过校验");
        return new MigrationSafetyAssessment(blockers.isEmpty(), List.copyOf(blockers));
    }

    private void add(List<MigrationSafetyAssessment.Blocker> blockers, boolean blocked, String code, String message) {
        if (blocked) {
            blockers.add(new MigrationSafetyAssessment.Blocker(code, message));
        }
    }
}
