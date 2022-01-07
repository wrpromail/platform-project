package net.coding.lib.project.exception;

public class ProjectGroupAlreadyInGroupException extends AppException{
    @Override
    public int getCode() {
        return 2001;
    }

    @Override
    public String getKey() {
        return "project_already_in_group";
    }
}
