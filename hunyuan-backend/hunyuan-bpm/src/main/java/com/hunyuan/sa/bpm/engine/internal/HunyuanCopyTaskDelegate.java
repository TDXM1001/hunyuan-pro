package com.hunyuan.sa.bpm.engine.internal;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmCopyRecipientResolver;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceCopyService;
import jakarta.annotation.Resource;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 将 Flowable 的设计时抄送节点投影为 Hunyuan 幂等事实。
 */
@Component("hunyuanCopyTaskDelegate")
public class HunyuanCopyTaskDelegate implements JavaDelegate {

    @Resource
    private BpmInstanceDao bpmInstanceDao;

    @Resource
    private BpmDefinitionNodeDao bpmDefinitionNodeDao;

    @Resource
    private BpmCopyRecipientResolver bpmCopyRecipientResolver;

    @Resource
    private BpmInstanceCopyService bpmInstanceCopyService;

    private Expression copyNodeKey;

    @Override
    public void execute(DelegateExecution execution) {
        Object rawInstanceId = execution.getVariable("hunyuanInstanceId");
        if (rawInstanceId == null) {
            throw new IllegalArgumentException("HUNYUAN_INSTANCE_ID_MISSING：Flowable 缺少 Hunyuan 实例ID");
        }
        Long instanceId = Long.valueOf(String.valueOf(rawInstanceId));
        String nodeKey = String.valueOf(copyNodeKey.getValue(execution));
        BpmInstanceEntity instance = bpmInstanceDao.selectById(instanceId);
        if (instance == null) {
            throw new IllegalArgumentException("COPY_INSTANCE_NOT_FOUND：抄送节点未找到 Hunyuan 实例");
        }
        BpmDefinitionNodeEntity node = bpmDefinitionNodeDao.selectOne(
                Wrappers.<BpmDefinitionNodeEntity>lambdaQuery()
                        .eq(BpmDefinitionNodeEntity::getDefinitionId, instance.getDefinitionId())
                        .eq(BpmDefinitionNodeEntity::getNodeKey, nodeKey)
        );
        if (node == null || !"COPY_TASK".equals(node.getNodeType())) {
            throw new IllegalArgumentException("COPY_SNAPSHOT_NOT_FOUND：设计时抄送节点冻结快照不存在");
        }
        List<Long> recipientIds = bpmCopyRecipientResolver.resolve(instance, node);
        ResponseDTO<String> response = bpmInstanceCopyService.createDesignCopies(
                instance,
                node,
                execution.getProcessInstanceId(),
                recipientIds
        );
        if (!Boolean.TRUE.equals(response.getOk())) {
            throw new IllegalArgumentException(response.getMsg());
        }
    }
}
