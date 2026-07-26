package com.hunyuan.sa.base.module.support.securityprotect.api;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

/**
 * 平台密码摘要稳定协议。
 *
 * <p>初始化管理员发生在可变安全策略加载之前，因此这里只保留确定性的摘要算法和最低字符组合判断；
 * 密码复杂度开关、历史重复次数与变更周期仍由 platform-security owner 管理。</p>
 */
public final class PlatformPasswordCodec {

    private static final String REQUIRED_CHARACTER_CATEGORIES_PATTERN =
            "^(?![a-zA-Z]+$)(?![A-Z0-9]+$)(?![A-Z\\W_!@#$%^&*`~()-+=]+$)"
                    + "(?![a-z0-9]+$)(?![a-z\\W_!@#$%^&*`~()-+=]+$)"
                    + "(?![0-9\\W_!@#$%^&*`~()-+=]+$)[a-zA-Z0-9\\W_!@#$%^&*`~()-+=]*$";

    private static final Argon2PasswordEncoder ENCODER =
            Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    private PlatformPasswordCodec() {
    }

    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        return ENCODER.matches(rawPassword, encodedPassword);
    }

    public static boolean hasRequiredCharacterCategories(String password) {
        return password != null && password.matches(REQUIRED_CHARACTER_CATEGORIES_PATTERN);
    }
}
