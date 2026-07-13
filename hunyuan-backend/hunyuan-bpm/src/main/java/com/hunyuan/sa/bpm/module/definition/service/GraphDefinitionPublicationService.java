package com.hunyuan.sa.bpm.module.definition.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.engine.compiler.graph.GraphBpmnCompiler;
import com.hunyuan.sa.bpm.engine.compiler.graph.GraphCompiledArtifact;
import com.hunyuan.sa.bpm.engine.compiler.graph.GraphCompiledElementMapping;
import com.hunyuan.sa.bpm.engine.graph.GraphDocumentCodec;
import com.hunyuan.sa.bpm.engine.graph.HunyuanProcessDefinitionGraph;
import com.hunyuan.sa.bpm.engine.internal.GraphFlowableDeployment;
import com.hunyuan.sa.bpm.engine.internal.GraphFlowableDeploymentGateway;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionElementMappingDao;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionElementMappingEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.definition.domain.form.GraphDefinitionPublishCommand;
import com.hunyuan.sa.bpm.module.definition.domain.vo.GraphDefinitionDetailVO;
import com.hunyuan.sa.bpm.module.definition.domain.vo.GraphDefinitionElementMappingVO;
import com.hunyuan.sa.bpm.module.category.dao.BpmCategoryDao;
import com.hunyuan.sa.bpm.module.category.domain.entity.BpmCategoryEntity;
import com.hunyuan.sa.bpm.module.model.dao.BpmProcessDraftDao;
import com.hunyuan.sa.bpm.module.model.domain.entity.BpmProcessDraftEntity;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 新 Graph 定义版本发布的服务边界。
 */
@Service
public class GraphDefinitionPublicationService {
    @Resource private BpmProcessDraftDao bpmProcessDraftDao; @Resource private GraphDefinitionVersionDao graphDefinitionVersionDao; @Resource private GraphDefinitionElementMappingDao graphDefinitionElementMappingDao; @Resource private GraphFlowableDeploymentGateway graphFlowableDeploymentGateway;
    @Resource private GraphPublicationDependencyResolver graphPublicationDependencyResolver;
    @Resource private BpmCategoryDao bpmCategoryDao;
    private final GraphDocumentCodec codec = new GraphDocumentCodec(); private final GraphBpmnCompiler compiler = new GraphBpmnCompiler();

    public String compilerVersion() {
        return "graph-v1";
    }
    @Transactional(rollbackFor=Exception.class)
    public Long publish(GraphDefinitionPublishCommand command) {
      BpmProcessDraftEntity draft=bpmProcessDraftDao.selectById(command.draftId()); if(draft==null) throw new IllegalArgumentException("Graph 草稿不存在");
      if (draft.getCategoryId() == null) throw new IllegalArgumentException("Graph 草稿必须选择流程分类");
      BpmCategoryEntity category = bpmCategoryDao.selectById(draft.getCategoryId()); if (category == null || Boolean.TRUE.equals(category.getDeletedFlag())) throw new IllegalArgumentException("Graph 草稿引用的流程分类不存在");
      HunyuanProcessDefinitionGraph graph=codec.restoreStored(draft.getGraphJson(),draft.getLayoutJson()); GraphPublicationDependencySnapshot dependencies=graphPublicationDependencyResolver.resolve(graph); GraphCompiledArtifact artifact=compiler.compile(draft.getProcessKey(),draft.getProcessName(),graph); GraphFlowableDeployment deployment=graphFlowableDeploymentGateway.deploy(draft.getProcessKey(),draft.getProcessName(),artifact.compiledBpmnXml());
      try { GraphDefinitionVersionEntity version=new GraphDefinitionVersionEntity(); version.setDraftId(draft.getDraftId()); version.setProcessKey(draft.getProcessKey()); version.setProcessNameSnapshot(draft.getProcessName()); version.setCategoryIdSnapshot(category == null ? null : category.getCategoryId()); version.setCategoryNameSnapshot(category == null ? null : category.getCategoryName()); Integer max=graphDefinitionVersionDao.selectMaxVersionByProcessKey(draft.getProcessKey()); version.setDefinitionVersion(max==null?1:max+1); version.setLifecycleState("ACTIVE"); version.setGraphSnapshotJson(draft.getGraphJson()); version.setLayoutSnapshotJson(draft.getLayoutJson()); version.setSemanticHash(draft.getSemanticHash()); version.setDependencyVersionsJson(JSON.toJSONString(dependencies.toSnapshotMap())); version.setCompilerVersion(compilerVersion()); version.setCompiledBpmnXml(artifact.compiledBpmnXml()); version.setDeploymentId(deployment.deploymentId()); version.setEngineProcessDefinitionId(deployment.processDefinitionId()); version.setPublishedByEmployeeId(command.publishedByEmployeeId()); graphDefinitionVersionDao.insert(version);
        for(GraphCompiledElementMapping mapping:artifact.mappings()){ GraphDefinitionElementMappingEntity e=new GraphDefinitionElementMappingEntity(); e.setGraphDefinitionVersionId(version.getGraphDefinitionVersionId()); e.setAuthoredElementId(mapping.authoredElementId()); e.setAuthoredElementKind(mapping.authoredElementKind()); e.setCompiledElementId(mapping.compiledElementId()); e.setCompiledElementType(mapping.compiledElementType()); graphDefinitionElementMappingDao.insert(e); } return version.getGraphDefinitionVersionId();
      } catch(RuntimeException ex){ graphFlowableDeploymentGateway.delete(deployment); throw ex; }
    }
    @Transactional(readOnly = true)
    public ResponseDTO<GraphDefinitionDetailVO> getDefinitionDetail(Long versionId) {
      GraphDefinitionVersionEntity version = graphDefinitionVersionDao.selectById(versionId);
      if (version == null) {
        return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
      }
      return ResponseDTO.ok(buildDefinitionDetail(version));
    }

