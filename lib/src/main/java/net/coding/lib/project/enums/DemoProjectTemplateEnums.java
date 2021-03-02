package net.coding.lib.project.enums;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * 范例项目类型
 */
public enum DemoProjectTemplateEnums {

    AGILE,         // 敏捷项目管理
    CLASSIC,         // 经典项目管理
    TESTING,       // 测试管理项目
    MOBILE,        // 移动端研发项目

    TENCENT_SLS_VUE,
    TENCENT_SLS_DB,
    SPRING,
    ROR,
    SINATRA,
    NODEJS,
    ANDROID,
    FLASK,
    TENCENT_SLS_EXPRESS,
    TENCENT_SLS_FLASK,
    TENCENT_SLS_STATIC_WEBSITE;


    public static DemoProjectTemplateEnums string2enum(String s) {
        if (StringUtils.isEmpty(s)) {
            return null;
        }
        final String upper = s.toUpperCase();
        return Arrays.stream(DemoProjectTemplateEnums.values())
                .filter(t -> upper.equals(t.name()))
                .findFirst()
                .orElse(null);

    }
}
