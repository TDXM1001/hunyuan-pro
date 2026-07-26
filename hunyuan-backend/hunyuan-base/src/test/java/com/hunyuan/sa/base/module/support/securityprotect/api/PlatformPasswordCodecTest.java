package com.hunyuan.sa.base.module.support.securityprotect.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformPasswordCodecTest {

    @Test
    void keepsArgon2CompatibilityAndMinimumCharacterCategories() {
        String encoded = PlatformPasswordCodec.encode("Strong#Password1");

        assertThat(PlatformPasswordCodec.matches("Strong#Password1", encoded)).isTrue();
        assertThat(PlatformPasswordCodec.matches("wrong", encoded)).isFalse();
        assertThat(PlatformPasswordCodec.hasRequiredCharacterCategories("Strong#Password1"))
                .isTrue();
        assertThat(PlatformPasswordCodec.hasRequiredCharacterCategories("onlyletters"))
                .isFalse();
    }
}
