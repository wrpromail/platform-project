package net.coding.lib.project.exception;

import net.coding.lib.project.exception.AppException;

public class ProjectSettingInvalidCodeException extends AppException {
    @Override
    public int getCode() {
        return -1;
    }

    @Override
    public String getKey() {
        return "project_setting_invalid_code";
    }
}
