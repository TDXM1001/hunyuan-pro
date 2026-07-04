package com.hunyuan.sa.admin.module.system.login.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import com.hunyuan.sa.base.common.swagger.SchemaEnum;
import com.hunyuan.sa.base.common.validator.enumeration.CheckEnum;
import com.hunyuan.sa.base.constant.LoginDeviceEnum;
import org.hibernate.validator.constraints.Length;

/**
 * Employee login form.
 */
@Data
public class LoginForm {

    @Schema(description = "Login account")
    @NotBlank(message = "登录账号不能为空")
    @Length(max = 30, message = "登录账号最多30字符")
    private String loginName;

    @Schema(description = "Password")
    @NotBlank(message = "密码不能为空")
    private String password;

    @SchemaEnum(desc = "Login device", value = LoginDeviceEnum.class)
    @CheckEnum(value = LoginDeviceEnum.class, required = true, message = "此终端不允许登录")
    private Integer loginDevice;

    @Schema(description = "Captcha code")
    private String captchaCode;

    @Schema(description = "Captcha uuid")
    private String captchaUuid;

    @Schema(description = "Email verification code")
    private String emailCode;
}
