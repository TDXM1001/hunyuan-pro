package com.hunyuan.sa.bpm.config;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.persistence.StrongUuidGenerator;
import org.flowable.engine.DynamicBpmnService;
import org.flowable.engine.FormService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.ProcessEngineFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 将 Flowable 作为 BPM 模块内部隐藏内核接入。
 */
@Configuration
@EnableConfigurationProperties(BpmFlowableProperties.class)
@ConditionalOnProperty(prefix = "bpm.flowable", name = "enabled", havingValue = "true")
public class BpmFlowableAutoConfiguration {

    /**
     * 手动装配 Flowable 配置，并把引擎建表顺序收口在 BPM 模块内部。
     */
    @Bean
    public SpringProcessEngineConfiguration processEngineConfiguration(
            DataSource dataSource,
            PlatformTransactionManager transactionManager,
            ApplicationContext applicationContext,
            BpmFlowableProperties properties
    ) {
        BpmFlowableSchemaBootstrapper schemaBootstrapper = new BpmFlowableSchemaBootstrapper();
        schemaBootstrapper.bootstrapIfNecessary(dataSource, properties);

        SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();
        configuration.setApplicationContext(applicationContext);
        configuration.setDataSource(dataSource);
        configuration.setTransactionManager(transactionManager);
        configuration.setEngineName("hunyuan-bpm");
        configuration.setDatabaseSchemaUpdate(BpmFlowableSchemaBootstrapper.SCHEMA_UPDATE_FALSE);
        configuration.setAsyncExecutorActivate(properties.isAsyncExecutorActivate());
        configuration.setAsyncHistoryExecutorActivate(false);
        configuration.setHistoryLevel(HistoryLevel.getHistoryLevelForKey(properties.getHistoryLevel()));
        configuration.setDisableIdmEngine(true);
        configuration.setDisableEventRegistry(true);
        configuration.setDeploymentResources(new org.springframework.core.io.Resource[0]);
        configuration.setDeploymentStrategies(Collections.emptyList());
        configuration.setIdGenerator(new StrongUuidGenerator());
        return configuration;
    }

    /**
     * 以标准 FactoryBean 方式暴露 ProcessEngine，保持后续扩展点与 Flowable 约定一致。
     */
    @Bean(name = "processEngine")
    public ProcessEngineFactoryBean processEngineFactoryBean(SpringProcessEngineConfiguration configuration) {
        ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
        factoryBean.setProcessEngineConfiguration(configuration);
        return factoryBean;
    }

    /**
     * 暴露仓储服务，供后续模型、定义发布等模块复用。
     */
    @Bean
    public RepositoryService repositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    /**
     * 暴露运行时服务，供后续实例启动与流程驱动模块复用。
     */
    @Bean
    public RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    /**
     * 暴露任务服务，供后续待办、审批与转办模块复用。
     */
    @Bean
    public TaskService taskService(ProcessEngine processEngine) {
        return processEngine.getTaskService();
    }

    /**
     * 暴露历史服务，供后续流程轨迹和归档查询模块复用。
     */
    @Bean
    public HistoryService historyService(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }

    /**
     * 暴露表单服务，供后续启动表单和任务表单读取模块复用。
     */
    @Bean
    public FormService formService(ProcessEngine processEngine) {
        return processEngine.getFormService();
    }

    /**
     * 暴露管理服务，供后续作业、锁与健康检查模块复用。
     */
    @Bean
    public ManagementService managementService(ProcessEngine processEngine) {
        return processEngine.getManagementService();
    }

    /**
     * 暴露动态 BPMN 服务，供后续模型编译结果补丁与动态节点能力复用。
     */
    @Bean
    public DynamicBpmnService dynamicBpmnService(ProcessEngine processEngine) {
        return processEngine.getDynamicBpmnService();
    }
}

/**
 * 在 BPM 模块内部显式控制 Flowable 官方 SQL 的执行顺序，
 * 避免 7.2.0 在空库 create-drop 时被内部 schema manager 顺序 bug 卡住。
 */
