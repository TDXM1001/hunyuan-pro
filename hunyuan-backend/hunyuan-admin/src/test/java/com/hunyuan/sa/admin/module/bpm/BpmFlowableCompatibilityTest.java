package com.hunyuan.sa.admin.module.bpm;

import com.hunyuan.sa.admin.module.bpm.adapter.AdminBpmCurrentActorProvider;
import com.hunyuan.sa.admin.module.bpm.adapter.AdminBpmOrgIdentityGateway;
import com.hunyuan.sa.admin.module.system.department.service.DepartmentService;
import com.hunyuan.sa.admin.module.system.employee.service.EmployeeService;
import com.hunyuan.sa.admin.module.system.login.manager.LoginManager;
import com.hunyuan.sa.admin.module.system.role.service.RoleEmployeeService;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.config.BpmFlowableAutoConfiguration;
import org.flowable.engine.ProcessEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

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
    private LoginManager loginManager;

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

    @Test
    void loadsFlowableAsHiddenKernelInsideBpmModule() {
        assertThat(processEngine).isNotNull();
        assertThat(bpmOrgIdentityGateway).isNotNull();
        assertThat(bpmCurrentActorProvider).isNotNull();
    }
}
