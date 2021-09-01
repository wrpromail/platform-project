package net.coding.lib.project.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PmTypeEnums {
    PROJECT(0),
    PROGRAM(1);

    private int type;

}
