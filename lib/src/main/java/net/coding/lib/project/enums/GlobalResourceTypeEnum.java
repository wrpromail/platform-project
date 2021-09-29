package net.coding.lib.project.enums;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

/**
 * @author Jyong <jiangyong@coding.net> on @date 2021/6/18
 */
public enum GlobalResourceTypeEnum {

    KNOWLEDGE_MANAGE("Knowledge");

    private final String value;

    GlobalResourceTypeEnum(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static GlobalResourceTypeEnum valueFrom(String type) {

        if (equalsIgnoreCase(KNOWLEDGE_MANAGE.value, type)) {
            return KNOWLEDGE_MANAGE;
        }
        return null;
    }
}