final class BpmFlowableSchemaBootstrapper {

    static final String SCHEMA_UPDATE_FALSE = "false";

    private static final String SCHEMA_UPDATE_TRUE = "true";

    private static final String SCHEMA_UPDATE_CREATE = "create";

    private static final String SCHEMA_UPDATE_CREATE_DROP = "create-drop";

    private static final String FLOWABLE_PROPERTY_TABLE = "ACT_GE_PROPERTY";

    private static final String FLOWABLE_ENGINE_TABLE = "ACT_RE_PROCDEF";

    private static final String FLOWABLE_HISTORY_TABLE = "ACT_HI_PROCINST";

    /**
     * 按配置决定是否在引擎启动前准备官方 SQL。
     */
    void bootstrapIfNecessary(DataSource dataSource, BpmFlowableProperties properties) {
        String schemaUpdateMode = normalizeSchemaUpdateMode(properties.getDatabaseSchemaUpdate());
        if (SCHEMA_UPDATE_FALSE.equals(schemaUpdateMode)) {
            return;
        }

        boolean historyEnabled = isHistoryEnabled(properties);
        String databaseType = resolveDatabaseType(dataSource);

        if (SCHEMA_UPDATE_CREATE_DROP.equals(schemaUpdateMode)) {
            executeScripts(dataSource, buildDropScripts(databaseType), true);
        }

        try (Connection connection = dataSource.getConnection()) {
            if (isSchemaReady(connection, historyEnabled)) {
                return;
            }

            if (hasPartialSchema(connection, historyEnabled)) {
                throw new IllegalStateException("检测到不完整的 Flowable 内核表，当前 BPM 底座不会在运行时尝试修复，请先清理残留表或改走正式迁移脚本。");
            }

            if (SCHEMA_UPDATE_TRUE.equals(schemaUpdateMode)
                    || SCHEMA_UPDATE_CREATE.equals(schemaUpdateMode)
                    || SCHEMA_UPDATE_CREATE_DROP.equals(schemaUpdateMode)) {
                executeScripts(dataSource, buildCreateScripts(databaseType, historyEnabled), false);
                return;
            }

            throw new IllegalStateException("暂不支持的 bpm.flowable.database-schema-update 配置：" + schemaUpdateMode);
        } catch (SQLException ex) {
            throw new IllegalStateException("准备 Flowable 隐藏内核表结构失败", ex);
        }
    }

    /**
     * 统一规范 schema-update 配置值，避免大小写和空白差异影响行为。
     */
    private String normalizeSchemaUpdateMode(String databaseSchemaUpdate) {
        if (!StringUtils.hasText(databaseSchemaUpdate)) {
            return SCHEMA_UPDATE_FALSE;
        }
        return databaseSchemaUpdate.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 仅对当前项目首发目标明确支持 MySQL / MariaDB。
     */
    private String resolveDatabaseType(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            String normalizedProductName = productName == null ? "" : productName.toLowerCase(Locale.ROOT);
            if (normalizedProductName.contains("mysql") || normalizedProductName.contains("mariadb")) {
                return "mysql";
            }
            throw new IllegalStateException("当前 BPM 隐藏内核首发仅支持 MySQL 系数据库，实际数据库为：" + productName);
        } catch (SQLException ex) {
            throw new IllegalStateException("识别 Flowable 隐藏内核数据库类型失败", ex);
        }
    }

    /**
     * Flowable history 关闭时无需准备历史表，避免引入无效对象。
     */
    private boolean isHistoryEnabled(BpmFlowableProperties properties) {
        return !"none".equalsIgnoreCase(properties.getHistoryLevel());
    }

    /**
     * 内核可用的最低标准是公共表、引擎表以及 schema.version 已经齐备。
     */
    private boolean isSchemaReady(Connection connection, boolean historyEnabled) throws SQLException {
        if (!hasTable(connection, FLOWABLE_PROPERTY_TABLE)
                || !hasTable(connection, FLOWABLE_ENGINE_TABLE)
                || !hasProperty(connection, "schema.version")) {
            return false;
        }
        return !historyEnabled || hasTable(connection, FLOWABLE_HISTORY_TABLE);
    }

