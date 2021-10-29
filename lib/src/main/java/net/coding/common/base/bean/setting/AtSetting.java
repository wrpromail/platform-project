package net.coding.common.base.bean.setting;

import net.coding.common.base.bean.Notification;

public class AtSetting extends BaseSetting {
    @Override
    public Short getType() {
        return Notification.TYPE_AT;
    }

    @Override
    public String getEmail_setting() {
        return "false";
    }

    @Override
    public String getNotification_setting() {
        return "true";
    }

    @Override
    public Short getFlag() {
        return MAIN;
    }
}
