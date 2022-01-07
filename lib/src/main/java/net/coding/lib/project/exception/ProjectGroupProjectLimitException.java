package net.coding.lib.project.exception;

public class ProjectGroupProjectLimitException extends AppException{
    @Override
    public int getCode() {
        return 2002;
    }

    @Override
    public String getKey() {
        return "project_group_project_limit";
    }
}
