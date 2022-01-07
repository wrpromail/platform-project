package net.coding.lib.project.exception;

public class ProjectGroupNameNullException extends AppException{
    @Override
    public int getCode() {
        return 2006;
    }

    @Override
    public String getKey() {
        return "project_group_name_null";
    }
}
