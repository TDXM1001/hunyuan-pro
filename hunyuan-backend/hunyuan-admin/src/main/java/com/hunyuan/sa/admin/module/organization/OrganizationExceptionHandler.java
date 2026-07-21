package com.hunyuan.sa.admin.module.organization;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OrganizationExceptionHandler {

    @ExceptionHandler(OrganizationBusinessException.class)
    public ResponseDTO<?> handle(OrganizationBusinessException exception) {
        return ResponseDTO.error(exception.getErrorCode(), exception.getMessage());
    }
}
