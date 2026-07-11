package com.hunyuan.sa.bpm.engine.compiler;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class FragmentComposerTest {

    private final FragmentComposer composer = new FragmentComposer();

    @Test
    void composeShouldConnectEveryExitToEveryNextEntry() {
        BpmnFragment left = new BpmnFragment(
                List.of("a"), List.of("a1", "a2"), List.of(), List.of(), List.of(), Set.of()
        );
        BpmnFragment right = new BpmnFragment(
                List.of("b1", "b2"), List.of("b"), List.of(), List.of(), List.of(), Set.of()
        );

        BpmnFragment composed = composer.compose(List.of(left, right), new StableBpmnIdFactory());

        assertThat(composed.sequenceFlows())
                .extracting(BpmnSequenceFlow::sourceRef, BpmnSequenceFlow::targetRef)
                .containsExactly(
                        tuple("a1", "b1"),
                        tuple("a1", "b2"),
                        tuple("a2", "b1"),
                        tuple("a2", "b2")
                );
        assertThat(composed.entryElementIds()).containsExactly("a");
        assertThat(composed.exitElementIds()).containsExactly("b");
    }
}
