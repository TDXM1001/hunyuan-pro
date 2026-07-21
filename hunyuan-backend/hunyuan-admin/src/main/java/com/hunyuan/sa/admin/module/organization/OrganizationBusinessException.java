package com.hunyuan.sa.admin.module.organization;

import com.hunyuan.sa.base.common.code.ErrorCode;
import com.hunyuan.sa.base.common.exception.BusinessException;

public class OrganizationBusinessException extends BusinessException {

    private final ErrorCode errorCode;

    public OrganizationBusinessException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.errorCode = errorCode;
    }

    public OrganizationBusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
