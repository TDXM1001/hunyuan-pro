package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.engine.ast.RouteCondition;
import com.hunyuan.sa.bpm.engine.route.BpmRouteExpressionRegistry;
import com.hunyuan.sa.bpm.module.runtime.service.BpmRouteConditionEvaluator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BpmRouteConditionEvaluatorTest {

    private final BpmRouteConditionEvaluator evaluator = new BpmRouteConditionEvaluator(
            new BpmRouteExpressionRegistry(List.of())
    );

    @Test
    void evaluateShouldCompareNumbersWithoutScaleDrift() {
        RouteCondition condition = fieldCondition("amount", "NUMBER", "GTE", "5000.00");

        assertThat(evaluator.evaluate(condition, Map.of("amount", new BigDecimal("5000")), null).matched())
                .isTrue();
    }

    @Test
    void evaluateShouldSupportTextMembership() {
        RouteCondition condition = fieldCondition("expenseType", "TEXT", "IN", List.of("TRAVEL", "TRAINING"));

        assertThat(evaluator.evaluate(condition, Map.of("expenseType", "TRAVEL"), null).matched())
                .isTrue();
    }

    @Test
    void evaluateShouldHandleEmptyWithoutConvertingMissingValue() {
        RouteCondition condition = fieldCondition("remark", "TEXT", "EMPTY", null);

        assertThat(evaluator.evaluate(condition, Map.of(), null).matched()).isTrue();
    }

    @Test
    void evaluateShouldTreatMissingNumberAsNotMatchedSoRouteCanUseDefaultBranch() {
        RouteCondition condition = fieldCondition("amount", "NUMBER", "LTE", 5000);

        assertThat(evaluator.evaluate(condition, Map.of(), null).matched()).isFalse();
    }

    @Test
    void evaluateShouldRejectNumberTypeMismatch() {
        RouteCondition condition = fieldCondition("amount", "NUMBER", "GT", 100);

        assertThatThrownBy(() -> evaluator.evaluate(condition, Map.of("amount", "not-a-number"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ROUTE_VALUE_TYPE_MISMATCH");
    }

    private RouteCondition fieldCondition(
            String fieldKey,
            String valueType,
            String operator,
            Object compareValue
    ) {
        return new RouteCondition(
                "FORM_FIELD",
                fieldKey,
                valueType,
                operator,
                compareValue,
                null,
                null,
                Map.of()
        );
    }
}
