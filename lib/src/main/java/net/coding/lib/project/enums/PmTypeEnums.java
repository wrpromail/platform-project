package net.coding.lib.project.enums;

import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PmTypeEnums {
    PROJECT(0),
    PROGRAM(1);

    private int type;

    public static PmTypeEnums of(int type) {
        return Arrays.stream(values())
                .filter(value -> value.type == type)
                .findFirst()
                .orElse(null);
    }
}