    @Transactional(readOnly = true)
    public ResponseDTO<GraphDefinitionDetailVO> getLatestDefinitionDetail(Long draftId) {
      GraphDefinitionVersionEntity version = graphDefinitionVersionDao.selectLatestByDraftId(draftId);
      return version == null ? ResponseDTO.ok() : ResponseDTO.ok(buildDefinitionDetail(version));
    }

    private GraphDefinitionDetailVO buildDefinitionDetail(GraphDefinitionVersionEntity version) {
      List<GraphDefinitionElementMappingEntity> mappings = graphDefinitionElementMappingDao.selectList(
              new LambdaQueryWrapper<GraphDefinitionElementMappingEntity>()
                      .eq(GraphDefinitionElementMappingEntity::getGraphDefinitionVersionId, version.getGraphDefinitionVersionId())
                      .orderByAsc(GraphDefinitionElementMappingEntity::getMappingId)
      );
      GraphDefinitionDetailVO detail = new GraphDefinitionDetailVO();
      detail.setGraphDefinitionVersionId(version.getGraphDefinitionVersionId());
      detail.setProcessKey(version.getProcessKey());
      detail.setDefinitionVersion(version.getDefinitionVersion());
      detail.setLifecycleState(version.getLifecycleState());
      detail.setGraphSnapshotJson(version.getGraphSnapshotJson());
      detail.setLayoutSnapshotJson(version.getLayoutSnapshotJson());
      detail.setSemanticHash(version.getSemanticHash());
      detail.setDependencyVersionsJson(version.getDependencyVersionsJson());
      detail.setCompilerVersion(version.getCompilerVersion());
      detail.setCompiledBpmnXml(version.getCompiledBpmnXml());
      detail.setDeploymentId(version.getDeploymentId());
      detail.setEngineProcessDefinitionId(version.getEngineProcessDefinitionId());
      detail.setPublishedByEmployeeId(version.getPublishedByEmployeeId());
      detail.setPublishedAt(version.getCreateTime());
      detail.setMappings(mappings.stream().map(this::toMappingVO).toList());
      return detail;
    }
    public void deactivate(Long versionId) { GraphDefinitionVersionEntity version=graphDefinitionVersionDao.selectById(versionId); if(version==null) throw new IllegalArgumentException("Graph 定义版本不存在"); graphFlowableDeploymentGateway.suspend(version.getEngineProcessDefinitionId()); if(graphDefinitionVersionDao.deactivate(versionId)!=1) throw new IllegalStateException("Graph 定义版本已下线或状态已变更"); }

    private GraphDefinitionElementMappingVO toMappingVO(GraphDefinitionElementMappingEntity entity) {
      GraphDefinitionElementMappingVO result = new GraphDefinitionElementMappingVO();
      result.setAuthoredElementId(entity.getAuthoredElementId());
      result.setAuthoredElementKind(entity.getAuthoredElementKind());
      result.setCompiledElementId(entity.getCompiledElementId());
      result.setCompiledElementType(entity.getCompiledElementType());
      return result;
    }
}
