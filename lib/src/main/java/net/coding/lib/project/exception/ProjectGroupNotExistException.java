package net.coding.lib.project.exception;

public class ProjectGroupNotExistException extends AppException{
    @Override
    public int getCode() {
        return 2004;
    }

    @Override
    public String getKey() {
        return "project_group_not_exist";
    }
}
