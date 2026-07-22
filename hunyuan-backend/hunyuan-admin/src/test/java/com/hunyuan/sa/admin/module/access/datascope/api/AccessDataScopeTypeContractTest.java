package com.hunyuan.sa.admin.module.access.datascope.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccessDataScopeTypeContractTest {

    @Test
    void preservesDataScopeTypeValuesAndOrder() {
        assertThat(AccessDataScopeType.values())
                .extracting(
                        AccessDataScopeType::getValue,
                        AccessDataScopeType::getSort,
                        AccessDataScopeType::getName,
                        AccessDataScopeType::getDesc)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1, 20, "系统通知", "系统通知数据范围"),
                        org.assertj.core.groups.Tuple.tuple(2, 30, "组织目录", "组织目录部门数据范围"));
    }

    @Test
    void preservesViewTypeValuesAndLevels() {
        assertThat(AccessDataScopeViewType.values())
                .extracting(
                        AccessDataScopeViewType::getValue,
                        AccessDataScopeViewType::getLevel,
                        AccessDataScopeViewType::getDesc)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(0, 0, "本人"),
                        org.assertj.core.groups.Tuple.tuple(1, 5, "本部门"),
                        org.assertj.core.groups.Tuple.tuple(2, 10, "本部门及下属子部门"),
                        org.assertj.core.groups.Tuple.tuple(10, 100, "全部"));
    }
}
