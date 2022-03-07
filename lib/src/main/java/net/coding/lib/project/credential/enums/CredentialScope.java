package net.coding.lib.project.credential.enums;

import java.util.Arrays;

/**
 * Project 是项目范围内公开，Private 是个人私有
 */
public enum CredentialScope {

    PROJECT(1), PRIVATE(2);

    private final int code;

    public int getCode() {
        return this.code;
    }

    CredentialScope(int value) {
        this.code = value;
    }

    public static CredentialScope of(int code) {
        return Arrays.stream(CredentialScope.values())
                .filter(s -> s.getCode() == code)
                .findFirst()
                .orElse(PROJECT);
    }
}
