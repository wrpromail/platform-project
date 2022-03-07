package net.coding.lib.project.credential.enums;

import java.util.Arrays;

public enum CredentialState {

    Default(0), Success(1), Fail(2);

    private final int value;

    public int value() {
        return this.value;
    }

    CredentialState(int value) {
        this.value = value;
    }

    public static CredentialState of(int value) {
        return Arrays.stream(CredentialState.values())
                .filter(c -> c.value == value)
                .findFirst()
                .orElse(Default);
    }

    public static CredentialState nameOf(String name) {
        return Arrays.stream(CredentialState.values())
                .filter(c -> c.name().equals(name))
                .findFirst()
                .orElse(Default);
    }
}
