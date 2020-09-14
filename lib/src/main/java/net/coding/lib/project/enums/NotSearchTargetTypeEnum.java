package net.coding.lib.project.enums;

import java.util.ArrayList;
import java.util.List;

/**
 * 暂不允许在关联资源搜索到
 */
public enum NotSearchTargetTypeEnum {
    TestingCase,
    TestingRun,
    Testing,
    TestingReport;

    public static List<String> getTargetTypes() {
        List<String> allTypes = new ArrayList<>();
        for (NotSearchTargetTypeEnum type : NotSearchTargetTypeEnum.values()) {
            allTypes.add(type.toString());
        }
        return allTypes;
    }
}
