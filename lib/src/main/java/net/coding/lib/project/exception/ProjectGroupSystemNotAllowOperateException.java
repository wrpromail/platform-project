package net.coding.lib.project.exception;

public class ProjectGroupSystemNotAllowOperateException extends AppException {
    @Override
    public int getCode() {
        return 2005;
    }

    @Override
    public String getKey() {
        return "project_group_system_not_allow_operate";
    }
}
