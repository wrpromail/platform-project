package net.coding.lib.project.template;

import java.util.Arrays;
import java.util.Optional;

/**
 * 项目模版
 */
public enum ProjectTemplateType {
    DEV_OPS, // devOps
    DEMO_BEGIN, // 范例项目
    CHOICE_DEMAND, // 按需选择
    PROJECT_MANAGE, // 项目管理
    CODE_HOST; // 代码托管


    public static ProjectTemplateType valueFrom(String value) {
        return Arrays.stream(ProjectTemplateType.values())
                .filter(t -> Optional.ofNullable(value).map(s -> s.toUpperCase().equals(t.name())).orElse(false))
                .findFirst()
                .orElse(null);

    }
}
