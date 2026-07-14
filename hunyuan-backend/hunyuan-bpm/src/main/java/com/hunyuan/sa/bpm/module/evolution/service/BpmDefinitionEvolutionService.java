package com.hunyuan.sa.bpm.module.evolution.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceRunStateEnum;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.evolution.domain.model.GraphEvolutionDiff;
import com.hunyuan.sa.bpm.module.evolution.domain.vo.GraphEvolutionDiffVO;
import com.hunyuan.sa.bpm.module.evolution.domain.vo.BpmAffectedInstanceVO;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BpmDefinitionEvolutionService {
    @Resource private GraphDefinitionVersionDao versionDao;
    @Resource private BpmInstanceDao instanceDao;
    @Resource private GraphEvolutionDiffService diffService;

    public ResponseDTO<GraphEvolutionDiffVO> diff(Long sourceVersionId, Long targetVersionId) {
        GraphDefinitionVersionEntity source = versionDao.selectById(sourceVersionId);
        GraphDefinitionVersionEntity target = versionDao.selectById(targetVersionId);
        if (source == null || target == null) return ResponseDTO.userErrorParam("源或目标 Graph 定义版本不存在");
        if (!source.getProcessKey().equals(target.getProcessKey())) return ResponseDTO.userErrorParam("只允许比较同一流程键的版本");
        GraphEvolutionDiff diff = diffService.compare(source.getGraphSnapshotJson(), source.getLayoutSnapshotJson(),
                source.getDependencyVersionsJson(), target.getGraphSnapshotJson(), target.getLayoutSnapshotJson(),
                target.getDependencyVersionsJson());
        GraphEvolutionDiffVO vo = new GraphEvolutionDiffVO();
        vo.setSourceVersionId(sourceVersionId); vo.setTargetVersionId(targetVersionId);
        vo.setSemanticChanged(diff.semanticChanged()); vo.setLayoutChanged(diff.layoutChanged());
        vo.setMigrationSuggested(diff.migrationSuggested()); vo.setChanges(diff.changes());
        return ResponseDTO.ok(vo);
    }

    public ResponseDTO<List<BpmAffectedInstanceVO>> affectedInstances(Long sourceVersionId) {
        List<BpmInstanceEntity> instances = instanceDao.selectList(Wrappers.<BpmInstanceEntity>lambdaQuery()
                .eq(BpmInstanceEntity::getGraphDefinitionVersionId, sourceVersionId)
                .eq(BpmInstanceEntity::getRunState, BpmInstanceRunStateEnum.RUNNING.getValue())
                .orderByDesc(BpmInstanceEntity::getStartedAt));
        return ResponseDTO.ok(instances.stream().map(this::toAffectedVO).toList());
    }

    private BpmAffectedInstanceVO toAffectedVO(BpmInstanceEntity instance) {
        BpmAffectedInstanceVO vo = new BpmAffectedInstanceVO();
        vo.setInstanceId(instance.getInstanceId()); vo.setInstanceNo(instance.getInstanceNo());
        vo.setGraphDefinitionVersionId(instance.getGraphDefinitionVersionId());
        vo.setDefinitionKeySnapshot(instance.getDefinitionKeySnapshot()); vo.setTitle(instance.getTitle());
        vo.setBusinessKey(instance.getBusinessKey()); vo.setRunState(instance.getRunState());
        vo.setActiveTaskCount(instance.getActiveTaskCount()); vo.setStartedAt(instance.getStartedAt());
        return vo;
    }
}