    /**
     * 如果只建了一半，直接显式失败，避免在运行时做带风险的“猜测性修复”。
     */
    private boolean hasPartialSchema(Connection connection, boolean historyEnabled) throws SQLException {
        boolean propertyTableExists = hasTable(connection, FLOWABLE_PROPERTY_TABLE);
        boolean engineTableExists = hasTable(connection, FLOWABLE_ENGINE_TABLE);
        boolean historyTableExists = historyEnabled && hasTable(connection, FLOWABLE_HISTORY_TABLE);
        if (!propertyTableExists && !engineTableExists && !historyTableExists) {
            return false;
        }
        return !isSchemaReady(connection, historyEnabled);
    }

    /**
     * 通过 JDBC 元数据探测表是否存在，兼容不同大小写返回风格。
     */
    private boolean hasTable(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = connection.getCatalog();
        String schema = connection.getSchema();
        return hasTable(metaData, catalog, schema, tableName)
                || hasTable(metaData, catalog, schema, tableName.toUpperCase(Locale.ROOT))
                || hasTable(metaData, catalog, schema, tableName.toLowerCase(Locale.ROOT));
    }

    /**
     * 检查 Flowable 属性表中的关键版本键是否已经落库。
     */
    private boolean hasProperty(Connection connection, String propertyName) throws SQLException {
        if (!hasTable(connection, FLOWABLE_PROPERTY_TABLE)) {
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_ = ?"
        )) {
            statement.setString(1, propertyName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && StringUtils.hasText(resultSet.getString(1));
            }
        }
    }

    /**
     * 执行 Flowable 官方 SQL 资源，并统一收口 UTF-8、注释前缀和错误策略。
     */
    private void executeScripts(DataSource dataSource, List<ClassPathResource> scripts, boolean continueOnError) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setSqlScriptEncoding("UTF-8");
        populator.setContinueOnError(continueOnError);
        populator.setCommentPrefixes("--", "#");
        for (ClassPathResource script : scripts) {
            populator.addScript(script);
        }
        DatabasePopulatorUtils.execute(populator, dataSource);
    }

    /**
     * 创建脚本按 common -> engine -> history 顺序执行，确保引用关系稳定。
     */
    private List<ClassPathResource> buildCreateScripts(String databaseType, boolean historyEnabled) {
        List<ClassPathResource> scripts = new ArrayList<>();
        scripts.add(new ClassPathResource("org/flowable/common/db/create/flowable." + databaseType + ".create.common.sql"));
        scripts.add(new ClassPathResource("org/flowable/db/create/flowable." + databaseType + ".create.engine.sql"));
        if (historyEnabled) {
            scripts.add(new ClassPathResource("org/flowable/db/create/flowable." + databaseType + ".create.history.sql"));
        }
        return scripts;
    }

    /**
     * 清理脚本按 history -> engine -> common 逆序执行，减少外键残留干扰。
     */
    private List<ClassPathResource> buildDropScripts(String databaseType) {
        List<ClassPathResource> scripts = new ArrayList<>();
        scripts.add(new ClassPathResource("org/flowable/db/drop/flowable." + databaseType + ".drop.history.sql"));
        scripts.add(new ClassPathResource("org/flowable/db/drop/flowable." + databaseType + ".drop.engine.sql"));
        scripts.add(new ClassPathResource("org/flowable/common/db/drop/flowable." + databaseType + ".drop.common.sql"));
        return scripts;
    }

    /**
     * 单点封装元数据判断，保持上层逻辑更可读。
     */
    private boolean hasTable(DatabaseMetaData metaData, String catalog, String schema, String tableName) throws SQLException {
        try (ResultSet resultSet = metaData.getTables(catalog, schema, tableName, new String[]{"TABLE"})) {
            return resultSet.next();
        }
    }
}
