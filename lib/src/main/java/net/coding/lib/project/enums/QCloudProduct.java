package net.coding.lib.project.enums;

import org.apache.commons.lang.StringUtils;

import java.util.Arrays;

public enum QCloudProduct {
    TKE,TCB,SLS;


    public static QCloudProduct resolve(String label) {
        if (StringUtils.isBlank(label)) {
            return null;
        }
        return Arrays.stream(values()).filter(value -> value.toString().equalsIgnoreCase(label))
                .findFirst()
                .orElse(null);
    }

}

