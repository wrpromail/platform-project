package net.coding.lib.project.template;

import java.util.Arrays;
import java.util.Optional;

/**
 * 范例项目类型
 */
public enum ProjectTemplateDemoType {

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


    public static ProjectTemplateDemoType valueFrom(String value) {
        return Arrays.stream(ProjectTemplateDemoType.values())
                .filter(t -> Optional.ofNullable(value).map(s -> s.toUpperCase().equals(t.name())).orElse(false))
                .findFirst()
                .orElse(null);

    }
}
