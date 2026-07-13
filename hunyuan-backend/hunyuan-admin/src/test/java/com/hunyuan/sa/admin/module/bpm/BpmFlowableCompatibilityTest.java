package com.hunyuan.sa.admin.module.bpm;

import com.hunyuan.sa.admin.module.bpm.adapter.AdminBpmCurrentActorProvider;
import com.hunyuan.sa.admin.module.bpm.adapter.AdminBpmOrgIdentityGateway;
import com.hunyuan.sa.admin.module.system.department.service.DepartmentService;
import com.hunyuan.sa.admin.module.system.employee.dao.OrganizationRelationDao;
import com.hunyuan.sa.admin.module.system.employee.service.EmployeeService;
import com.hunyuan.sa.admin.module.system.login.manager.LoginManager;
import com.hunyuan.sa.admin.module.system.role.service.RoleEmployeeService;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.config.BpmFlowableAutoConfiguration;
import com.hunyuan.sa.bpm.engine.compiler.CompiledDefinitionArtifact;
import com.hunyuan.sa.bpm.engine.compiler.SimpleModelBpmnCompiler;
import com.hunyuan.sa.bpm.engine.compiler.graph.GraphBpmnCompiler;
import com.hunyuan.sa.bpm.engine.graph.GraphEdge;
import com.hunyuan.sa.bpm.engine.graph.GraphNode;
import com.hunyuan.sa.bpm.engine.graph.GraphNodeType;
import com.hunyuan.sa.bpm.engine.graph.GraphScope;
import com.hunyuan.sa.bpm.engine.graph.HunyuanProcessDefinitionGraph;
import com.hunyuan.sa.bpm.engine.internal.HunyuanDelayEndListener;
import com.hunyuan.sa.bpm.engine.internal.HunyuanDelayStartListener;
import com.hunyuan.sa.bpm.engine.internal.HunyuanGraphRouteDecisionListener;
import com.hunyuan.sa.bpm.engine.internal.HunyuanExternalTriggerDelegate;
import com.hunyuan.sa.bpm.engine.internal.HunyuanExternalWaitListener;
import com.hunyuan.sa.bpm.engine.internal.HunyuanTimeEventDelegate;
import com.hunyuan.sa.bpm.engine.internal.HunyuanSubProcessLifecycleListener;
import com.hunyuan.sa.bpm.engine.internal.HunyuanSubProcessInstanceStartListener;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.integration.service.BpmConnectorInvocationService;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmExternalWaitService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmGraphRuntimeMetadataService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmRouteDecisionResult;
import com.hunyuan.sa.bpm.module.runtime.service.BpmRouteDecisionService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmSubProcessService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTimeEventService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 BPM 模块可以把 Flowable 作为隐藏内核接入，
 * 同时避免被完整管理端上下文和基础库表初始化噪音干扰。
 */
@SpringBootTest(
        classes = BpmFlowableCompatibilityTest.BpmCompatibilityTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "bpm.flowable.enabled=true",
                "bpm.flowable.database-schema-update=create-drop",
                "bpm.flowable.async-executor-activate=false",
                "flowable.app.enabled=false",
                "flowable.cmmn.enabled=false",
                "flowable.dmn.enabled=false",
                "flowable.idm.enabled=false",
                "flowable.eventregistry.enabled=false",
                "knife4j.enable=false",
                "smart.job.enabled=false",
                "spring.cache.type=simple",
                "spring.data.redis.repositories.enabled=false"
        }
)
class BpmFlowableCompatibilityTest {

    private static final String TEST_SCHEMA_NAME = "hunyuan_bpm_test_" + System.currentTimeMillis();

