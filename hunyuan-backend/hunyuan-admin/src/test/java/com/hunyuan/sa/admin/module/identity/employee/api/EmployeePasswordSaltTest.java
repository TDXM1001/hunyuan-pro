package com.hunyuan.sa.admin.module.identity.employee.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeePasswordSaltTest {

    @Test
    void preservesLegacyPasswordSaltFormat() {
        assertThat(EmployeePasswordSalt.apply("secret", "Ab-12"))
                .isEqualTo("secret_AB-12_ab-12");
    }
}
