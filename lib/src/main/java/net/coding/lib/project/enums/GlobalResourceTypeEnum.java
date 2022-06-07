package net.coding.lib.project.enums;

import java.util.Arrays;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

/**
 * @author Jyong <jiangyong@coding.net> on @date 2021/6/18
 */
public enum GlobalResourceTypeEnum {

    KNOWLEDGE_MANAGE("Knowledge"),
    GLOBAL_REQUIREMENT("GlobalRequirement");

    private final String value;

    GlobalResourceTypeEnum(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static GlobalResourceTypeEnum valueFrom(String type) {
        return Arrays.stream(values())
                .filter(e -> equalsIgnoreCase(e.value, type))
                .findFirst()
                .orElse(null);
    }
}
