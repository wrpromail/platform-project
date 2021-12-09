package net.coding.lib.project.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProjectMemberPrincipalTypeEnum {

    USER_GROUP(1),
    DEPARTMENT(2),
    USER(3);

    private int sort;

}
