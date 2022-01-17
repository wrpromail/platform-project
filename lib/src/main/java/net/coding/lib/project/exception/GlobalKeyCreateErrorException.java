package net.coding.lib.project.exception;

public class GlobalKeyCreateErrorException  extends AppException{
    @Override
    public int getCode() {
        return 1231;
    }

    @Override
    public String getKey() {
        return "global_key_create_error";
    }
}
