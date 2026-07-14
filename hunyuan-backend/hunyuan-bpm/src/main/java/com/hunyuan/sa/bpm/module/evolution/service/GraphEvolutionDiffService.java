package com.hunyuan.sa.bpm.module.evolution.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.module.evolution.domain.model.GraphEvolutionDiff;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class GraphEvolutionDiffService {

    public GraphEvolutionDiff compare(
            String sourceGraphJson,
            String sourceLayoutJson,
            String sourceDependenciesJson,
            String targetGraphJson,
            String targetLayoutJson,
            String targetDependenciesJson
    ) {
        JSONObject source = requireObject(sourceGraphJson, "源 Graph");
        JSONObject target = requireObject(targetGraphJson, "目标 Graph");
        List<GraphEvolutionDiff.Change> changes = new ArrayList<>();
        compareElements(source.getJSONArray("nodes"), target.getJSONArray("nodes"), "nodeId", "NODE", changes);
        compareElements(source.getJSONArray("edges"), target.getJSONArray("edges"), "edgeId", "EDGE", changes);
        compareElements(source.getJSONArray("scopes"), target.getJSONArray("scopes"), "scopeId", "SCOPE", changes);
        if (!jsonEquivalent(sourceDependenciesJson, targetDependenciesJson)) {
            changes.add(new GraphEvolutionDiff.Change("DEPENDENCY_CHANGED", null, "冻结依赖版本发生变化"));
        }
        boolean layoutChanged = !jsonEquivalent(sourceLayoutJson, targetLayoutJson);
        if (layoutChanged) {
            changes.add(new GraphEvolutionDiff.Change("LAYOUT_CHANGED", null, "画布布局发生变化"));
        }
        boolean semanticChanged = changes.stream().anyMatch(change -> !"LAYOUT_CHANGED".equals(change.kind()));
        return new GraphEvolutionDiff(semanticChanged, layoutChanged, semanticChanged, List.copyOf(changes));
    }

    private void compareElements(
            JSONArray sourceArray,
            JSONArray targetArray,
            String idField,
            String prefix,
            List<GraphEvolutionDiff.Change> changes
    ) {
        Map<String, JSONObject> source = index(sourceArray, idField, prefix);
        Map<String, JSONObject> target = index(targetArray, idField, prefix);
        source.forEach((id, value) -> {
            if (!target.containsKey(id)) {
                changes.add(new GraphEvolutionDiff.Change(prefix + "_REMOVED", id, "稳定元素已删除"));
            } else if (!Objects.equals(value, target.get(id))) {
                changes.add(new GraphEvolutionDiff.Change(prefix + "_CONFIG_CHANGED", id, "稳定元素配置或连接发生变化"));
            }
        });
        target.forEach((id, value) -> {
            if (!source.containsKey(id)) {
                changes.add(new GraphEvolutionDiff.Change(prefix + "_ADDED", id, "新增稳定元素"));
            }
        });
    }

    private Map<String, JSONObject> index(JSONArray array, String idField, String prefix) {
        Map<String, JSONObject> result = new LinkedHashMap<>();
        if (array == null) {
            return result;
        }
        for (Object value : array) {
            JSONObject element = (JSONObject) value;
            String id = element.getString(idField);
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException(prefix + " 缺少稳定 ID");
            }
            if (result.put(id, element) != null) {
                throw new IllegalArgumentException(prefix + " 稳定 ID 重复: " + id);
            }
        }
        return result;
    }

    private boolean jsonEquivalent(String left, String right) {
        Object leftValue = left == null || left.isBlank() ? null : JSON.parse(left);
        Object rightValue = right == null || right.isBlank() ? null : JSON.parse(right);
        return Objects.equals(leftValue, rightValue);
    }

    private JSONObject requireObject(String json, String label) {
        JSONObject value = JSON.parseObject(json);
        if (value == null) {
            throw new IllegalArgumentException(label + " 不能为空");
        }
        return value;
    }
}
