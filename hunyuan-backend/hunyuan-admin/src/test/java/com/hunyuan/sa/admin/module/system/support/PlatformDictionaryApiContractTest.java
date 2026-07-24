package com.hunyuan.sa.admin.module.system.support;

import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryCreateCommand;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryUpdateCommand;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryItemCreateCommand;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryItemUpdateCommand;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 稳定字典 HTTP 路由契约，防止前端读取路径回退到历史支持路由。
 */
class PlatformDictionaryApiContractTest {

    @Test
    void exposesStableDictionaryReadRoutes() throws Exception {
        RequestMapping mapping = PlatformDictionaryController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping.value()).containsExactly("/api/admin/v1/platform/dictionaries");
        assertThat(PlatformDictionaryController.class
                .getMethod("queryPage", com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryPageQuery.class)
                .getAnnotation(PostMapping.class).value()).containsExactly("/query");
        assertThat(PlatformDictionaryController.class
                .getMethod("getAllItems")
                .getAnnotation(GetMapping.class).value()).containsExactly("/items");
        assertThat(PlatformDictionaryController.class
                .getMethod("getItemsByDictionaryId", Long.class)
                .getAnnotation(GetMapping.class).value()).containsExactly("/{dictionaryId}/items");
        assertThat(PlatformDictionaryController.class
                .getMethod("create", PlatformDictionaryCreateCommand.class)
                .getAnnotation(PostMapping.class).value()).isEmpty();
        assertThat(PlatformDictionaryController.class
                .getMethod("update", Long.class, PlatformDictionaryUpdateCommand.class)
                .getAnnotation(PutMapping.class).value()).containsExactly("/{dictionaryId}");
        assertThat(PlatformDictionaryController.class
                .getMethod("toggleDisabled", Long.class)
                .getAnnotation(PostMapping.class).value()).containsExactly("/{dictionaryId}/toggle-disabled");
        assertThat(PlatformDictionaryController.class
                .getMethod("delete", Long.class)
                .getAnnotation(DeleteMapping.class).value()).containsExactly("/{dictionaryId}");
        assertThat(PlatformDictionaryController.class
                .getMethod("createItem", Long.class, PlatformDictionaryItemCreateCommand.class)
                .getAnnotation(PostMapping.class).value()).containsExactly("/{dictionaryId}/items");
        assertThat(PlatformDictionaryController.class
                .getMethod("updateItem", Long.class, Long.class, PlatformDictionaryItemUpdateCommand.class)
                .getAnnotation(PutMapping.class).value())
                .containsExactly("/{dictionaryId}/items/{dictionaryItemId}");
        assertThat(PlatformDictionaryController.class
                .getMethod("deleteItem", Long.class)
                .getAnnotation(DeleteMapping.class).value()).containsExactly("/items/{dictionaryItemId}");
    }
}
