package net.coding.lib.project.exception;

public class PermissionDenyException extends AppException {
    @Override
    public int getCode() {
        return 1400;
    }

    @Override
    public String getKey() {
        return "permission_denied";
    }
}
