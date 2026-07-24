package com.hunyuan.sa.base.module.support.codegenerator.api;

import com.hunyuan.sa.base.common.swagger.SchemaEnum;
import com.hunyuan.sa.base.common.validator.enumeration.CheckEnum;
import com.hunyuan.sa.base.module.support.codegenerator.constant.CodeDeleteEnum;
import com.hunyuan.sa.base.module.support.codegenerator.constant.CodeFrontComponentEnum;
import com.hunyuan.sa.base.module.support.codegenerator.constant.CodeGeneratorPageTypeEnum;
import com.hunyuan.sa.base.module.support.codegenerator.constant.CodeQueryFieldQueryTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 代码生成器稳定配置结构。
 *
 * <p>嵌套类型属于公开契约，避免稳定接口直接暴露历史领域模型。</p>
 */
@Data
public class PlatformCodeGeneratorConfig {

    @Valid
    @NotNull(message = "基础信息不能为空")
    private Basic basic;

    @Valid
    @NotNull(message = "字段信息不能为空")
    private List<Field> fields;

    @Valid
    @NotNull(message = "增加、修改信息不能为空")
    private InsertAndUpdate insertAndUpdate;

    @Valid
    @NotNull(message = "删除信息不能为空")
    private DeleteInfo deleteInfo;

    @Valid
    private List<QueryField> queryFields;

    @Valid
    private List<TableField> tableFields;

    /**
     * 基础命名配置。
     */
    @Data
    public static class Basic {

        @NotBlank(message = "业务名称不能为空")
        private String moduleName;

        @NotBlank(message = "Java 包名不能为空")
        private String javaPackageName;

        @NotBlank(message = "注释不能为空")
        private String description;

        @NotBlank(message = "前端作者不能为空")
        private String frontAuthor;

        @NotNull(message = "前端时间不能为空")
        private LocalDateTime frontDate;

        @NotBlank(message = "后端作者不能为空")
        private String backendAuthor;

        @NotNull(message = "后端时间不能为空")
        private LocalDateTime backendDate;

        @NotNull(message = "版权信息不能为空")
        private String copyright;
    }

    /**
     * 基础字段配置。
     */
    @Data
    public static class Field {

        @NotBlank(message = "列名不能为空")
        private String columnName;

        private String columnComment;

        @NotBlank(message = "字段名称不能为空")
        private String label;

        @NotBlank(message = "字段命名不能为空")
        private String fieldName;

        @NotBlank(message = "Java 类型不能为空")
        private String javaType;

        @NotBlank(message = "JavaScript 类型不能为空")
        private String jsType;

        private String dict;

        private String enumName;

        @NotNull(message = "主键标识不能为空")
        private Boolean primaryKeyFlag;

        @NotNull(message = "自增标识不能为空")
        private Boolean autoIncreaseFlag;
    }

    /**
     * 新增和修改页面配置。
     */
    @Data
    public static class InsertAndUpdate {

        @NotNull(message = "是否支持新增和修改不能为空")
        private Boolean isSupportInsertAndUpdate;

        @SchemaEnum(CodeGeneratorPageTypeEnum.class)
        @CheckEnum(value = CodeGeneratorPageTypeEnum.class, message = "页面类型错误")
        private String pageType;

        private String width;

        @NotNull(message = "每行字段数量不能为空")
        private Integer countPerLine;

        @Valid
        private List<InsertAndUpdateField> fieldList;
    }

    /**
     * 新增和修改字段配置。
     */
    @Data
    public static class InsertAndUpdateField {

        @NotBlank(message = "列名不能为空")
        private String columnName;

        @NotNull(message = "必填标识不能为空")
        private Boolean requiredFlag;

        @NotNull(message = "插入标识不能为空")
        private Boolean insertFlag;

        @NotNull(message = "更新标识不能为空")
        private Boolean updateFlag;

        @SchemaEnum(CodeFrontComponentEnum.class)
        @CheckEnum(value = CodeFrontComponentEnum.class, message = "组件类型错误", required = true)
        private String frontComponent;
    }

    /**
     * 删除行为配置。
     */
    @Data
    public static class DeleteInfo {

        @NotNull(message = "是否支持删除不能为空")
        private Boolean isSupportDelete;

        @NotNull(message = "是否物理删除不能为空")
        private Boolean isPhysicallyDeleted;

        @NotBlank(message = "删除类型不能为空")
        @SchemaEnum(CodeDeleteEnum.class)
        @CheckEnum(value = CodeDeleteEnum.class, message = "删除类型错误")
        private String deleteEnum;
    }

    /**
     * 查询条件配置。
     */
    @Data
    public static class QueryField {

        @NotBlank(message = "条件名称不能为空")
        private String label;

        @NotBlank(message = "字段名不能为空")
        private String fieldName;

        @SchemaEnum(CodeQueryFieldQueryTypeEnum.class)
        @CheckEnum(value = CodeQueryFieldQueryTypeEnum.class, message = "查询类型错误")
        private String queryTypeEnum;

        @NotEmpty(message = "查询列不能为空")
        private List<String> columnNameList;

        @NotBlank(message = "查询宽度不能为空")
        private String width;
    }

    /**
     * 列表字段配置。
     */
    @Data
    public static class TableField {

        @NotBlank(message = "列名不能为空")
        private String columnName;

        @NotBlank(message = "字段名称不能为空")
        private String label;

        @NotBlank(message = "字段命名不能为空")
        private String fieldName;

        @NotNull(message = "列表显示标识不能为空")
        private Boolean showFlag;

        private Integer width;

        @NotNull(message = "自动省略标识不能为空")
        private Boolean ellipsisFlag;
    }
}
