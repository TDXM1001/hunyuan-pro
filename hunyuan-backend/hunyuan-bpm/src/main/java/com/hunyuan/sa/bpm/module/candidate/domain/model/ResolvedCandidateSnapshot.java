package com.hunyuan.sa.bpm.module.candidate.domain.model;

import java.util.List;

/**
 * 单次候选解析的稳定输出，供 M4 持久化为阶段成员事实。
 */
public record ResolvedCandidateSnapshot(
        List<ResolvedCandidateMember> members,
        List<CandidateDiagnostic> diagnostics,
        CandidateAutomaticOutcome automaticOutcome
) {

    public ResolvedCandidateSnapshot(List<ResolvedCandidateMember> members, List<CandidateDiagnostic> diagnostics) {
        this(members, diagnostics, null);
    }

    public ResolvedCandidateSnapshot {
        members = List.copyOf(members);
        diagnostics = List.copyOf(diagnostics);
    }
}
