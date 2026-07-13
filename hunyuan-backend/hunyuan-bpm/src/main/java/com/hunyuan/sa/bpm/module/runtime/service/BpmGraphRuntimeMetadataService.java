package com.hunyuan.sa.bpm.module.runtime.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.engine.graph.GraphNodeType;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionElementMappingDao;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionElementMappingEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Resolves immutable Graph runtime metadata without guessing from Flowable ids.
 */
@Service
public class BpmGraphRuntimeMetadataService {

    private final GraphDefinitionVersionDao graphDefinitionVersionDao;
    private final GraphDefinitionElementMappingDao graphDefinitionElementMappingDao;

    public BpmGraphRuntimeMetadataService(
            GraphDefinitionVersionDao graphDefinitionVersionDao,
            GraphDefinitionElementMappingDao graphDefinitionElementMappingDao
    ) {
        this.graphDefinitionVersionDao = graphDefinitionVersionDao;
        this.graphDefinitionElementMappingDao = graphDefinitionElementMappingDao;
    }

    public GraphNodeMetadata resolveCompiledNode(Long graphDefinitionVersionId, String compiledElementId) {
        GraphDefinitionElementMappingEntity mapping = graphDefinitionElementMappingDao
                .selectByGraphDefinitionVersionIdAndCompiledElementId(graphDefinitionVersionId, compiledElementId);
        if (mapping == null || !"NODE".equals(mapping.getAuthoredElementKind())) {
            throw new IllegalArgumentException("Graph 编译节点映射不存在：" + compiledElementId);
        }
        return requireNode(graphDefinitionVersionId, mapping.getAuthoredElementId());
    }

    public GraphNodeMetadata requireNode(Long graphDefinitionVersionId, String authoredNodeId) {
        return listNodes(graphDefinitionVersionId).stream()
                .filter(node -> authoredNodeId.equals(node.authoredNodeId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Graph authored 节点不存在：" + authoredNodeId));
    }

    public List<GraphNodeMetadata> listNodes(Long graphDefinitionVersionId) {
        GraphDefinitionVersionEntity version = graphDefinitionVersionDao.selectById(graphDefinitionVersionId);
        if (version == null || version.getGraphSnapshotJson() == null) {
            throw new IllegalArgumentException("Graph 定义版本快照不存在：" + graphDefinitionVersionId);
        }
        JSONObject graph = JSON.parseObject(version.getGraphSnapshotJson());
        JSONArray nodes = graph == null ? null : graph.getJSONArray("nodes");
        if (nodes == null) {
            throw new IllegalArgumentException("Graph 定义版本缺少节点快照：" + graphDefinitionVersionId);
        }
        return nodes.stream()
                .filter(JSONObject.class::isInstance)
                .map(JSONObject.class::cast)
                .map(node -> new GraphNodeMetadata(
                        node.getString("nodeId"),
                        node.getString("scopeId"),
                        node.getString("name"),
                        GraphNodeType.valueOf(node.getString("type")),
                        node.getJSONObject("properties") == null ? new JSONObject(true) : node.getJSONObject("properties")
                ))
                .toList();
    }

    public record GraphNodeMetadata(
            String authoredNodeId,
            String scopeId,
            String nodeName,
            GraphNodeType nodeType,
            JSONObject properties
    ) {
    }
}
