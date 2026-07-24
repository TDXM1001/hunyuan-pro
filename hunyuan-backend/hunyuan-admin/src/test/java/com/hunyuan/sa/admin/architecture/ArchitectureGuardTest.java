package com.hunyuan.sa.admin.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.hunyuan.sa", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureGuardTest {

    @ArchTest
    static final ArchRule BASE_MUST_NOT_DEPEND_ON_ADMIN = noClasses()
            .that().resideInAPackage("com.hunyuan.sa.base..")
            .should().dependOnClassesThat().resideInAPackage("com.hunyuan.sa.admin..");

    @ArchTest
    static final ArchRule DOMAIN_MUST_NOT_DEPEND_ON_WEB_CONTROLLERS = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..controller..", "org.springframework.web..");

    @ArchTest
    static final ArchRule ADMIN_MODULE_CYCLES_MUST_NOT_GROW = freeze(slices()
            .matching("com.hunyuan.sa.admin.module.(*)..")
            .should().beFreeOfCycles());

    @ArchTest
    static final ArchRule SUPPORT_MODULE_CYCLES_MUST_NOT_GROW = freeze(slices()
            .matching("com.hunyuan.sa.base.module.support.(*)..")
            .should().beFreeOfCycles());

    @ArchTest
    static final ArchRule CROSS_MODULE_PERSISTENCE_ACCESS_MUST_NOT_GROW = freeze(noClasses()
            .that().resideInAPackage("com.hunyuan.sa.admin.module..")
            .should(notAccessAnotherModulePersistenceInternals()));

    /**
     * 基础工程中的 HTTP 入口属于待归位遗留项，只允许逐步减少，禁止继续新增。
     */
    @ArchTest
    static final ArchRule BASE_HTTP_ROUTES_MUST_NOT_GROW = freeze(noClasses()
            .that().resideInAPackage("com.hunyuan.sa.base..")
            .should().beAnnotatedWith("org.springframework.web.bind.annotation.RestController"));

    /**
     * 平台支撑模块不能新增对其他 owner 持久化内部实现的直接访问。
     */
    @ArchTest
    static final ArchRule SUPPORT_CROSS_MODULE_PERSISTENCE_ACCESS_MUST_NOT_GROW = freeze(noClasses()
            .that().resideInAPackage("com.hunyuan.sa.base.module.support..")
            .should(notAccessAnotherSupportModulePersistenceInternals()));

    /**
     * 公开 Facade 的方法签名不能继续暴露 Entity、DAO、Mapper 或历史 Form/VO。
     */
    @ArchTest
    static final ArchRule PUBLIC_FACADE_MODEL_LEAKS_MUST_NOT_GROW = freeze(classes()
            .that().haveSimpleNameEndingWith("Facade")
            .and().resideInAnyPackage(
                    "com.hunyuan.sa.admin.module..",
                    "com.hunyuan.sa.base.module.support..")
            .should(notExposeInternalModels()));

    @ArchTest
    static final ArchRule LEGACY_EMPLOYEE_DEPENDENCIES_MUST_NOT_GROW = freeze(noClasses()
            .that().resideOutsideOfPackage("com.hunyuan.sa.admin.module.system.employee..")
            .should().dependOnClassesThat().resideInAPackage(
                    "com.hunyuan.sa.admin.module.system.employee.."));

    @ArchTest
    static final ArchRule ORGANIZATION_DOMAIN_MUST_NOT_DEPEND_ON_FRAMEWORKS = noClasses()
            .that().resideInAPackage("com.hunyuan.sa.admin.module.organization..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "org.apache.ibatis..", "com.baomidou.mybatisplus..", "..controller..");

    @ArchTest
    static final ArchRule ORGANIZATION_MUST_NOT_DEPEND_ON_LEGACY_PERSISTENCE = noClasses()
            .that().resideInAPackage("com.hunyuan.sa.admin.module.organization..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.hunyuan.sa.admin.module.system..dao..",
                    "com.hunyuan.sa.admin.module.system..domain.entity..");

    @ArchTest
    static final ArchRule LOGIN_MUST_USE_ACCESS_AUTHORIZATION_API = noClasses()
            .that().resideInAPackage("com.hunyuan.sa.admin.module.system.login..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.hunyuan.sa.admin.module.system.role.service..",
                    "com.hunyuan.sa.admin.module.system.role.dao..");

    /**
     * 登录认证只能通过身份员工公开接口读取账号，禁止回退到旧员工内部实现。
     */
    @ArchTest
    static final ArchRule LOGIN_MUST_USE_IDENTITY_EMPLOYEE_API = noClasses()
            .that().resideInAPackage("com.hunyuan.sa.admin.module.system.login..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.hunyuan.sa.admin.module.system.employee.service..",
                    "com.hunyuan.sa.admin.module.system.employee.dao..",
                    "com.hunyuan.sa.admin.module.system.employee.domain.entity..");

    /**
     * 登录模块只能通过平台邮件公开边界发送验证码，不能依赖底层邮件服务。
     */
    @ArchTest
    static final ArchRule LOGIN_MUST_USE_PLATFORM_MAIL_API = noClasses()
            .that().resideInAPackage("com.hunyuan.sa.admin.module.system.login..")
            .should().dependOnClassesThat().haveFullyQualifiedName(
                    "com.hunyuan.sa.base.module.support.mail.MailService");

    /**
     * 身份和登录模块只能通过平台文件公开接口解析头像，不能依赖具体存储实现。
     */
    @ArchTest
    static final ArchRule IDENTITY_AND_LOGIN_MUST_USE_PLATFORM_FILE_API = noClasses()
            .that().resideInAnyPackage(
                    "com.hunyuan.sa.admin.module.identity..",
                    "com.hunyuan.sa.admin.module.system.login..")
            .should().dependOnClassesThat().haveFullyQualifiedName(
                    "com.hunyuan.sa.base.module.support.file.service.IFileStorageService");

    /**
     * 管理端消息控制器只能通过平台消息公开边界访问消息能力。
     */
    @ArchTest
    static final ArchRule ADMIN_MESSAGE_CONTROLLERS_MUST_USE_PLATFORM_MESSAGE_API = noClasses()
            .that().resideInAPackage("com.hunyuan.sa.admin.module.system.message..")
            .should().dependOnClassesThat().haveFullyQualifiedName(
                    "com.hunyuan.sa.base.module.support.message.service.MessageService");

    /**
     * 历史个人消息控制器只能通过消息箱公开边界访问消息能力。
     */
    @ArchTest
    static final ArchRule LEGACY_MESSAGE_CONTROLLER_MUST_USE_INBOX_API = noClasses()
            .that().haveFullyQualifiedName(
                    "com.hunyuan.sa.base.module.support.message.controller.MessageController")
            .should().dependOnClassesThat().haveFullyQualifiedName(
                    "com.hunyuan.sa.base.module.support.message.service.MessageService");

    /**
     * 历史审计日志控制器只能通过平台审计公开边界查询日志。
     */
    @ArchTest
    static final ArchRule OPERATE_LOG_CONTROLLER_MUST_USE_PLATFORM_AUDIT_API = noClasses()
            .that().haveSimpleName("AdminOperateLogController")
            .should().dependOnClassesThat().haveFullyQualifiedName(
                    "com.hunyuan.sa.base.module.support.operatelog.OperateLogService");

    @ArchTest
    static final ArchRule LOGIN_LOG_CONTROLLER_MUST_USE_PLATFORM_AUDIT_API = noClasses()
            .that().haveSimpleName("AdminLoginLogController")
            .should().dependOnClassesThat().haveFullyQualifiedName(
                    "com.hunyuan.sa.base.module.support.loginlog.LoginLogService");

    /**
     * 历史短信管理控制器只能通过平台短信公开边界访问短信能力。
     */
    @ArchTest
    static final ArchRule SMS_CONTROLLER_MUST_USE_PLATFORM_SMS_API = noClasses()
            .that().haveSimpleName("AdminSmsController")
            .should().dependOnClassesThat().haveFullyQualifiedName(
                    "com.hunyuan.sa.base.module.support.sms.service.SmsService");

    /**
     * 历史序列号控制器只能通过平台运行时公开边界访问序列号能力。
     */
    @ArchTest
    static final ArchRule SERIAL_NUMBER_CONTROLLER_MUST_USE_PLATFORM_RUNTIME_API = noClasses()
            .that().haveSimpleName("AdminSerialNumberController")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.hunyuan.sa.base.module.support.serialnumber.dao..",
                    "com.hunyuan.sa.base.module.support.serialnumber.service..");

    /**
     * 历史定时任务控制器只能通过平台运行时公开边界访问任务能力。
     */
    @ArchTest
    static final ArchRule JOB_CONTROLLER_MUST_USE_PLATFORM_RUNTIME_API = noClasses()
            .that().haveSimpleName("AdminSmartJobController")
            .should().dependOnClassesThat().haveFullyQualifiedName(
                    "com.hunyuan.sa.base.module.support.job.api.SmartJobService");

    /**
     * 历史代码生成器控制器只能通过开发工具公开边界访问生成能力。
     */
    @ArchTest
    static final ArchRule CODE_GENERATOR_CONTROLLER_MUST_USE_DEVTOOLS_API = noClasses()
            .that().haveFullyQualifiedName(
                    "com.hunyuan.sa.base.module.support.codegenerator.controller.CodeGeneratorController")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.hunyuan.sa.base.module.support.codegenerator.service..",
                    "com.hunyuan.sa.base.module.support.codegenerator.dao..");

    /**
     * 历史重载控制器只能通过平台运行时公开边界访问重载能力。
     */
    @ArchTest
    static final ArchRule RELOAD_CONTROLLER_MUST_USE_PLATFORM_RUNTIME_API = noClasses()
            .that().haveSimpleName("AdminReloadController")
            .should().dependOnClassesThat().haveFullyQualifiedName(
                    "com.hunyuan.sa.base.module.support.reload.ReloadService");

    /**
     * 历史心跳控制器只能通过平台运行时公开边界查询心跳记录。
     */
    @ArchTest
    static final ArchRule HEARTBEAT_CONTROLLER_MUST_USE_PLATFORM_RUNTIME_API = noClasses()
            .that().haveSimpleName("AdminHeartBeatController")
            .should().dependOnClassesThat().haveFullyQualifiedName(
                    "com.hunyuan.sa.base.module.support.heartbeat.HeartBeatService");

    @ArchTest
    static final ArchRule IDENTITY_MUST_USE_ACCESS_ROLE_API = noClasses()
            .that().resideInAPackage("com.hunyuan.sa.admin.module.identity..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.hunyuan.sa.admin.module.system.role.service..",
                    "com.hunyuan.sa.admin.module.system.role.dao..");

    @ArchTest
    static final ArchRule ROLE_EMPLOYEE_MUST_USE_IDENTITY_PUBLIC_MODELS = noClasses()
            .that().resideInAPackage("com.hunyuan.sa.admin.module.system.role..")
            .should().dependOnClassesThat().resideInAPackage(
                    "com.hunyuan.sa.admin.module.system.employee..");

    @ArchTest
    static final ArchRule DATASCOPE_MUST_USE_ACCESS_DATA_SCOPE_API = noClasses()
            .that().resideInAPackage("com.hunyuan.sa.admin.module.system.datascope..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.hunyuan.sa.admin.module.system.role.dao..",
                    "com.hunyuan.sa.admin.module.system.role.domain.entity..");

    @ArchTest
    static final ArchRule ORGANIZATION_SCOPE_ADAPTER_MUST_USE_ACCESS_DATA_SCOPE_API = noClasses()
            .that().haveSimpleName("OrganizationDepartmentScopeAdapter")
            .should().dependOnClassesThat().resideInAPackage(
                    "com.hunyuan.sa.admin.module.system.datascope..");

    @ArchTest
    static final ArchRule ACCESS_AUTHORIZATION_ADAPTER_MUST_USE_ACCESS_CAPABILITY_QUERY_API =
            noClasses()
                    .that().haveSimpleName("AccessAuthorizationFacadeAdapter")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.hunyuan.sa.admin.module.system.menu..",
                            "com.hunyuan.sa.admin.module.system.role.dao..");

    @ArchTest
    static final ArchRule ACCESS_AUTHORIZATION_ADAPTER_MUST_USE_ROLE_MEMBERSHIP_API = noClasses()
            .that().haveSimpleName("AccessAuthorizationFacadeAdapter")
            .should().dependOnClassesThat().haveSimpleName("RoleEmployeeService");

    @ArchTest
    static final ArchRule ROLE_CAPABILITY_ADAPTERS_MUST_USE_ACCESS_MENU_QUERY_API = noClasses()
            .that().haveSimpleNameEndingWith("CapabilityQueryFacadeAdapter")
            .or().haveSimpleNameEndingWith("CapabilityGrantFacadeAdapter")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.hunyuan.sa.admin.module.system.menu.dao..",
                    "com.hunyuan.sa.admin.module.system.menu.domain..");

    @ArchTest
    static final ArchRule ROLE_MUST_NOT_DEPEND_ON_MENU_OWNER = noClasses()
            .that().resideInAPackage("com.hunyuan.sa.admin.module.system.role..")
            .should().dependOnClassesThat().resideInAPackage(
                    "com.hunyuan.sa.admin.module.system.menu..");

    @ArchTest
    static final ArchRule ROLE_MUST_NOT_DEPEND_ON_DATA_SCOPE_OWNER = noClasses()
            .that().resideInAPackage("com.hunyuan.sa.admin.module.system.role..")
            .should().dependOnClassesThat().resideInAPackage(
                    "com.hunyuan.sa.admin.module.system.datascope..");

    private static ArchCondition<JavaClass> notAccessAnotherModulePersistenceInternals() {
        return new ArchCondition<>("not access another admin module's DAO, Mapper, or Entity") {
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                String sourceModule = adminModuleName(source.getPackageName());
                if (sourceModule == null) {
                    return;
                }
                for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String targetPackage = target.getPackageName();
                    String targetModule = adminModuleName(targetPackage);
                    if (targetModule == null || sourceModule.equals(targetModule)) {
                        continue;
                    }
                    if (targetPackage.contains(".dao.")
                            || targetPackage.endsWith(".dao")
                            || targetPackage.contains(".domain.entity.")) {
                        String message = source.getName() + " directly depends on " + target.getName();
                        events.add(SimpleConditionEvent.violated(source, message));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notAccessAnotherSupportModulePersistenceInternals() {
        return new ArchCondition<>("不访问其他平台支撑模块的 DAO、Mapper 或 Entity") {
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                String sourceModule = supportModuleName(source.getPackageName());
                if (sourceModule == null) {
                    return;
                }
                for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String targetPackage = target.getPackageName();
                    String targetModule = supportModuleName(targetPackage);
                    if (targetModule == null || sourceModule.equals(targetModule)) {
                        continue;
                    }
                    if (isPersistenceInternal(targetPackage)) {
                        String message = source.getName() + " 直接依赖 " + target.getName();
                        events.add(SimpleConditionEvent.violated(source, message));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notExposeInternalModels() {
        return new ArchCondition<>("公开方法签名不暴露内部持久化或历史协议模型") {
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                for (JavaMethod method : source.getMethods()) {
                    if (!method.getOwner().equals(source)
                            || !method.getModifiers().contains(JavaModifier.PUBLIC)) {
                        continue;
                    }
                    for (JavaClass involvedType : method.getAllInvolvedRawTypes()) {
                        if (isInternalModel(involvedType.getPackageName())) {
                            String message = method.getFullName() + " 暴露 " + involvedType.getName();
                            events.add(SimpleConditionEvent.violated(method, message));
                        }
                    }
                }
            }
        };
    }

    private static boolean isPersistenceInternal(String packageName) {
        return packageName.contains(".dao.")
                || packageName.endsWith(".dao")
                || packageName.contains(".mapper.")
                || packageName.endsWith(".mapper")
                || packageName.contains(".domain.entity.")
                || packageName.endsWith(".domain.entity");
    }

    private static boolean isInternalModel(String packageName) {
        return isPersistenceInternal(packageName)
                || packageName.contains(".domain.form.")
                || packageName.endsWith(".domain.form")
                || packageName.contains(".domain.vo.")
                || packageName.endsWith(".domain.vo");
    }

    private static String adminModuleName(String packageName) {
        String prefix = "com.hunyuan.sa.admin.module.";
        if (!packageName.startsWith(prefix)) {
            return null;
        }
        String remainder = packageName.substring(prefix.length());
        int separator = remainder.indexOf('.');
        return separator < 0 ? remainder : remainder.substring(0, separator);
    }

    private static String supportModuleName(String packageName) {
        String prefix = "com.hunyuan.sa.base.module.support.";
        if (!packageName.startsWith(prefix)) {
            return null;
        }
        String remainder = packageName.substring(prefix.length());
        int separator = remainder.indexOf('.');
        return separator < 0 ? remainder : remainder.substring(0, separator);
    }
}
