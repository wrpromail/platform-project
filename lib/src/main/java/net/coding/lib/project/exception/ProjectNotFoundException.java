package net.coding.lib.project.exception;

public class ProjectNotFoundException extends AppException {
    @Override
    public int getCode() {
        return 1100;
    }

    @Override
    public String getKey() {
        return "project_not_exists";
    }
}
