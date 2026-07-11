package com.hunyuan.sa.bpm.engine.route;

import java.util.Map;

/**
 * 后端登记并测试后才能在流程模型中引用的路由表达式。
 */
public interface BpmRouteExpression {

    String expressionKey();

    int version();

    BpmRouteExpressionDescriptor descriptor();

    BpmRouteExpressionResult evaluate(BpmRouteExpressionContext context, Map<String, Object> parameters);
}
