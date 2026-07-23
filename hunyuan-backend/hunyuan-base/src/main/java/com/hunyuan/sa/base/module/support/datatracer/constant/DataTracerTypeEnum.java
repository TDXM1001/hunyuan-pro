package com.hunyuan.sa.base.module.support.datatracer.constant;


import lombok.AllArgsConstructor;
import lombok.Getter;
import com.hunyuan.sa.base.common.enumeration.BaseEnum;

/**
 * 数据业务类型
 *
 * @Author 1024创新实验室-主任: 卓大
 * @Date 2022-07-23 19:38:52-
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright  <a href="https://1024lab.net">1024创新实验室</a>
 */
@AllArgsConstructor
@Getter
public enum DataTracerTypeEnum implements BaseEnum {

    /**
     * 商品
     */
    GOODS(1, "商品"),

    ;

    private final Integer value;

    private final String desc;
}
