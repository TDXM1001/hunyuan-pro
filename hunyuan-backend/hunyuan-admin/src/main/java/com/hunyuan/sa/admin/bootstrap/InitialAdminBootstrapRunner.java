package com.hunyuan.sa.admin.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InitialAdminBootstrapRunner implements ApplicationRunner {

    private final Environment environment;
    private final InitialAdminBootstrapService bootstrapService;

    public InitialAdminBootstrapRunner(
            Environment environment,
            InitialAdminBootstrapService bootstrapService) {
        this.environment = environment;
        this.bootstrapService = bootstrapService;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean enabled = environment.getProperty(
                "hunyuan.bootstrap.admin.enabled", Boolean.class, Boolean.FALSE);
        if (!enabled) {
            log.debug("Initial administrator bootstrap is disabled");
            return;
        }

        String loginName = environment.getProperty("hunyuan.bootstrap.admin.login-name", "admin");
        String password = environment.getProperty("hunyuan.bootstrap.admin.password");
        String actualName = environment.getProperty("hunyuan.bootstrap.admin.actual-name", "系统管理员");
        Long departmentId = environment.getProperty("hunyuan.bootstrap.admin.department-id", Long.class);

        bootstrapService.bootstrap(new InitialAdminBootstrapService.BootstrapCommand(
                loginName,
                password,
                actualName,
                departmentId));
    }
}
