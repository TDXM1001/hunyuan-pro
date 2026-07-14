package com.hunyuan.sa.bpm.schema;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.hunyuan.sa.bpm.module.category.domain.entity.BpmCategoryEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.form.domain.entity.BpmFormEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskActionLogEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class BpmAuditFieldFillConfigTest {

    @Test
    void criticalBpmEntitiesShouldFillCreateAndUpdateTimeAutomatically() throws NoSuchFieldException {
        assertFill(BpmCategoryEntity.class, "createTime", FieldFill.INSERT);
        assertFill(BpmCategoryEntity.class, "updateTime", FieldFill.INSERT_UPDATE);
        assertFill(BpmFormEntity.class, "createTime", FieldFill.INSERT);
        assertFill(BpmFormEntity.class, "updateTime", FieldFill.INSERT_UPDATE);
        assertFill(BpmDefinitionEntity.class, "createTime", FieldFill.INSERT);
        assertFill(BpmDefinitionEntity.class, "updateTime", FieldFill.INSERT_UPDATE);
        assertFill(BpmDefinitionNodeEntity.class, "createTime", FieldFill.INSERT);
        assertFill(BpmDefinitionNodeEntity.class, "updateTime", FieldFill.INSERT_UPDATE);
        assertFill(BpmInstanceEntity.class, "createTime", FieldFill.INSERT);
        assertFill(BpmInstanceEntity.class, "updateTime", FieldFill.INSERT_UPDATE);
        assertFill(BpmTaskEntity.class, "createTime", FieldFill.INSERT);
        assertFill(BpmTaskEntity.class, "updateTime", FieldFill.INSERT_UPDATE);
        assertFill(BpmTaskActionLogEntity.class, "createTime", FieldFill.INSERT);
    }

    private void assertFill(Class<?> entityClass, String fieldName, FieldFill expectedFill) throws NoSuchFieldException {
        Field field = entityClass.getDeclaredField(fieldName);
        TableField tableField = field.getAnnotation(TableField.class);
        assertThat(tableField)
                .as("%s.%s should declare @TableField fill", entityClass.getSimpleName(), fieldName)
                .isNotNull();
        assertThat(tableField.fill())
                .as("%s.%s fill mode", entityClass.getSimpleName(), fieldName)
                .isEqualTo(expectedFill);
    }
}
