package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.engine.graph.ApprovalStageControl;
import com.hunyuan.sa.bpm.module.candidate.domain.model.EngineEffect;
import com.hunyuan.sa.bpm.module.runtime.event.BpmApprovalStageEngineEffectRequestedEvent;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalStageEngineEffectAfterCommitListener;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class BpmApprovalStageEngineEffectAfterCommitListenerTest {

    @Test
    void listenerShouldRunOnlyAfterTheActionTransactionCommits() throws NoSuchMethodException {
        Class<?> listenerType = loadClass(
                "com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalStageEngineEffectAfterCommitListener"
        );
        Class<?> eventType = loadClass(
                "com.hunyuan.sa.bpm.module.runtime.event.BpmApprovalStageEngineEffectRequestedEvent"
        );
        Method handler = listenerType.getMethod("handle", eventType);
        TransactionalEventListener listener = handler.getAnnotation(TransactionalEventListener.class);

        assertThat(listener).isNotNull();
        assertThat(listener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(listener.fallbackExecution()).isFalse();
    }

    @Test
    void listenerShouldRouteCommittedEngineEffectsThroughTheSingleControlPort() {
        ApprovalStageControl control = Mockito.mock(ApprovalStageControl.class);
        BpmApprovalStageEngineEffectAfterCommitListener listener = listener(control);

        listener.handle(new BpmApprovalStageEngineEffectRequestedEvent(
                "execution-approve", EngineEffect.COMPLETE_ONCE, "APPROVED"
        ));
        listener.handle(new BpmApprovalStageEngineEffectRequestedEvent(
                "execution-return", EngineEffect.CLOSE_ONCE, "RETURNED"
        ));

        verify(control).completeOnce("execution-approve");
        verify(control).closeOnce("execution-return", "RETURNED");
    }

    @Test
    void listenerShouldKeepPostCommitEngineFailuresOutOfTheCompletedActionResponse() {
        ApprovalStageControl control = Mockito.mock(ApprovalStageControl.class);
        BpmApprovalStageEngineEffectAfterCommitListener listener = listener(control);
        IllegalStateException engineFailure = new IllegalStateException("engine unavailable");
        doThrow(engineFailure).when(control).closeOnce("execution-reject", "REJECTED");
        Logger logger = Logger.getLogger(BpmApprovalStageEngineEffectAfterCommitListener.class.getName());
        Level originalLevel = logger.getLevel();

        logger.setLevel(Level.OFF);
        try {
            assertThatCode(() -> listener.handle(new BpmApprovalStageEngineEffectRequestedEvent(
                    "execution-reject", EngineEffect.CLOSE_ONCE, "REJECTED"
            ))).doesNotThrowAnyException();
        } finally {
            logger.setLevel(originalLevel);
        }

        verify(control).closeOnce("execution-reject", "REJECTED");
    }

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new AssertionError("Expected after-commit engine effect component is missing", ex);
        }
    }

    private BpmApprovalStageEngineEffectAfterCommitListener listener(ApprovalStageControl control) {
        BpmApprovalStageEngineEffectAfterCommitListener listener = new BpmApprovalStageEngineEffectAfterCommitListener();
        try {
            Field field = listener.getClass().getDeclaredField("approvalStageControl");
            field.setAccessible(true);
            field.set(listener, control);
            return listener;
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

}
