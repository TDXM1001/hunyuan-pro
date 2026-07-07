package com.hunyuan.sa.bpm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * BPM 内部 Flowable 内核配置。
 */
@ConfigurationProperties(prefix = "bpm.flowable")
public class BpmFlowableProperties {

    /**
     * 是否启用 Flowable 内核。
     */
    private boolean enabled = false;

    /**
     * 是否自动建表。
     */
    private String databaseSchemaUpdate = "false";

    /**
     * 是否启用异步执行器。
     */
    private boolean asyncExecutorActivate = false;

    /**
     * 历史级别。
     */
    private String historyLevel = "audit";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDatabaseSchemaUpdate() {
        return databaseSchemaUpdate;
    }

    public void setDatabaseSchemaUpdate(String databaseSchemaUpdate) {
        this.databaseSchemaUpdate = databaseSchemaUpdate;
    }

    public boolean isAsyncExecutorActivate() {
        return asyncExecutorActivate;
    }

    public void setAsyncExecutorActivate(boolean asyncExecutorActivate) {
        this.asyncExecutorActivate = asyncExecutorActivate;
    }

    public String getHistoryLevel() {
        return historyLevel;
    }

    public void setHistoryLevel(String historyLevel) {
        this.historyLevel = historyLevel;
    }
}
