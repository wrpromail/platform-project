package net.coding.lib.project.exception;

public class ProjectAuthDenyException extends AppException {
    @Override
    public int getCode() {
        return 9004;
    }

    @Override
    public String getKey() {
        return "project_auth_deny";
    }
}
