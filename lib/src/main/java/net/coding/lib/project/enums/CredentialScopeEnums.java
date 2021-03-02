package net.coding.lib.project.enums;

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
        for (CredentialScopeEnums cs : CredentialScopeEnums.values()) {
            if (cs.code == code) {
                return cs;
            }
        }
        return PROJECT;
    }

    public static CredentialScopeEnums nameOf(String name) {
        for (CredentialScopeEnums cs : CredentialScopeEnums.values()) {
            if (cs.name().equals(name)) {
                return cs;
            }
        }
        return PROJECT;
    }

}
