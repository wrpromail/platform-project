package net.coding.lib.project.exception;

public class ProjectGroupSameNameException extends AppException{
    @Override
    public int getCode() {
        return 2000;
    }

    @Override
    public String getKey() {
        return "project_group_same_name";
    }
}
