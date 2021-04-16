package net.coding.lib.project.exception;

public class ProjectAuthTokenDisabledException extends AppException {
    @Override
    public int getCode() {
        return 9005;
    }

    @Override
    public String getKey() {
        return "project_auth_token_disabled";
    }
}
