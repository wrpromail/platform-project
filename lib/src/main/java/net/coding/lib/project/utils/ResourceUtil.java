package net.coding.lib.project.utils;

import net.coding.common.i18n.utils.LocaleMessageSourceUtil;

import java.text.MessageFormat;

public class ResourceUtil {
    public static String ui(String key, Object... args) {
        String text = LocaleMessageSourceUtil.getMessageByBaseName("messages", key);
        return (text != null) ? MessageFormat.format(text, args) : null;
    }

    public static String error(String key, Object... args) {
        String text = LocaleMessageSourceUtil.getMessageByBaseName("messages", key);
        return (text != null) ? MessageFormat.format(text, args) : null;
    }
}