    private static final String MYSQL_ADMIN_JDBC_URL = "jdbc:mysql://127.0.0.1:3306/mysql"
            + "?useSSL=false&allowMultiQueries=true&serverTimezone=Asia/Shanghai";

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "org.redisson.spring.starter.RedissonAutoConfigurationV2",
            "cn.dev33.satoken.spring.SaBeanRegister",
            "cn.dev33.satoken.spring.SaBeanInject",
            "org.flowable.spring.boot.FlowableJpaAutoConfiguration",
            "org.flowable.spring.boot.EndpointAutoConfiguration",
            "org.flowable.spring.boot.RestApiAutoConfiguration",
            "org.flowable.spring.boot.FlowableSecurityAutoConfiguration",
            "org.flowable.spring.boot.actuate.info.FlowableInfoAutoConfiguration",
            "org.flowable.spring.boot.ldap.FlowableLdapAutoConfiguration",
            "org.flowable.spring.boot.app.AppEngineAutoConfiguration",
            "org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration",
            "org.flowable.spring.boot.dmn.DmnEngineAutoConfiguration",
            "org.flowable.spring.boot.dmn.DmnEngineServicesAutoConfiguration",
            "org.flowable.spring.boot.cmmn.CmmnEngineAutoConfiguration",
            "org.flowable.spring.boot.cmmn.CmmnEngineServicesAutoConfiguration",
            "org.flowable.spring.boot.idm.IdmEngineAutoConfiguration",
            "org.flowable.spring.boot.idm.IdmEngineServicesAutoConfiguration",
            "org.flowable.spring.boot.eventregistry.EventRegistryAutoConfiguration",
            "org.flowable.spring.boot.eventregistry.EventRegistryServicesAutoConfiguration"
    })
    @Import({
            BpmFlowableAutoConfiguration.class,
            SimpleModelBpmnCompiler.class,
            HunyuanDelayStartListener.class,
            HunyuanDelayEndListener.class,
            HunyuanGraphRouteDecisionListener.class,
            HunyuanExternalTriggerDelegate.class,
            HunyuanExternalWaitListener.class,
            HunyuanTimeEventDelegate.class,
            HunyuanSubProcessLifecycleListener.class,
            HunyuanSubProcessInstanceStartListener.class,
            AdminBpmOrgIdentityGateway.class,
            AdminBpmCurrentActorProvider.class
    })
    static class BpmCompatibilityTestApplication {
    }

    @MockBean
    private EmployeeService employeeService;

    @MockBean
    private DepartmentService departmentService;

    @MockBean
    private RoleEmployeeService roleEmployeeService;

    @MockBean
    private OrganizationRelationDao organizationRelationDao;

    @MockBean
    private LoginManager loginManager;

    @MockBean
    private BpmTimeEventService bpmTimeEventService;

    @MockBean
    private BpmRouteDecisionService bpmRouteDecisionService;

    @MockBean
    private BpmInstanceDao bpmInstanceDao;

    @MockBean
    private BpmDefinitionNodeDao bpmDefinitionNodeDao;

    @MockBean
    private BpmConnectorInvocationService bpmConnectorInvocationService;

    @MockBean
    private BpmExternalWaitService bpmExternalWaitService;

    @MockBean
    private BpmGraphRuntimeMetadataService bpmGraphRuntimeMetadataService;

    @MockBean
    private BpmSubProcessService bpmSubProcessService;

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        String jdbcUrl = "jdbc:mysql://127.0.0.1:3306/" + TEST_SCHEMA_NAME
                + "?createDatabaseIfNotExist=true&useServerPreparedStmts=false&rewriteBatchedStatements=true"
                + "&characterEncoding=UTF-8&useSSL=false&allowMultiQueries=true&serverTimezone=Asia/Shanghai";
        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.datasource.username", () -> "root");
        registry.add("spring.datasource.password", () -> "root");
        registry.add("spring.datasource.druid.username", () -> "root");
        registry.add("spring.datasource.druid.password", () -> "root");
    }

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private BpmOrgIdentityGateway bpmOrgIdentityGateway;

    @Autowired
    private BpmCurrentActorProvider bpmCurrentActorProvider;

    @Autowired
    private SimpleModelBpmnCompiler simpleModelBpmnCompiler;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Test
    void loadsFlowableAsHiddenKernelInsideBpmModule() {
        assertThat(processEngine).isNotNull();
        assertThat(bpmOrgIdentityGateway).isNotNull();
        assertThat(bpmCurrentActorProvider).isNotNull();
    }

    @Test
    void deploysControlledV2BranchModel() {
        CompiledDefinitionArtifact artifact = simpleModelBpmnCompiler.compile(
                "compat_v2",
                "v2 网关兼容",
                """
                        {"schemaVersion":2,"nodes":[
                          {"nodeKey":"route","name":"排他路由","type":"EXCLUSIVE_BRANCH","branches":[
                            {"branchKey":"a","name":"A","condition":{"sourceType":"FORM_FIELD","fieldKey":"amount","valueType":"NUMBER","operator":"GT","compareValue":0},"nodes":[{"nodeKey":"task_a","name":"A审批","type":"USER_TASK","candidateResolverType":"EMPLOYEE","employeeId":1}]},
                            {"branchKey":"default","name":"默认","isDefault":true,"nodes":[]}
                          ]},
                          {"nodeKey":"inclusive","name":"包容路由","type":"INCLUSIVE_BRANCH","branches":[
                            {"branchKey":"b","name":"B","condition":{"sourceType":"FORM_FIELD","fieldKey":"amount","valueType":"NUMBER","operator":"GTE","compareValue":100},"nodes":[{"nodeKey":"task_b","name":"B审批","type":"USER_TASK","candidateResolverType":"EMPLOYEE","employeeId":1}]},
                            {"branchKey":"default","name":"默认","isDefault":true,"nodes":[]}
                          ]}
                        ]}
                        """,
                "{\"type\":\"ALL\"}",
                "{}"
        );

        var deployment = repositoryService.createDeployment()
                .name("v2 网关兼容")
                .addBytes("compat-v2.bpmn20.xml", artifact.compiledBpmnXml().getBytes(StandardCharsets.UTF_8))
                .deploy();

        assertThat(repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).count())
                .isEqualTo(1);
    }

    @Test
    void deploysControlledV3TimeAndExternalWaitModel() {
        CompiledDefinitionArtifact artifact = simpleModelBpmnCompiler.compile(
                "compat_v3",
                "v3 时间与外部等待兼容",
                """
                        {"schemaVersion":3,"nodes":[
                          {
                            "nodeKey":"review","name":"财务审批","type":"USER_TASK","candidateResolverType":"EMPLOYEE","employeeId":1,
                            "taskSlaPolicy":{"dueAfter":"PT2H","reminderSchedule":["PT1H"],"timeoutAction":"REMIND_ONLY","systemActionComment":"审批超时提醒"}
                          },
                          {"nodeKey":"cooling","name":"冷静期","type":"DELAY","mode":"DURATION","value":"PT1H","timezone":"Asia/Shanghai","overduePolicy":"TRIGGER_IMMEDIATELY"},
                          {
                            "nodeKey":"finance_sync","name":"同步财务","type":"EXTERNAL_TRIGGER","connectorKey":"finance","operationKey":"createExpense",
                            "requestMapping":{"amount":"approvedAmount"},"responseMapping":{"externalNo":"financeNo"},
                            "waitMode":"WAIT_CALLBACK","timeoutPolicy":{"timeoutAfter":"PT30M"}
                          }
                        ]}
                        """,
                "{\"type\":\"ALL\"}",
                "{}"
        );

        var deployment = repositoryService.createDeployment()
                .name("v3 时间与外部等待兼容")
                .addBytes("compat-v3.bpmn20.xml", artifact.compiledBpmnXml().getBytes(StandardCharsets.UTF_8))
                .deploy();

        assertThat(repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).count())
                .isEqualTo(1);
    }

    @Test
    void deploysControlledGraphDefinitionWithConditionalParallelAndCopyNodes() {
        var artifact = new GraphBpmnCompiler().compile(
                "compat_graph_m1",
                "Graph M1 兼容",
                fullM1Graph()
        );

        var deployment = repositoryService.createDeployment()
                .name("Graph M1 兼容")
                .addBytes("compat-graph-m1.bpmn20.xml", artifact.compiledBpmnXml().getBytes(StandardCharsets.UTF_8))
                .deploy();

        assertThat(repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).count())
                .isEqualTo(1);
        assertThat(repositoryService.getBpmnModel(
                repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult().getId()
        ).getMainProcess().getFlowElement("graph_edge_route_large")).isNotNull();
    }

    @Test
    void deploysControlledGraphAdvancedRuntimeNodes() {
        var artifact = new GraphBpmnCompiler().compile(
                "compat_graph_m5",
                "Graph M5 兼容",
                new HunyuanProcessDefinitionGraph(
                        1, "scope_root", List.of(new GraphScope("scope_root", null, "主流程")),
                        List.of(
                                node("start", GraphNodeType.START, "开始"),
                                new GraphNode("delay", "scope_root", GraphNodeType.DELAY, "短暂等待",
                                        Map.of("mode", "DURATION", "value", "PT1M"), Map.of()),
                                new GraphNode("external", "scope_root", GraphNodeType.EXTERNAL_TRIGGER, "外部等待",
                                        Map.of("connectorKey", "finance", "connectorVersion", 1,
                                                "operationKey", "createExpense", "waitMode", "WAIT_CALLBACK",
                                                "timeoutPolicy", Map.of("timeoutAfter", "PT5M")), Map.of()),
                                new GraphNode("child", "scope_root", GraphNodeType.SUB_PROCESS, "子流程",
                                        Map.of("calledProcessKey", "compat_graph_child", "calledDefinitionVersionId", 1,
                                                "calledEngineProcessDefinitionId", "compat_graph_child:1:test",
                                                "failurePolicy", "PAUSE_PARENT", "cancelPropagation", "CANCEL_CHILD"), Map.of()),
                                node("end", GraphNodeType.END, "结束")
                        ),
                        List.of(
                                edge("start_delay", "start", "delay", "default", Map.of()),
                                edge("delay_external", "delay", "external", "default", Map.of()),
                                edge("external_child", "external", "child", "default", Map.of()),
                                edge("child_end", "child", "end", "default", Map.of())
                        ), Map.of()
                )
        );

        var deployment = repositoryService.createDeployment()
                .name("Graph M5 兼容")
                .addBytes("compat-graph-m5.bpmn20.xml", artifact.compiledBpmnXml().getBytes(StandardCharsets.UTF_8))
                .deploy();

        assertThat(repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).count())
                .isEqualTo(1);
    }

    @Test
    void executesGraphConditionListenerAndReachesAuthoredHandleTask() {
        var artifact = new GraphBpmnCompiler().compile(
                "compat_graph_route_runtime",
                "Graph 路由运行兼容",
                routeRuntimeGraph()
        );
        var deployment = repositoryService.createDeployment()
                .name("Graph 路由运行兼容")
                .addBytes("compat-graph-route-runtime.bpmn20.xml", artifact.compiledBpmnXml().getBytes(StandardCharsets.UTF_8))
                .deploy();
        when(bpmRouteDecisionService.evaluateAndRecord(any()))
                .thenReturn(new BpmRouteDecisionResult(1L, List.of("large"), false, 1L));
        doAnswer(invocation -> {
            var execution = invocation.getArgument(0, org.flowable.engine.delegate.DelegateExecution.class);
            execution.setVariable("hunyuan_graph_route_route_split_large", true);
            return null;
        }).when(bpmRouteDecisionService).writeGraphBranchVariables(any(), any(), any());

        var definition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId()).singleResult();
        var instance = runtimeService.startProcessInstanceById(
                definition.getId(), Map.of("hunyuanInstanceId", 901L)
        );

        assertThat(processEngine.getTaskService().createTaskQuery()
                .processInstanceId(instance.getId()).singleResult().getTaskDefinitionKey())
                .isEqualTo("graph_node_large_handle");
    }

    private HunyuanProcessDefinitionGraph routeRuntimeGraph() {
        return new HunyuanProcessDefinitionGraph(
                1,
                "scope_root",
                List.of(new GraphScope("scope_root", null, "主流程")),
                List.of(
                        node("start", GraphNodeType.START, "开始"),
                        gateway("route_split", GraphNodeType.CONDITION, "金额路由", "SPLIT", "route_join"),
                        node("large_handle", GraphNodeType.HANDLE, "大额办理"),
                        node("default_handle", GraphNodeType.HANDLE, "默认办理"),
                        gateway("route_join", GraphNodeType.CONDITION, "金额汇聚", "JOIN", "route_split"),
                        node("end", GraphNodeType.END, "结束")
                ),
                List.of(
                        edge("start_route", "start", "route_split", "default", Map.of()),
                        edge("route_large", "route_split", "large_handle", "large", Map.of("routeCondition", Map.of(
                                "sourceType", "FORM_FIELD", "fieldKey", "amount", "valueType", "NUMBER", "operator", "GT", "compareValue", 5000
                        ))),
                        edge("route_default", "route_split", "default_handle", "default", Map.of()),
                        edge("large_join", "large_handle", "route_join", "default", Map.of()),
                        edge("default_join", "default_handle", "route_join", "default", Map.of()),
                        edge("join_end", "route_join", "end", "default", Map.of())
                ),
                Map.of()
        );
    }

    private HunyuanProcessDefinitionGraph fullM1Graph() {
        return new HunyuanProcessDefinitionGraph(
                1,
                "scope_root",
                List.of(new GraphScope("scope_root", null, "主流程")),
                List.of(
                        node("start", GraphNodeType.START, "开始"),
                        gateway("route_split", GraphNodeType.CONDITION, "金额路由", "SPLIT", "route_join"),
                        node("large_review", GraphNodeType.APPROVAL, "大额审批"),
                        gateway("route_join", GraphNodeType.CONDITION, "金额汇聚", "JOIN", "route_split"),
                        gateway("parallel_split", GraphNodeType.PARALLEL_GATEWAY, "并行分叉", "SPLIT", "parallel_join"),
                        node("finance_copy", GraphNodeType.COPY, "财务抄送"),
                        node("archive_review", GraphNodeType.HANDLE, "归档办理"),
                        gateway("parallel_join", GraphNodeType.PARALLEL_GATEWAY, "并行汇聚", "JOIN", "parallel_split"),
                        node("end", GraphNodeType.END, "结束")
                ),
                List.of(
                        edge("start_route", "start", "route_split", "default", Map.of()),
                        edge("route_large", "route_split", "large_review", "large_amount", Map.of("routeCondition", Map.of(
                                "sourceType", "FORM_FIELD", "fieldKey", "amount", "valueType", "NUMBER", "operator", "GT", "compareValue", 5000
                        ))),
                        edge("large_join", "large_review", "route_join", "default", Map.of()),
                        edge("route_default", "route_split", "route_join", "default", Map.of()),
                        edge("route_join_parallel", "route_join", "parallel_split", "default", Map.of()),
                        edge("parallel_copy", "parallel_split", "finance_copy", "copy_branch", Map.of()),
                        edge("parallel_archive", "parallel_split", "archive_review", "archive_branch", Map.of()),
                        edge("copy_join", "finance_copy", "parallel_join", "default", Map.of()),
                        edge("archive_join", "archive_review", "parallel_join", "default", Map.of()),
                        edge("parallel_end", "parallel_join", "end", "default", Map.of())
                ),
                Map.of()
        );
    }

    private GraphNode node(String nodeId, GraphNodeType type, String name) {
        return new GraphNode(nodeId, "scope_root", type, name, Map.of(), Map.of());
    }

    private GraphNode gateway(String nodeId, GraphNodeType type, String name, String mode, String pairedGatewayId) {
        return new GraphNode(nodeId, "scope_root", type, name, Map.of("gatewayMode", mode, "pairedGatewayId", pairedGatewayId), Map.of());
    }

    private GraphEdge edge(String edgeId, String sourceNodeId, String targetNodeId, String sourcePort, Map<String, Object> properties) {
        return new GraphEdge(edgeId, "scope_root", sourceNodeId, targetNodeId, sourcePort, properties);
    }

    @Test
    void createsRealFlowableTimerJobForV3DelayNode() {
        CompiledDefinitionArtifact artifact = simpleModelBpmnCompiler.compile(
                "compat_v3_delay",
                "v3 延迟计时兼容",
                """
                        {"schemaVersion":3,"nodes":[
                          {"nodeKey":"cooling","name":"冷静期","type":"DELAY","mode":"DURATION","value":"PT1H","timezone":"Asia/Shanghai","overduePolicy":"TRIGGER_IMMEDIATELY"}
                        ]}
                        """,
                "{\"type\":\"ALL\"}",
                "{}"
        );
        repositoryService.createDeployment()
                .name("v3 延迟计时兼容")
                .addBytes("compat-v3-delay.bpmn20.xml", artifact.compiledBpmnXml().getBytes(StandardCharsets.UTF_8))
                .deploy();

        var processInstance = runtimeService.startProcessInstanceByKey(
                "compat_v3_delay",
                Map.of("hunyuanInstanceId", 1L)
        );

        assertThat(processEngine.getManagementService().createTimerJobQuery()
                .processInstanceId(processInstance.getId())
                .count()).isEqualTo(1);
    }

    @Test
    void restoresActiveTimerAfterFlowableEngineRestart() throws Exception {
        String schema = "hunyuan_bpm_restart_" + System.nanoTime();
        String jdbcUrl = "jdbc:mysql://127.0.0.1:3306/" + schema
                + "?createDatabaseIfNotExist=true&nullCatalogMeansCurrent=true"
                + "&useSSL=false&serverTimezone=Asia/Shanghai";
        ProcessEngine first = null;
        ProcessEngine second = null;
        try {
            first = standaloneEngine(jdbcUrl);
            String xml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" targetNamespace="restart">
                      <process id="restart_timer" isExecutable="true">
                        <startEvent id="start"/><intermediateCatchEvent id="wait">
                          <timerEventDefinition><timeDuration>PT2H</timeDuration></timerEventDefinition>
                        </intermediateCatchEvent><endEvent id="end"/>
                        <sequenceFlow id="f1" sourceRef="start" targetRef="wait"/>
                        <sequenceFlow id="f2" sourceRef="wait" targetRef="end"/>
                      </process>
                    </definitions>
                    """;
            first.getRepositoryService().createDeployment()
                    .addString("restart-timer.bpmn20.xml", xml).deploy();
            var instance = first.getRuntimeService().startProcessInstanceByKey("restart_timer");
            String instanceId = instance.getId();
            assertThat(first.getManagementService().createTimerJobQuery()
                    .processInstanceId(instanceId).count()).isEqualTo(1);
            first.close();
            first = null;

            second = standaloneEngine(jdbcUrl);
            assertThat(second.getManagementService().createTimerJobQuery()
                    .processInstanceId(instanceId).count()).isEqualTo(1);
        } finally {
            if (first != null) first.close();
            if (second != null) second.close();
            try (Connection connection = DriverManager.getConnection(MYSQL_ADMIN_JDBC_URL, "root", "root");
                 Statement statement = connection.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS `" + schema + "`");
            }
        }
    }

    private ProcessEngine standaloneEngine(String jdbcUrl) {
        ProcessEngineConfiguration configuration = ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration()
                .setJdbcDriver("com.mysql.cj.jdbc.Driver")
                .setJdbcUrl(jdbcUrl)
                .setJdbcUsername("root")
                .setJdbcPassword("root")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
                .setAsyncExecutorActivate(false);
        ProcessEngineConfigurationImpl configurationImpl = (ProcessEngineConfigurationImpl) configuration;
        configurationImpl.setDisableIdmEngine(true);
        configurationImpl.setDisableEventRegistry(true);
        return configuration.buildProcessEngine();
    }

    @Test
    void createsRealFlowableTimerJobForGraphDelayNode() {
        var artifact = new GraphBpmnCompiler().compile(
                "compat_graph_delay_runtime",
                "Graph 延迟运行兼容",
                linearGraph(List.of(
                        node("start", GraphNodeType.START, "开始"),
                        new GraphNode("delay", "scope_root", GraphNodeType.DELAY, "短暂等待",
                                Map.of("mode", "DURATION", "value", "PT1H"), Map.of()),
                        node("end", GraphNodeType.END, "结束")
                ))
        );
        var deployment = repositoryService.createDeployment()
                .name("Graph 延迟运行兼容")
                .addBytes("compat-graph-delay-runtime.bpmn20.xml", artifact.compiledBpmnXml().getBytes(StandardCharsets.UTF_8))
                .deploy();
        var definition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();

        var instance = runtimeService.startProcessInstanceById(definition.getId(), Map.of("hunyuanInstanceId", 901L));

        assertThat(processEngine.getManagementService().createTimerJobQuery()
                .processInstanceId(instance.getId()).count()).isEqualTo(1);
        verify(bpmTimeEventService).scheduleDelay(901L, instance.getId(),
                runtimeService.createExecutionQuery().processInstanceId(instance.getId()).activityId("graph_node_delay").singleResult().getId(),
                "delay");
    }

    @Test
    void resolvesGraphFormDatetimeIntoARealFlowableTimer() {
        var artifact = new GraphBpmnCompiler().compile(
                "compat_graph_form_delay_runtime", "Graph 表单日期延迟兼容",
                linearGraph(List.of(
                        node("start", GraphNodeType.START, "开始"),
                        new GraphNode("delay", "scope_root", GraphNodeType.DELAY, "等待业务日期",
                                Map.of("mode", "FORM_DATETIME", "value", "resumeAt", "timezone", "Asia/Shanghai"), Map.of()),
                        node("end", GraphNodeType.END, "结束")
                )));
        var deployment = repositoryService.createDeployment().name("Graph 表单日期延迟兼容")
                .addBytes("compat-graph-form-delay-runtime.bpmn20.xml",
                        artifact.compiledBpmnXml().getBytes(StandardCharsets.UTF_8)).deploy();
        var definition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        when(bpmTimeEventService.scheduleDelay(any(), any(), any(), org.mockito.ArgumentMatchers.eq("delay")))
                .thenReturn(java.time.LocalDateTime.now().plusHours(2));

        var instance = runtimeService.startProcessInstanceById(definition.getId(), Map.of("hunyuanInstanceId", 906L));

        assertThat(processEngine.getManagementService().createTimerJobQuery()
                .processInstanceId(instance.getId()).count()).isEqualTo(1);
    }

    @Test
    void invokesControlledConnectorAndWaitsOnRealGraphReceiveTask() {
        var artifact = new GraphBpmnCompiler().compile(
                "compat_graph_external_runtime",
                "Graph 外部等待运行兼容",
                linearGraph(List.of(
                        node("start", GraphNodeType.START, "开始"),
                        new GraphNode("external", "scope_root", GraphNodeType.EXTERNAL_TRIGGER, "外部等待",
                                Map.of("connectorKey", "finance", "connectorVersion", 1,
                                        "operationKey", "createExpense", "waitMode", "WAIT_CALLBACK",
                                        "timeoutPolicy", Map.of("timeoutAfter", "PT5M")), Map.of()),
                        node("end", GraphNodeType.END, "结束")
                ))
        );
        var deployment = repositoryService.createDeployment()
                .name("Graph 外部等待运行兼容")
                .addBytes("compat-graph-external-runtime.bpmn20.xml", artifact.compiledBpmnXml().getBytes(StandardCharsets.UTF_8))
                .deploy();
        var definition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        BpmInstanceEntity platformInstance = new BpmInstanceEntity();
        platformInstance.setInstanceId(902L);
        platformInstance.setDefinitionSource("GRAPH");
        platformInstance.setGraphDefinitionVersionId(92L);
        platformInstance.setCurrentFormDataSnapshotJson("{}");
        when(bpmInstanceDao.selectById(902L)).thenReturn(platformInstance);
        when(bpmGraphRuntimeMetadataService.requireNode(92L, "external")).thenReturn(
                new BpmGraphRuntimeMetadataService.GraphNodeMetadata(
                        "external", "scope_root", "外部等待", GraphNodeType.EXTERNAL_TRIGGER,
                        com.alibaba.fastjson.JSONObject.parseObject("""
                                {"connectorVersion":1,"requestMapping":{},"responseMapping":{},
                                 "timeoutPolicy":{"timeoutAfter":"PT5M"}}
                                """)));
        when(bpmExternalWaitService.prepareWait(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new BpmExternalWaitService.PreparedWait("callback-token", "902:external:1", 1));
        when(bpmConnectorInvocationService.invokePersistent(any(), any(), any(), any(), any(), any()))
                .thenReturn(new com.alibaba.fastjson.JSONObject());

        var instance = runtimeService.startProcessInstanceById(definition.getId(), Map.of("hunyuanInstanceId", 902L));

        var waiting = runtimeService.createExecutionQuery().processInstanceId(instance.getId())
                .activityId("graph_node_external_wait").singleResult();
        assertThat(waiting).isNotNull();
        assertThat(processEngine.getManagementService().createTimerJobQuery()
                .processInstanceId(instance.getId()).count()).isEqualTo(1);
        verify(bpmExternalWaitService).bindExecution(instance.getId(), "external", waiting.getId());
        runtimeService.trigger(waiting.getId());
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(instance.getId()).count()).isZero();
        assertThat(processEngine.getManagementService().createTimerJobQuery()
                .processInstanceId(instance.getId()).count()).isZero();
    }

    @Test
    void executesPersistedGraphExternalTimeoutAndEndsTheWaitingPath() {
        var artifact = new GraphBpmnCompiler().compile(
                "compat_graph_external_timeout_runtime",
                "Graph 外部等待超时兼容",
                linearGraph(List.of(
                        node("start", GraphNodeType.START, "开始"),
                        new GraphNode("external", "scope_root", GraphNodeType.EXTERNAL_TRIGGER, "外部等待",
                                Map.of("connectorKey", "finance", "connectorVersion", 1,
                                        "operationKey", "createExpense", "waitMode", "WAIT_CALLBACK",
                                        "timeoutPolicy", Map.of("timeoutAfter", "PT5M")), Map.of()),
                        node("end", GraphNodeType.END, "结束")
                ))
        );
        var deployment = repositoryService.createDeployment().name("Graph 外部等待超时兼容")
                .addBytes("compat-graph-external-timeout-runtime.bpmn20.xml",
                        artifact.compiledBpmnXml().getBytes(StandardCharsets.UTF_8)).deploy();
        var definition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        BpmInstanceEntity platformInstance = new BpmInstanceEntity();
        platformInstance.setInstanceId(905L);
        platformInstance.setDefinitionSource("GRAPH");
        platformInstance.setGraphDefinitionVersionId(95L);
        platformInstance.setCurrentFormDataSnapshotJson("{}");
        when(bpmInstanceDao.selectById(905L)).thenReturn(platformInstance);
        when(bpmGraphRuntimeMetadataService.requireNode(95L, "external")).thenReturn(
                new BpmGraphRuntimeMetadataService.GraphNodeMetadata(
                        "external", "scope_root", "外部等待", GraphNodeType.EXTERNAL_TRIGGER,
                        com.alibaba.fastjson.JSONObject.parseObject("""
                                {"connectorVersion":1,"requestMapping":{},"responseMapping":{},
                                 "timeoutPolicy":{"timeoutAfter":"PT5M"}}
                                """)));
        when(bpmExternalWaitService.prepareWait(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new BpmExternalWaitService.PreparedWait("callback-token", "905:external:1", 1));
        when(bpmConnectorInvocationService.invokePersistent(any(), any(), any(), any(), any(), any()))
                .thenReturn(new com.alibaba.fastjson.JSONObject());

        var instance = runtimeService.startProcessInstanceById(definition.getId(), Map.of("hunyuanInstanceId", 905L));
        var timer = processEngine.getManagementService().createTimerJobQuery()
                .processInstanceId(instance.getId()).singleResult();

        assertThat(timer).isNotNull();
        processEngine.getManagementService().moveTimerToExecutableJob(timer.getId());
        var executable = processEngine.getManagementService().createJobQuery()
                .processInstanceId(instance.getId()).singleResult();
        processEngine.getManagementService().executeJob(executable.getId());

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(instance.getId()).count()).isZero();
        verify(bpmTimeEventService).trigger(any(), org.mockito.ArgumentMatchers.eq("external"),
                org.mockito.ArgumentMatchers.eq("EXTERNAL_TIMEOUT"), any(), any());
    }

    @Test
    void startsWaitsForAndCompletesFrozenGraphSubProcess() {
        String childXml = new GraphBpmnCompiler().compile(
                "compat_graph_child_runtime", "Graph 子流程",
                linearGraph(List.of(
                        node("start", GraphNodeType.START, "开始"),
                        node("child_task", GraphNodeType.HANDLE, "子流程办理"),
                        node("end", GraphNodeType.END, "结束")
                ))).compiledBpmnXml();
        var childDeployment = repositoryService.createDeployment().name("Graph 子流程")
                .addBytes("compat-graph-child-runtime.bpmn20.xml", childXml.getBytes(StandardCharsets.UTF_8)).deploy();
        var childDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(childDeployment.getId()).singleResult();
        var parentArtifact = new GraphBpmnCompiler().compile(
                "compat_graph_parent_runtime", "Graph 父流程",
                linearGraph(List.of(
                        node("start", GraphNodeType.START, "开始"),
                        new GraphNode("child", "scope_root", GraphNodeType.SUB_PROCESS, "调用子流程",
                                Map.of("calledProcessKey", "compat_graph_child_runtime", "calledDefinitionVersionId", 1,
                                        "calledEngineProcessDefinitionId", childDefinition.getId(),
                                        "failurePolicy", "PAUSE_PARENT", "cancelPropagation", "CANCEL_CHILD"), Map.of()),
                        node("end", GraphNodeType.END, "结束")
                )));
        var parentDeployment = repositoryService.createDeployment().name("Graph 父流程")
                .addBytes("compat-graph-parent-runtime.bpmn20.xml", parentArtifact.compiledBpmnXml().getBytes(StandardCharsets.UTF_8)).deploy();
        var parentDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(parentDeployment.getId()).singleResult();
        when(bpmSubProcessService.prepareChild(any(), any(), any(), any()))
                .thenReturn(new BpmSubProcessService.PreparedSubProcess(71L, 904L));

        var parent = runtimeService.startProcessInstanceById(parentDefinition.getId(), Map.of("hunyuanInstanceId", 903L));
        var child = runtimeService.createProcessInstanceQuery().superProcessInstanceId(parent.getId()).singleResult();

        assertThat(child).isNotNull();
        assertThat(processEngine.getTaskService().createTaskQuery().processInstanceId(child.getId()).count()).isEqualTo(1);
        assertThat(runtimeService.getVariable(child.getId(), "hunyuanInstanceId")).isEqualTo(904L);
        assertThat(runtimeService.getVariable(child.getId(), "hunyuanParentInstanceId")).isEqualTo(903L);
        verify(bpmSubProcessService).bindChildEngineInstance(903L, 904L, child.getId());
        processEngine.getTaskService().complete(
                processEngine.getTaskService().createTaskQuery().processInstanceId(child.getId()).singleResult().getId());
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(child.getId()).count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(parent.getId()).count()).isZero();
        verify(bpmSubProcessService).prepareChild(any(), any(), any(), any());
        verify(bpmSubProcessService).complete(any(), any(), any());
    }

    private HunyuanProcessDefinitionGraph linearGraph(List<GraphNode> nodes) {
        java.util.ArrayList<GraphEdge> edges = new java.util.ArrayList<>();
        for (int index = 0; index < nodes.size() - 1; index++) {
            edges.add(edge("linear_" + index, nodes.get(index).nodeId(), nodes.get(index + 1).nodeId(), "default", Map.of()));
        }
        return new HunyuanProcessDefinitionGraph(1, "scope_root",
                List.of(new GraphScope("scope_root", null, "主流程")), nodes, edges, Map.of());
    }

    @Test
    void evaluatesMissingRouteVariableAsFalseInsideFlowableKernel() {
        String bpmnXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             targetNamespace="http://hunyuan.sa/bpm">
                  <process id="compat_safe_route" name="缺失路由变量兼容" isExecutable="true">
                    <startEvent id="start"/>
                    <exclusiveGateway id="route" default="flow_default"/>
                    <endEvent id="matched_end"/>
                    <endEvent id="default_end"/>
                    <sequenceFlow id="flow_start" sourceRef="start" targetRef="route"/>
                    <sequenceFlow id="flow_matched" sourceRef="route" targetRef="matched_end">
                      <conditionExpression xsi:type="tFormalExpression"><![CDATA[${execution.getVariable('route_missing') == true}]]></conditionExpression>
                    </sequenceFlow>
                    <sequenceFlow id="flow_default" sourceRef="route" targetRef="default_end"/>
                  </process>
                </definitions>
                """;
        repositoryService.createDeployment()
                .name("缺失路由变量兼容")
                .addBytes("compat-safe-route.bpmn20.xml", bpmnXml.getBytes(StandardCharsets.UTF_8))
                .deploy();

        var processInstance = runtimeService.startProcessInstanceByKey("compat_safe_route");

        assertThat(runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .count()).isZero();
    }

    @AfterAll
    static void dropTemporaryTestSchema() throws SQLException {
        try (
                Connection connection = DriverManager.getConnection(MYSQL_ADMIN_JDBC_URL, "root", "root");
                Statement statement = connection.createStatement()
        ) {
            statement.execute("DROP DATABASE IF EXISTS `" + TEST_SCHEMA_NAME + "`");
        }
    }
}
