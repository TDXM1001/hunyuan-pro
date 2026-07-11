package com.hunyuan.sa.bpm.engine.route;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 按稳定 key 和版本索引登记表达式。
 */
@Component
public class BpmRouteExpressionRegistry {

    private final Map<ExpressionId, BpmRouteExpression> expressionMap;

    public BpmRouteExpressionRegistry(List<BpmRouteExpression> expressions) {
        Map<ExpressionId, BpmRouteExpression> indexed = new LinkedHashMap<>();
        if (expressions != null) {
            for (BpmRouteExpression expression : expressions) {
                ExpressionId id = new ExpressionId(expression.expressionKey(), expression.version());
                if (indexed.putIfAbsent(id, expression) != null) {
                    throw new IllegalStateException("路由表达式重复登记：" + id.key() + "@" + id.version());
                }
            }
        }
        this.expressionMap = Map.copyOf(indexed);
    }

    public boolean contains(String expressionKey, Integer version) {
        return expressionKey != null
                && version != null
                && expressionMap.containsKey(new ExpressionId(expressionKey, version));
    }

    public Optional<BpmRouteExpression> find(String expressionKey, int version) {
        return Optional.ofNullable(expressionMap.get(new ExpressionId(expressionKey, version)));
    }

    public List<BpmRouteExpressionDescriptor> listDescriptors() {
        return expressionMap.values().stream()
                .map(BpmRouteExpression::descriptor)
                .toList();
    }

    private record ExpressionId(String key, int version) {
    }
}
