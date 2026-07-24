package com.hunyuan.sa.base.module.support.codegenerator.application;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.module.support.codegenerator.api.PlatformCodeGeneratorColumnView;
import com.hunyuan.sa.base.module.support.codegenerator.api.PlatformCodeGeneratorConfig;
import com.hunyuan.sa.base.module.support.codegenerator.api.PlatformCodeGeneratorConfigUpdateCommand;
import com.hunyuan.sa.base.module.support.codegenerator.api.PlatformCodeGeneratorTableView;
import com.hunyuan.sa.base.module.support.codegenerator.domain.form.CodeGeneratorConfigForm;
import com.hunyuan.sa.base.module.support.codegenerator.domain.model.CodeBasic;
import com.hunyuan.sa.base.module.support.codegenerator.domain.model.CodeDelete;
import com.hunyuan.sa.base.module.support.codegenerator.domain.model.CodeField;
import com.hunyuan.sa.base.module.support.codegenerator.domain.model.CodeInsertAndUpdate;
import com.hunyuan.sa.base.module.support.codegenerator.domain.model.CodeInsertAndUpdateField;
import com.hunyuan.sa.base.module.support.codegenerator.domain.model.CodeQueryField;
import com.hunyuan.sa.base.module.support.codegenerator.domain.model.CodeTableField;
import com.hunyuan.sa.base.module.support.codegenerator.domain.vo.TableColumnVO;
import com.hunyuan.sa.base.module.support.codegenerator.domain.vo.TableConfigVO;
import com.hunyuan.sa.base.module.support.codegenerator.domain.vo.TableVO;

import java.util.List;

/**
 * 稳定代码生成器 DTO 与历史模型之间的集中映射器。
 */
public final class PlatformCodeGeneratorMapper {

    private PlatformCodeGeneratorMapper() {
    }

    public static List<PlatformCodeGeneratorColumnView> toColumnViews(
            List<TableColumnVO> source) {
        return SmartBeanUtil.copyList(source, PlatformCodeGeneratorColumnView.class);
    }

    public static List<TableColumnVO> toLegacyColumns(
            List<PlatformCodeGeneratorColumnView> source) {
        return SmartBeanUtil.copyList(source, TableColumnVO.class);
    }

    public static PageResult<PlatformCodeGeneratorTableView> toTablePage(
            PageResult<TableVO> source) {
        PageResult<PlatformCodeGeneratorTableView> result = copyPageMetadata(source);
        result.setList(SmartBeanUtil.copyList(
                source.getList(), PlatformCodeGeneratorTableView.class));
        return result;
    }

    public static PageResult<TableVO> toLegacyTablePage(
            PageResult<PlatformCodeGeneratorTableView> source) {
        PageResult<TableVO> result = copyPageMetadata(source);
        result.setList(SmartBeanUtil.copyList(source.getList(), TableVO.class));
        return result;
    }

    public static PlatformCodeGeneratorConfig toConfig(TableConfigVO source) {
        PlatformCodeGeneratorConfig target = new PlatformCodeGeneratorConfig();
        target.setBasic(SmartBeanUtil.copy(
                source.getBasic(), PlatformCodeGeneratorConfig.Basic.class));
        target.setFields(copyNullableList(
                source.getFields(), PlatformCodeGeneratorConfig.Field.class));
        target.setInsertAndUpdate(toInsertAndUpdate(source.getInsertAndUpdate()));
        target.setDeleteInfo(SmartBeanUtil.copy(
                source.getDeleteInfo(), PlatformCodeGeneratorConfig.DeleteInfo.class));
        target.setQueryFields(copyNullableList(
                source.getQueryFields(), PlatformCodeGeneratorConfig.QueryField.class));
        target.setTableFields(copyNullableList(
                source.getTableFields(), PlatformCodeGeneratorConfig.TableField.class));
        return target;
    }

