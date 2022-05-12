package net.coding.lib.project.enums;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public enum ProjectLabelEnums {
    TKE, TCB, SLS, APIGW;


    public static ProjectLabelEnums resolve(String label) {
        if (StringUtils.isBlank(label)) {
            return null;
        }
        return Arrays.stream(values()).filter(value -> value.toString().equalsIgnoreCase(label))
                .findFirst()
                .orElse(null);
    }

}

