package com.hunyuan.sa.bpm.engine.compiler;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 按 authored 顺序连接片段，显式处理多出口和多入口。
 */
public class FragmentComposer {

    public BpmnFragment compose(List<BpmnFragment> fragments, StableBpmnIdFactory idFactory) {
        if (fragments == null || fragments.isEmpty()) {
            return new BpmnFragment(List.of(), List.of(), List.of(), List.of(), List.of(), Set.of());
        }
        List<String> elements = new ArrayList<>();
        List<BpmnSequenceFlow> flows = new ArrayList<>();
        List<CompiledNodeSnapshot> snapshots = new ArrayList<>();
        Set<String> requirements = new LinkedHashSet<>();

        BpmnFragment previous = null;
        for (BpmnFragment fragment : fragments) {
            elements.addAll(fragment.generatedElements());
            flows.addAll(fragment.sequenceFlows());
            snapshots.addAll(fragment.compiledNodeSnapshots());
            requirements.addAll(fragment.runtimeRequirements());
            if (previous != null) {
                for (String sourceRef : previous.exitElementIds()) {
                    for (String targetRef : fragment.entryElementIds()) {
                        flows.add(new BpmnSequenceFlow(
                                idFactory.nextFlowId(sourceRef, targetRef),
                                sourceRef,
                                targetRef,
                                null
                        ));
                    }
                }
            }
            previous = fragment;
        }

        return new BpmnFragment(
                fragments.get(0).entryElementIds(),
                fragments.get(fragments.size() - 1).exitElementIds(),
                elements,
                flows,
                snapshots,
                requirements
        );
    }
}
