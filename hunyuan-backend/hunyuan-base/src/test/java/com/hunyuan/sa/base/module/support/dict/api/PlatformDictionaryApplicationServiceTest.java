package com.hunyuan.sa.base.module.support.dict.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.module.support.dict.application.PlatformDictionaryApplicationService;
import com.hunyuan.sa.base.module.support.dict.domain.form.DictQueryForm;
import com.hunyuan.sa.base.module.support.dict.domain.form.DictAddForm;
import com.hunyuan.sa.base.module.support.dict.domain.form.DictUpdateForm;
import com.hunyuan.sa.base.module.support.dict.domain.form.DictDataAddForm;
import com.hunyuan.sa.base.module.support.dict.domain.form.DictDataUpdateForm;
import com.hunyuan.sa.base.module.support.dict.domain.vo.DictDataVO;
import com.hunyuan.sa.base.module.support.dict.domain.vo.DictVO;
import com.hunyuan.sa.base.module.support.dict.service.DictService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定稳定字典 Facade 与历史字典服务之间的读取映射。
 */
@ExtendWith(MockitoExtension.class)
class PlatformDictionaryApplicationServiceTest {

    @Mock
    private DictService dictService;

    private PlatformDictionaryApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PlatformDictionaryApplicationService();
        ReflectionTestUtils.setField(service, "dictService", dictService);
    }

    @Test
    void mapsPageQueryAndResultToStableContract() {
        DictVO legacyItem = new DictVO();
        legacyItem.setDictId(8L);
        legacyItem.setDictCode("gender");
        legacyItem.setDictName("性别");
        PageResult<DictVO> legacyResult = new PageResult<>();
        legacyResult.setPageNum(1L);
        legacyResult.setPageSize(20L);
        legacyResult.setTotal(1L);
        legacyResult.setPages(1L);
        legacyResult.setEmptyFlag(false);
        legacyResult.setList(List.of(legacyItem));
        when(dictService.queryPage(org.mockito.ArgumentMatchers.any(DictQueryForm.class))).thenReturn(legacyResult);

        PlatformDictionaryPageQuery query = new PlatformDictionaryPageQuery();
        query.setPageNum(1L);
        query.setPageSize(20L);
        query.setKeywords("性别");
        PageResult<PlatformDictionarySummary> result = service.queryPage(query);

        ArgumentCaptor<DictQueryForm> captor = ArgumentCaptor.forClass(DictQueryForm.class);
        verify(dictService).queryPage(captor.capture());
        assertThat(captor.getValue().getKeywords()).isEqualTo("性别");
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).singleElement().satisfies(item -> {
            assertThat(item.getDictId()).isEqualTo(8L);
            assertThat(item.getDictCode()).isEqualTo("gender");
        });
    }

    @Test
    void mapsDictionaryItemsWithoutExposingLegacyValueObject() {
        DictDataVO legacyItem = new DictDataVO();
        legacyItem.setDictDataId(9L);
        legacyItem.setDictId(8L);
        legacyItem.setDataValue("1");
        legacyItem.setDataLabel("男");
        when(dictService.queryDictData(8L)).thenReturn(List.of(legacyItem));

        List<PlatformDictionaryItem> items = service.getItemsByDictionaryId(8L);

        assertThat(items).singleElement().satisfies(item -> {
            assertThat(item.getDictDataId()).isEqualTo(9L);
            assertThat(item.getDataLabel()).isEqualTo("男");
        });
    }

    @Test
    void delegatesCreateToLegacyWritePath() {
        PlatformDictionaryCreateCommand command = new PlatformDictionaryCreateCommand();
        command.setDictName("业务类型");
        command.setDictCode("BUSINESS_TYPE");
        when(dictService.add(org.mockito.ArgumentMatchers.any(DictAddForm.class)))
                .thenReturn(com.hunyuan.sa.base.common.domain.ResponseDTO.ok());

        com.hunyuan.sa.base.common.domain.ResponseDTO<String> response = service.create(command);

        ArgumentCaptor<DictAddForm> captor = ArgumentCaptor.forClass(DictAddForm.class);
        verify(dictService).add(captor.capture());
        assertThat(captor.getValue().getDictCode()).isEqualTo("BUSINESS_TYPE");
        assertThat(response.getOk()).isTrue();
    }

    @Test
    void delegatesUpdateWithPathIdentifierToLegacyWritePath() {
        PlatformDictionaryUpdateCommand command = new PlatformDictionaryUpdateCommand();
        command.setDictName("业务类型");
        command.setDictCode("BUSINESS_TYPE");
        when(dictService.update(org.mockito.ArgumentMatchers.any(DictUpdateForm.class)))
                .thenReturn(com.hunyuan.sa.base.common.domain.ResponseDTO.ok());

        com.hunyuan.sa.base.common.domain.ResponseDTO<String> response = service.update(8L, command);

        ArgumentCaptor<DictUpdateForm> captor = ArgumentCaptor.forClass(DictUpdateForm.class);
        verify(dictService).update(captor.capture());
        assertThat(captor.getValue().getDictId()).isEqualTo(8L);
        assertThat(response.getOk()).isTrue();
    }

    @Test
    void delegatesStateAndDeleteOperationsToLegacyWritePath() {
        when(dictService.updateDisabled(8L)).thenReturn(com.hunyuan.sa.base.common.domain.ResponseDTO.ok());
        when(dictService.batchDelete(List.of(8L, 9L))).thenReturn(com.hunyuan.sa.base.common.domain.ResponseDTO.ok());
        when(dictService.delete(8L)).thenReturn(com.hunyuan.sa.base.common.domain.ResponseDTO.ok());

        assertThat(service.toggleDisabled(8L).getOk()).isTrue();
        assertThat(service.batchDelete(List.of(8L, 9L)).getOk()).isTrue();
        assertThat(service.delete(8L).getOk()).isTrue();

        verify(dictService).updateDisabled(8L);
        verify(dictService).batchDelete(List.of(8L, 9L));
        verify(dictService).delete(8L);
    }

    @Test
    void delegatesDictionaryItemMutationsAndPreservesPathIdentifiers() {
        PlatformDictionaryItemCreateCommand createCommand = new PlatformDictionaryItemCreateCommand();
        createCommand.setDataValue("Y");
        createCommand.setDataLabel("启用");
        createCommand.setSortOrder(10);
        PlatformDictionaryItemUpdateCommand updateCommand = new PlatformDictionaryItemUpdateCommand();
        updateCommand.setDataValue("N");
        updateCommand.setDataLabel("禁用");
        updateCommand.setSortOrder(5);
        updateCommand.setDictCode("SYS_STATUS");
        when(dictService.addDictData(org.mockito.ArgumentMatchers.any(DictDataAddForm.class)))
                .thenReturn(com.hunyuan.sa.base.common.domain.ResponseDTO.ok());
        when(dictService.updateDictData(org.mockito.ArgumentMatchers.any(DictDataUpdateForm.class)))
                .thenReturn(com.hunyuan.sa.base.common.domain.ResponseDTO.ok());
        when(dictService.updateDictDataDisabled(11L)).thenReturn(com.hunyuan.sa.base.common.domain.ResponseDTO.ok());
        when(dictService.batchDeleteDictData(List.of(11L, 12L)))
                .thenReturn(com.hunyuan.sa.base.common.domain.ResponseDTO.ok());
        when(dictService.deleteDictData(11L)).thenReturn(com.hunyuan.sa.base.common.domain.ResponseDTO.ok());

        assertThat(service.createItem(8L, createCommand).getOk()).isTrue();
        assertThat(service.updateItem(8L, 11L, updateCommand).getOk()).isTrue();
        assertThat(service.toggleItemDisabled(11L).getOk()).isTrue();
        assertThat(service.batchDeleteItems(List.of(11L, 12L)).getOk()).isTrue();
        assertThat(service.deleteItem(11L).getOk()).isTrue();

        ArgumentCaptor<DictDataAddForm> addCaptor = ArgumentCaptor.forClass(DictDataAddForm.class);
        verify(dictService).addDictData(addCaptor.capture());
        assertThat(addCaptor.getValue().getDictId()).isEqualTo(8L);
        ArgumentCaptor<DictDataUpdateForm> updateCaptor = ArgumentCaptor.forClass(DictDataUpdateForm.class);
        verify(dictService).updateDictData(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getDictId()).isEqualTo(8L);
        assertThat(updateCaptor.getValue().getDictDataId()).isEqualTo(11L);
    }
}
