package net.coding.lib.project.exception;

public class ProjectGroupNameTooLongException extends AppException{
    @Override
    public int getCode() {
        return 2003;
    }

    @Override
    public String getKey() {
        return "project_group_name_too_long";
    }
}
