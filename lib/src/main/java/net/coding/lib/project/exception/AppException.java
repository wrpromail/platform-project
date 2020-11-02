package net.coding.lib.project.exception;

import java.util.Map;

public abstract class AppException extends RuntimeException {
    public abstract int getCode();

    public abstract String getKey();

    public Map<String, String> getData() {
        return null;
    }

    public Object[] getArguments() {
        return new Object[0];
    }
}
