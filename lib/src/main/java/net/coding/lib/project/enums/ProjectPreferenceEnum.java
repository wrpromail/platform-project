package net.coding.lib.project.enums;

import java.util.Arrays;

public enum ProjectPreferenceEnum {
    PREFERENCE_TYPE_PROJECT_TWEET("1"),
    PREFERENCE_TYPE_UNPROTECTED_BRANCH_MERGE_REQUEST("2"),
    PREFERENCE_TYPE_SERVICE_HOOK_ENABLED("3");
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
