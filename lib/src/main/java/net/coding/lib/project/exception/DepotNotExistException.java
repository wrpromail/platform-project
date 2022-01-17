package net.coding.lib.project.exception;

public class DepotNotExistException extends AppException{
    @Override
    public int getCode() {
        return 2311;
    }

    @Override
    public String getKey() {
        return "depot_not_exist";
    }
}
