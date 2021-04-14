package net.coding.lib.project.enums;

import java.util.Arrays;

public enum ProjectPreferenceEnum {
    PREFERENCE_TYPE_PROJECT_TWEET("1"),
    PREFERENCE_TYPE_UNPROTECTED_BRANCH_MERGE_REQUEST("2"),
    PREFERENCE_TYPE_SERVICE_HOOK_ENABLED("10"); //历史数据中含有1-9的数据，所以新建的类型从10开始
    private String code;

    public String getCode() {
        return this.code;
    }

    ProjectPreferenceEnum(String value) {
        this.code = value;
    }

    public static ProjectPreferenceEnum of(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElse(null);
    }

}
