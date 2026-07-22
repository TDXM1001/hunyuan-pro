package com.hunyuan.sa.admin.module.identity.employee.api;

import com.hunyuan.sa.base.common.enumeration.GenderEnum;
import com.hunyuan.sa.base.common.swagger.SchemaEnum;
import com.hunyuan.sa.base.common.util.SmartVerificationUtil;
import com.hunyuan.sa.base.common.validator.enumeration.CheckEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public record EmployeeUpdateCommand(
        @Schema(description = "员工id")
        @NotNull(message = "员工id不能为空")
        Long employeeId,

        @Schema(description = "姓名")
        @NotNull(message = "姓名不能为空")
        @Length(max = 30, message = "姓名最多30字符")
        String actualName,

        @Schema(description = "登录账号")
        @NotNull(message = "登录账号不能为空")
        @Length(max = 30, message = "登录账号最多30字符")
        String loginName,

        @SchemaEnum(GenderEnum.class)
        @CheckEnum(value = GenderEnum.class, message = "性别错误")
        Integer gender,

        @Schema(description = "部门id")
        @NotNull(message = "部门id不能为空")
        Long departmentId,

        @Schema(description = "手机号")
        @NotNull(message = "手机号不能为空")
        @Pattern(regexp = SmartVerificationUtil.PHONE_REGEXP, message = "手机号格式不正确")
        String phone,

        @Schema(description = "邮箱账号")
        @NotNull(message = "邮箱账号不能为空")
        @Pattern(regexp = SmartVerificationUtil.EMAIL, message = "邮箱账号格式不正确")
        String email,

        @Schema(description = "职务级别ID")
        Long positionId,

        @Schema(description = "备注")
        @Length(max = 200, message = "备注最多200字符")
        String remark
) {
}
