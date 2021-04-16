package net.coding.lib.project.enums;

import java.util.Arrays;

/**
 * Project 是项目范围内公开，Private 是个人私有
 */
public enum CredentialScopeEnums {

    PROJECT(1), PRIVATE(2);

    private int code;

    public int getCode() {
        return this.code;
    }

    CredentialScopeEnums(int value) {
        this.code = value;
    }

    public static CredentialScopeEnums of(int code) {
        return Arrays.stream(CredentialScopeEnums.values())
                .filter(s -> s.getCode() == code)
                .findFirst()
                .orElse(PROJECT);
    }
}
