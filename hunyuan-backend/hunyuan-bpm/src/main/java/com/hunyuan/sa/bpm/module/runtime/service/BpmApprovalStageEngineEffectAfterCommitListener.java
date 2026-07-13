package com.hunyuan.sa.bpm.module.runtime.service;

import com.hunyuan.sa.base.config.AsyncConfig;
import com.hunyuan.sa.bpm.engine.graph.ApprovalStageControl;
import com.hunyuan.sa.bpm.module.runtime.event.BpmApprovalStageEngineEffectRequestedEvent;
import jakarta.annotation.Resource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 将引擎副作用移出审批成员、阶段和实例事实的锁定事务。
 */
@Component
public class BpmApprovalStageEngineEffectAfterCommitListener {

    private static final Logger LOGGER =
            Logger.getLogger(BpmApprovalStageEngineEffectAfterCommitListener.class.getName());

    @Resource
    private ApprovalStageControl approvalStageControl;

    @Resource(name = AsyncConfig.ASYNC_EXECUTOR_THREAD_NAME)
    private AsyncTaskExecutor asyncTaskExecutor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(BpmApprovalStageEngineEffectRequestedEvent event) {
        try {
            asyncTaskExecutor.execute(() -> executeEffect(event));
        } catch (RuntimeException ex) {
            logFailure(event, ex);
        }
    }

    private void executeEffect(BpmApprovalStageEngineEffectRequestedEvent event) {
        try {
            switch (event.engineEffect()) {
                case COMPLETE_ONCE -> approvalStageControl.completeOnce(event.stageInvocationId());
                case CLOSE_ONCE -> approvalStageControl.closeOnce(
                        event.stageInvocationId(),
                        event.terminalReason()
                );
                case NONE -> throw new IllegalArgumentException("审批阶段引擎副作用事件不能使用 NONE");
            }
        } catch (RuntimeException ex) {
            logFailure(event, ex);
        }
    }

    private void logFailure(BpmApprovalStageEngineEffectRequestedEvent event, RuntimeException ex) {
        LOGGER.log(
                Level.SEVERE,
                "审批阶段引擎副作用执行失败，已保留状态等待受控恢复：" + event.stageInvocationId(),
                ex
        );
    }
}
