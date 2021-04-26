package net.coding.lib.project.exception;

public class ProjectAuthTokenExpiredException extends AppException {
    @Override
    public int getCode() {
        return 9006;
    }

    @Override
    public String getKey() {
        return "project_auth_token_expired";
    }
}
