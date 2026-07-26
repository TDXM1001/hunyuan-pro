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
    static final ArchRule PLATFORM_MODULE_CYCLES_MUST_NOT_GROW = freeze(slices()
            .matching("com.hunyuan.sa.admin.module.platform.(*)..")
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
     * base 仅承载通用基础设施与稳定协议，不允许重新放入业务 HTTP 入口。
     */
    @ArchTest
    static final ArchRule BASE_MUST_NOT_EXPOSE_HTTP_ROUTES = noClasses()
            .that().resideInAPackage("com.hunyuan.sa.base..")
            .should().beAnnotatedWith("org.springframework.web.bind.annotation.RestController");

    /**
     * 平台支撑模块不能新增对其他 owner 持久化内部实现的直接访问。
     */
    @ArchTest
    static final ArchRule SUPPORT_CROSS_MODULE_PERSISTENCE_ACCESS_MUST_NOT_GROW = freeze(noClasses()
            .that().resideInAPackage("com.hunyuan.sa.base.module.support..")
            .should(notAccessAnotherSupportModulePersistenceInternals()));

    /**
     * 配置 owner 的实现已经迁出 base，其他模块只能通过稳定 Facade 或配置值协议访问。
     */
    @ArchTest
    static final ArchRule PLATFORM_CONFIGURATION_INTERNALS_MUST_NOT_LEAK = noClasses()
            .that().resideOutsideOfPackage("com.hunyuan.sa.admin.module.platform.configuration..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.hunyuan.sa.admin.module.platform.configuration.config..",
                    "com.hunyuan.sa.admin.module.platform.configuration.dict..");

    /**
     * 文件存储、Mapper 和历史模型只属于 platform-file，消费者必须使用 PlatformFileFacade。
     */
    @ArchTest
    static final ArchRule PLATFORM_FILE_INTERNALS_MUST_NOT_LEAK = noClasses()
            .that().resideOutsideOfPackage("com.hunyuan.sa.admin.module.platform.file..")
            .should().dependOnClassesThat().resideInAPackage(
                    "com.hunyuan.sa.admin.module.platform.file..");

    /**
     * 安全策略、验证码、登录失败与密码历史实现归属 platform-security；
     * 登录、身份和文件 owner 只能依赖 base 中的稳定安全协议与密码 codec。
     */
    @ArchTest
    static final ArchRule PLATFORM_SECURITY_INTERNALS_MUST_NOT_LEAK = noClasses()
            .that().resideOutsideOfPackage("com.hunyuan.sa.admin.module.platform.security..")
            .should().dependOnClassesThat().resideInAPackage(
                    "com.hunyuan.sa.admin.module.platform.security..");

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
     * 消息、短信和邮件实现已经归属 platform-notification，其他 owner 只能依赖稳定协议。
     */
    @ArchTest
    static final ArchRule PLATFORM_NOTIFICATION_INTERNALS_MUST_NOT_LEAK = noClasses()
            .that().resideOutsideOfPackage("com.hunyuan.sa.admin.module.platform.notification..")
            .should().dependOnClassesThat().resideInAPackage(
                    "com.hunyuan.sa.admin.module.platform.notification..");

    @ArchTest
    static final ArchRule LOGIN_MUST_USE_PLATFORM_NOTIFICATION_API = noClasses()
            .that().resideInAPackage("com.hunyuan.sa.admin.module.system.login..")
            .should().dependOnClassesThat().resideInAPackage(
                    "com.hunyuan.sa.admin.module.platform.notification..");

    /**
     * 身份和登录模块只能通过平台文件公开接口解析头像，不能依赖具体存储实现。
     */
    @ArchTest
    static final ArchRule IDENTITY_AND_LOGIN_MUST_USE_PLATFORM_FILE_API = noClasses()
            .that().resideInAnyPackage(
                    "com.hunyuan.sa.admin.module.identity..",
                    "com.hunyuan.sa.admin.module.system.login..")
            .should().dependOnClassesThat().haveFullyQualifiedName(
                    "com.hunyuan.sa.admin.module.platform.file.service.IFileStorageService");

    /**
     * 审计的 Controller、切面、Service 和持久化模型已经归属 platform-audit，
     * 其他 owner 只能依赖 base 中的稳定审计协议。
     */
    @ArchTest
    static final ArchRule PLATFORM_AUDIT_INTERNALS_MUST_NOT_LEAK = noClasses()
            .that().resideOutsideOfPackage("com.hunyuan.sa.admin.module.platform.audit..")
            .should().dependOnClassesThat().resideInAPackage(
                    "com.hunyuan.sa.admin.module.platform.audit..");

    /**
     * 登录认证写入和读取登录审计信息时，不得回退到审计 Entity、DAO 或 Service。
     */
    @ArchTest
    static final ArchRule LOGIN_MUST_USE_PLATFORM_AUDIT_API = noClasses()
            .that().resideInAPackage("com.hunyuan.sa.admin.module.system.login..")
            .should().dependOnClassesThat().resideInAPackage(
                    "com.hunyuan.sa.admin.module.platform.audit..");

    /**
     * 运行时 Controller、ApplicationService、调度实现和持久化模型归属 platform-runtime；
     * 其他 owner 只能依赖 base 中的稳定 Facade、公开 DTO、任务扩展协议和重载注解。
     */
    @ArchTest
    static final ArchRule PLATFORM_RUNTIME_INTERNALS_MUST_NOT_LEAK = noClasses()
            .that().resideOutsideOfPackage("com.hunyuan.sa.admin.module.platform.runtime..")
            .should().dependOnClassesThat().resideInAPackage(
                    "com.hunyuan.sa.admin.module.platform.runtime..");

    /**
     * 代码生成器、API 加密验证和历史兼容入口统一归属 platform-devtools；
     * 其他 owner 只能依赖 base 中的稳定 Facade 与公开 DTO。
     */
    @ArchTest
    static final ArchRule PLATFORM_DEVTOOLS_INTERNALS_MUST_NOT_LEAK = noClasses()
            .that().resideOutsideOfPackage("com.hunyuan.sa.admin.module.platform.devtools..")
            .should().dependOnClassesThat().resideInAPackage(
                    "com.hunyuan.sa.admin.module.platform.devtools..");

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
                    if (isPersistenceInternal(target)) {
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
                    if (isPersistenceInternal(target)) {
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
                        if (isInternalModel(involvedType)) {
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

    private static boolean isPersistenceInternal(JavaClass type) {
        return isPersistenceInternal(type.getPackageName())
                || type.getSimpleName().endsWith("Dao")
                || type.getSimpleName().endsWith("Mapper");
    }

    private static boolean isInternalModel(JavaClass type) {
        String packageName = type.getPackageName();
        return isPersistenceInternal(type)
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
        if (separator < 0) {
            return remainder;
        }
        String topLevelModule = remainder.substring(0, separator);
        if (!"platform".equals(topLevelModule)) {
            return topLevelModule;
        }
        String platformRemainder = remainder.substring(separator + 1);
        int platformSeparator = platformRemainder.indexOf('.');
        String platformOwner = platformSeparator < 0
                ? platformRemainder
                : platformRemainder.substring(0, platformSeparator);
        return platformOwner.isEmpty() ? topLevelModule : topLevelModule + "." + platformOwner;
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
