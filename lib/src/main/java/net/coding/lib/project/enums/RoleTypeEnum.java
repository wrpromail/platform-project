package net.coding.lib.project.enums;

import java.util.Arrays;

public enum RoleTypeEnum {
    PROJECT_ADMIN(90),
    PROJECT_MEMBER(80);
    private int code;

    public int getCode() {
        return this.code;
    }

    RoleTypeEnum(int value) {
        this.code = value;
    }

    public static RoleTypeEnum of(int code) {
        return Arrays.stream(values())
                .filter(value -> value.code == code)
                .findFirst()
                .orElse(null);
    }

}
