package net.coding.lib.project.enums;

/**
 * @author Jyong <jiangyong@coding.net> on @date 2021/6/18
 */
public enum ScopeTypeEnum {

    PROJECT(1),

    TEAM(2);

    private final Integer value;

    ScopeTypeEnum(Integer value) {
        this.value = value;
    }

    public Integer value() {
        return value;
    }
}
