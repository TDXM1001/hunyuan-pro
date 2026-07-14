package com.hunyuan.sa.bpm.module.runtime.service;

import com.hunyuan.sa.bpm.module.runtime.domain.model.RouteCondition;
import com.hunyuan.sa.bpm.engine.route.BpmRouteExpression;
import com.hunyuan.sa.bpm.engine.route.BpmRouteExpressionContext;
import com.hunyuan.sa.bpm.engine.route.BpmRouteExpressionRegistry;
import com.hunyuan.sa.bpm.engine.route.BpmRouteExpressionResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * 计算类型化规则或后端登记表达式，不执行用户提供的代码。
 */
@Component
public class BpmRouteConditionEvaluator {

    private final BpmRouteExpressionRegistry expressionRegistry;

    public BpmRouteConditionEvaluator(BpmRouteExpressionRegistry expressionRegistry) {
        this.expressionRegistry = expressionRegistry;
    }

    public BpmRouteConditionResult evaluate(
            RouteCondition condition,
            Map<String, Object> formData,
            BpmRouteExpressionContext context
    ) {
        if (condition == null) {
            throw new IllegalArgumentException("ROUTE_CONDITION_MISSING：路由条件不存在");
        }
        if ("REGISTERED_EXPRESSION".equals(condition.sourceType())) {
            BpmRouteExpression expression = expressionRegistry
                    .find(condition.expressionKey(), condition.expressionVersion())
                    .orElseThrow(() -> new IllegalArgumentException("ROUTE_EXPRESSION_NOT_REGISTERED：路由表达式未登记"));
            BpmRouteExpressionResult result = expression.evaluate(context, condition.parameters());
            return new BpmRouteConditionResult(result.matched(), result.reasonCode(), result.reasonText());
        }

        Object actualValue = formData == null ? null : formData.get(condition.fieldKey());
        boolean matched = evaluateTyped(actualValue, condition.compareValue(), condition.valueType(), condition.operator());
        return new BpmRouteConditionResult(
                matched,
                matched ? "ROUTE_CONDITION_MATCHED" : "ROUTE_CONDITION_NOT_MATCHED",
                matched ? "条件已满足" : "条件未满足"
        );
    }

    private boolean evaluateTyped(Object actual, Object expected, String valueType, String operator) {
        if ("EMPTY".equals(operator)) {
            return isEmpty(actual);
        }
        if ("NOT_EMPTY".equals(operator)) {
            return !isEmpty(actual);
        }
        // 缺失字段对普通比较视为未命中，由路由节点统一选择默认分支。
        if (actual == null) {
            return false;
        }
        if ("NUMBER".equals(valueType)) {
            int comparison = toBigDecimal(actual).compareTo(toBigDecimal(expected));
            return switch (operator) {
                case "EQ" -> comparison == 0;
                case "NEQ" -> comparison != 0;
                case "GT" -> comparison > 0;
                case "GTE" -> comparison >= 0;
                case "LT" -> comparison < 0;
                case "LTE" -> comparison <= 0;
                default -> throw unsupportedOperator(operator, valueType);
            };
        }
        if ("BOOLEAN".equals(valueType)) {
            if (!(actual instanceof Boolean) || !(expected instanceof Boolean)) {
                throw typeMismatch("BOOLEAN");
            }
            return switch (operator) {
                case "EQ" -> Objects.equals(actual, expected);
                case "NEQ" -> !Objects.equals(actual, expected);
                default -> throw unsupportedOperator(operator, valueType);
            };
        }

        String actualText = actual == null ? null : String.valueOf(actual);
        return switch (operator) {
            case "EQ" -> Objects.equals(actualText, expected == null ? null : String.valueOf(expected));
            case "NEQ" -> !Objects.equals(actualText, expected == null ? null : String.valueOf(expected));
            case "IN" -> asCollection(expected).stream().map(String::valueOf).anyMatch(item -> Objects.equals(item, actualText));
            case "NOT_IN" -> asCollection(expected).stream().map(String::valueOf).noneMatch(item -> Objects.equals(item, actualText));
            case "CONTAINS" -> actualText != null && actualText.contains(String.valueOf(expected));
            case "NOT_CONTAINS" -> actualText == null || !actualText.contains(String.valueOf(expected));
            default -> throw unsupportedOperator(operator, valueType);
        };
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            throw typeMismatch("NUMBER");
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw typeMismatch("NUMBER");
        }
    }

    private Collection<?> asCollection(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection;
        }
        throw typeMismatch("COLLECTION");
    }

    private boolean isEmpty(Object value) {
        return value == null
                || value instanceof String text && text.isBlank()
                || value instanceof Collection<?> collection && collection.isEmpty();
    }

    private IllegalArgumentException typeMismatch(String expectedType) {
        return new IllegalArgumentException("ROUTE_VALUE_TYPE_MISMATCH：路由值不是 " + expectedType);
    }

    private IllegalArgumentException unsupportedOperator(String operator, String valueType) {
        return new IllegalArgumentException("ROUTE_OPERATOR_TYPE_MISMATCH：" + valueType + " 不支持 " + operator);
    }
}
