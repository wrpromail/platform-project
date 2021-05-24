package net.coding.lib.project.enums;

import java.util.Arrays;

public enum ConnStateEnums {

    Default(0), Success(1), Fail(2);

    private int value;

    public int value() {
        return this.value;
    }

    ConnStateEnums(int value) {
        this.value = value;
    }

    public static ConnStateEnums of(int value) {
        return Arrays.stream(ConnStateEnums.values())
                .filter(c -> c.value == value)
                .findFirst()
                .orElse(Default);
    }

    public static ConnStateEnums nameOf(String name) {
        return Arrays.stream(ConnStateEnums.values())
                .filter(c -> c.name().equals(name))
                .findFirst()
                .orElse(Default);
    }
}
