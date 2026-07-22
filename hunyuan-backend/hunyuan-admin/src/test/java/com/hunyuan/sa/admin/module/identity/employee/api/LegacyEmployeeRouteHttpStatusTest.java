package com.hunyuan.sa.admin.module.identity.employee.api;

import com.hunyuan.sa.base.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyEmployeeRouteHttpStatusTest {

    @Test
    void mapsRemovedRoutesToHttpNotFound() throws Exception {
        Method handler = GlobalExceptionHandler.class.getMethod(
                "noResourceFoundExceptionHandler",
                NoResourceFoundException.class);

        ResponseStatus responseStatus = handler.getAnnotation(ResponseStatus.class);

        assertThat(responseStatus).isNotNull();
        assertThat(responseStatus.value()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
