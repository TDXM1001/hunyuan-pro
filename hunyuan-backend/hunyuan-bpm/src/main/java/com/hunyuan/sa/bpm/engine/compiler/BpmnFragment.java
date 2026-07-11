package com.hunyuan.sa.bpm.engine.compiler;

import java.util.List;
import java.util.Set;

/**
 * 单入口/多出口或多入口/单出口的可组合 BPMN 片段。
 */
public record BpmnFragment(
        List<String> entryElementIds,
        List<String> exitElementIds,
        List<String> generatedElements,
        List<BpmnSequenceFlow> sequenceFlows,
        List<CompiledNodeSnapshot> compiledNodeSnapshots,
        Set<String> runtimeRequirements
) {
    public BpmnFragment {
        entryElementIds = List.copyOf(entryElementIds);
        exitElementIds = List.copyOf(exitElementIds);
        generatedElements = List.copyOf(generatedElements);
        sequenceFlows = List.copyOf(sequenceFlows);
        compiledNodeSnapshots = List.copyOf(compiledNodeSnapshots);
        runtimeRequirements = Set.copyOf(runtimeRequirements);
    }
}
