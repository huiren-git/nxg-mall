package com.nxg.mall.common.annotation;

import com.nxg.mall.common.enums.ActionType;
import com.nxg.mall.common.enums.TargetType;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
    /** 模块名称或操作描述 (如：修改商品价格) */
    String title() default "";

    /** 业务操作类型 */
    ActionType actionType();

    /** 操作对象类型 */
    TargetType targetType();

    /**
     * 是否保存请求参数
     * 有些参数可能包含敏感信息或数据量巨大（如Base64图片），可以选择关闭
     */
    boolean isSaveRequestData() default true;
}
