package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.engine.graph.GraphNodeType;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionElementMappingDao;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionElementMappingEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmGraphRuntimeMetadataService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class BpmGraphRuntimeMetadataServiceTest {

    @Test
    void resolveCompiledNodeShouldReturnAuthoredHandleMetadata() {
        GraphDefinitionVersionDao versionDao = Mockito.mock(GraphDefinitionVersionDao.class);
        GraphDefinitionElementMappingDao mappingDao = Mockito.mock(GraphDefinitionElementMappingDao.class);
        GraphDefinitionVersionEntity version = new GraphDefinitionVersionEntity();
        version.setGraphDefinitionVersionId(41L);
        version.setGraphSnapshotJson("""
                {"schemaVersion":1,"rootScopeId":"scope_root","scopes":[],"nodes":[
                  {"nodeId":"archive_handle","scopeId":"scope_root","type":"HANDLE","name":"归档办理","properties":{"allowedActions":["COMPLETE","RETURN"]},"layout":{}}
                ],"edges":[],"metadata":{}}
                """);
        GraphDefinitionElementMappingEntity mapping = new GraphDefinitionElementMappingEntity();
        mapping.setAuthoredElementId("archive_handle");
        mapping.setAuthoredElementKind("NODE");
        mapping.setCompiledElementId("graph_node_archive_handle");
        when(versionDao.selectById(41L)).thenReturn(version);
        when(mappingDao.selectByGraphDefinitionVersionIdAndCompiledElementId(41L, "graph_node_archive_handle"))
                .thenReturn(mapping);

        BpmGraphRuntimeMetadataService service = new BpmGraphRuntimeMetadataService(versionDao, mappingDao);

        BpmGraphRuntimeMetadataService.GraphNodeMetadata result =
                service.resolveCompiledNode(41L, "graph_node_archive_handle");

        assertThat(result.authoredNodeId()).isEqualTo("archive_handle");
        assertThat(result.nodeName()).isEqualTo("归档办理");
        assertThat(result.nodeType()).isEqualTo(GraphNodeType.HANDLE);
        assertThat(result.properties().getJSONArray("allowedActions").toJavaList(String.class))
                .containsExactly("COMPLETE", "RETURN");
    }
}