    public static TableConfigVO toLegacyConfig(PlatformCodeGeneratorConfig source) {
        TableConfigVO target = new TableConfigVO();
        target.setBasic(SmartBeanUtil.copy(source.getBasic(), CodeBasic.class));
        target.setFields(copyNullableList(source.getFields(), CodeField.class));
        target.setInsertAndUpdate(toLegacyInsertAndUpdate(source.getInsertAndUpdate()));
        target.setDeleteInfo(SmartBeanUtil.copy(source.getDeleteInfo(), CodeDelete.class));
        target.setQueryFields(copyNullableList(
                source.getQueryFields(), CodeQueryField.class));
        target.setTableFields(copyNullableList(
                source.getTableFields(), CodeTableField.class));
        return target;
    }

    public static PlatformCodeGeneratorConfigUpdateCommand toUpdateCommand(
            CodeGeneratorConfigForm source) {
        TableConfigVO legacyConfig = new TableConfigVO();
        legacyConfig.setBasic(source.getBasic());
        legacyConfig.setFields(source.getFields());
        legacyConfig.setInsertAndUpdate(source.getInsertAndUpdate());
        legacyConfig.setDeleteInfo(source.getDeleteInfo());
        legacyConfig.setQueryFields(source.getQueryFields());
        legacyConfig.setTableFields(source.getTableFields());
        PlatformCodeGeneratorConfigUpdateCommand target =
                new PlatformCodeGeneratorConfigUpdateCommand();
        SmartBeanUtil.copyProperties(toConfig(legacyConfig), target);
        target.setTableName(source.getTableName());
        return target;
    }

    public static CodeGeneratorConfigForm toLegacyUpdateForm(
            PlatformCodeGeneratorConfigUpdateCommand source) {
        TableConfigVO config = toLegacyConfig(source);
        CodeGeneratorConfigForm target = new CodeGeneratorConfigForm();
        target.setTableName(source.getTableName());
        target.setBasic(config.getBasic());
        target.setFields(config.getFields());
        target.setInsertAndUpdate(config.getInsertAndUpdate());
        target.setDeleteInfo(config.getDeleteInfo());
        target.setQueryFields(config.getQueryFields());
        target.setTableFields(config.getTableFields());
        return target;
    }

    private static PlatformCodeGeneratorConfig.InsertAndUpdate toInsertAndUpdate(
            CodeInsertAndUpdate source) {
        if (source == null) {
            return null;
        }
        PlatformCodeGeneratorConfig.InsertAndUpdate target = SmartBeanUtil.copy(
                source, PlatformCodeGeneratorConfig.InsertAndUpdate.class);
        target.setFieldList(copyNullableList(
                source.getFieldList(), PlatformCodeGeneratorConfig.InsertAndUpdateField.class));
        return target;
    }

    private static CodeInsertAndUpdate toLegacyInsertAndUpdate(
            PlatformCodeGeneratorConfig.InsertAndUpdate source) {
        if (source == null) {
            return null;
        }
        CodeInsertAndUpdate target = SmartBeanUtil.copy(source, CodeInsertAndUpdate.class);
        target.setFieldList(copyNullableList(
                source.getFieldList(), CodeInsertAndUpdateField.class));
        return target;
    }

    /**
     * 配置字段未设置时保留 null，避免历史接口把“未配置”误报为空配置。
     */
    private static <S, T> List<T> copyNullableList(List<S> source, Class<T> targetType) {
        return source == null ? null : SmartBeanUtil.copyList(source, targetType);
    }

    /**
     * 复制分页元数据，确保公开和历史元素类型彼此隔离。
     */
    private static <S, T> PageResult<T> copyPageMetadata(PageResult<S> source) {
        PageResult<T> result = new PageResult<>();
        result.setPageNum(source.getPageNum());
        result.setPageSize(source.getPageSize());
        result.setTotal(source.getTotal());
        result.setPages(source.getPages());
        result.setEmptyFlag(source.getEmptyFlag());
        return result;
    }
}
