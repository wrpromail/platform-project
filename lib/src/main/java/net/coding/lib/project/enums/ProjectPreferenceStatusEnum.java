package net.coding.lib.project.enums;

import java.util.Arrays;

public enum ProjectPreferenceStatusEnum {
    PREFERENCE_STATUS_FALSE("0"),
    PREFERENCE_STATUS_TRUE("1");

    private String code;

    public String getCode() {
        return this.code;
    }

    ProjectPreferenceStatusEnum(String value) {
        this.code = value;
    }

    public static ProjectPreferenceStatusEnum of(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElse(null);
    }

}
