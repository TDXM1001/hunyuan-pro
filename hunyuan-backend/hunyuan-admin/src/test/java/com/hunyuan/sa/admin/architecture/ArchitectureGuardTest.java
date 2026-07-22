package com.hunyuan.sa.admin.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
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
    static final ArchRule ROLE_CONTROLLER_MUST_USE_ACCESS_ROLE_LIFECYCLE_API = noClasses()
            .that().haveSimpleName("RoleController")
            .should().dependOnClassesThat().resideInAPackage(
                    "com.hunyuan.sa.admin.module.system.role.service..");

    @ArchTest
    static final ArchRule ROLE_MENU_CONTROLLER_MUST_USE_ACCESS_CAPABILITY_API = noClasses()
            .that().haveSimpleName("RoleMenuController")
            .should().dependOnClassesThat().resideInAPackage(
                    "com.hunyuan.sa.admin.module.system.role.service..");

    @ArchTest
    static final ArchRule ACCESS_AUTHORIZATION_ADAPTER_MUST_USE_ACCESS_CAPABILITY_QUERY_API =
            noClasses()
                    .that().haveSimpleName("AccessAuthorizationFacadeAdapter")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.hunyuan.sa.admin.module.system.menu..",
                            "com.hunyuan.sa.admin.module.system.role.dao..");

    @ArchTest
    static final ArchRule MENU_CONTROLLER_MUST_USE_ACCESS_MENU_API = noClasses()
            .that().haveSimpleName("MenuController")
            .should().dependOnClassesThat().resideInAPackage(
                    "com.hunyuan.sa.admin.module.system.menu.service..");

    @ArchTest
    static final ArchRule ROLE_EMPLOYEE_CONTROLLER_MUST_USE_ACCESS_ROLE_API = noClasses()
            .that().haveSimpleName("RoleEmployeeController")
            .should().dependOnClassesThat().resideInAPackage(
                    "com.hunyuan.sa.admin.module.system.role.service..");

    @ArchTest
    static final ArchRule ACCESS_AUTHORIZATION_ADAPTER_MUST_USE_ROLE_MEMBERSHIP_API = noClasses()
            .that().haveSimpleName("AccessAuthorizationFacadeAdapter")
            .should().dependOnClassesThat().haveSimpleName("RoleEmployeeService");

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

    private static String adminModuleName(String packageName) {
        String prefix = "com.hunyuan.sa.admin.module.";
        if (!packageName.startsWith(prefix)) {
            return null;
        }
        String remainder = packageName.substring(prefix.length());
        int separator = remainder.indexOf('.');
        return separator < 0 ? remainder : remainder.substring(0, separator);
    }
}
