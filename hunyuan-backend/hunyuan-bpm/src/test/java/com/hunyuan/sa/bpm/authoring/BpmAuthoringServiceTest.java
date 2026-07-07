package com.hunyuan.sa.bpm.authoring;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.engine.compiler.SimpleModelValidator;
import com.hunyuan.sa.bpm.module.form.dao.BpmFormDao;
import com.hunyuan.sa.bpm.module.model.dao.BpmModelDao;
import com.hunyuan.sa.bpm.module.model.domain.entity.BpmModelEntity;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmDesignerSaveForm;
import com.hunyuan.sa.bpm.module.model.service.BpmDesignerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class BpmAuthoringServiceTest {

    private BpmDesignerService bpmDesignerService;

    private BpmModelDao bpmModelDao;

    @BeforeEach
    void setUp() {
        bpmDesignerService = new BpmDesignerService();
        bpmModelDao = Mockito.mock(BpmModelDao.class);
        setField(bpmDesignerService, "bpmModelDao", bpmModelDao);
        setField(bpmDesignerService, "bpmFormDao", Mockito.mock(BpmFormDao.class));
        setField(bpmDesignerService, "simpleModelValidator", new SimpleModelValidator());
    }

    @Test
    void saveDesignerDraftShouldPersistDraftPayloadAndFlipHasUnpublishedChanges() {
        BpmModelEntity entity = new BpmModelEntity();
        entity.setModelId(1L);
        entity.setFormId(9L);
        entity.setHasUnpublishedChanges(Boolean.FALSE);
        when(bpmModelDao.selectById(anyLong())).thenReturn(entity);
        when(bpmModelDao.updateById(any(BpmModelEntity.class))).thenReturn(1);

        BpmDesignerSaveForm form = new BpmDesignerSaveForm();
        form.setModelId(1L);
        form.setSimpleModelJson("{\"nodes\":[]}");
        form.setStartRuleJson("{\"allowAll\":true}");
        form.setTitleRuleJson("{\"template\":\"测试流程\"}");

        ResponseDTO<String> response = bpmDesignerService.saveDesignerDraft(form);

        assertThat(response.getOk()).isTrue();
        assertThat(entity.getSimpleModelJson()).isEqualTo("{\"nodes\":[]}");
        assertThat(entity.getStartRuleJson()).isEqualTo("{\"allowAll\":true}");
        assertThat(entity.getTitleRuleJson()).isEqualTo("{\"template\":\"测试流程\"}");
        assertThat(entity.getHasUnpublishedChanges()).isTrue();
    }

    @Test
    void validateDraftShouldRejectUnsupportedApprovalMode() {
        BpmModelEntity entity = new BpmModelEntity();
        entity.setModelId(2L);
        entity.setSimpleModelJson("{\"nodes\":[{\"type\":\"userTask\",\"approvalMode\":\"ratio\"}]}");
        entity.setStartRuleJson("{\"allowAll\":true}");
        when(bpmModelDao.selectById(2L)).thenReturn(entity);

        ResponseDTO<String> response = bpmDesignerService.validateDesignerDraft(2L);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("单人审批");
    }

    @Test
    void controllerSourceShouldExposeBpmAuthoringRoutes() throws IOException {
        assertControllerContains(
                "src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmCategoryController.java",
                "/bpm/category/query",
                "/bpm/category/add",
                "/bpm/category/update"
        );
        assertControllerContains(
                "src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmFormController.java",
                "/bpm/form/query",
                "/bpm/form/add",
                "/bpm/form/update"
        );
        assertControllerContains(
                "src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmModelController.java",
                "/bpm/model/query",
                "/bpm/model/add",
                "/bpm/model/update"
        );
        assertControllerContains(
                "src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmDesignerController.java",
                "/bpm/designer/detail/",
                "/bpm/designer/save",
                "/bpm/designer/validate/",
                "/bpm/designer/simulate/"
        );
    }

    private void assertControllerContains(String relativePath, String... routeFragments) throws IOException {
        Path filePath = Path.of(relativePath);
        assertThat(Files.exists(filePath)).isTrue();

        String source = Files.readString(filePath);
        for (String routeFragment : routeFragments) {
            assertThat(source).contains(routeFragment);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("设置测试字段失败: " + fieldName, ex);
        }
    }
}
