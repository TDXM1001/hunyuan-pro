package com.hunyuan.sa.admin.module.access.role.api;

import com.hunyuan.sa.base.common.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色成员分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AccessRoleMemberQuery extends PageParam {

    @Schema(description = "姓名、手机号或登录账号关键字")
    private String keywords;

    @Schema(hidden = true)
    private String roleId;
}
