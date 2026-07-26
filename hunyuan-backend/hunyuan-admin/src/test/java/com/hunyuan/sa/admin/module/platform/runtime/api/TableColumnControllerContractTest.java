package com.hunyuan.sa.admin.module.platform.runtime.api;

import com.hunyuan.sa.admin.module.platform.runtime.api.legacy.TableColumnController;
import com.hunyuan.sa.admin.module.platform.runtime.tablecolumn.domain.TableColumnUpdateForm;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定表格列偏好的历史 URL，归位运行时 owner 时不得破坏旧客户端契约。
 */
class TableColumnControllerContractTest {

    @Test
    void preservesLegacyTableColumnRoutes() throws Exception {
        var update = TableColumnController.class.getMethod(
                "updateTableColumn", TableColumnUpdateForm.class);
        assertThat(update.getAnnotation(PostMapping.class).value())
                .containsExactly("/tableColumn/update");

        var delete = TableColumnController.class.getMethod(
                "deleteTableColumn", Integer.class);
        assertThat(delete.getAnnotation(GetMapping.class).value())
                .containsExactly("/tableColumn/delete/{tableId}");

        var getColumns = TableColumnController.class.getMethod(
                "getColumns", Integer.class);
        assertThat(getColumns.getAnnotation(GetMapping.class).value())
                .containsExactly("/tableColumn/getColumns/{tableId}");
    }
}
