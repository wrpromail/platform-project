package net.coding.lib.project.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class DesensitizationUtil {
    private final static Pattern CENTER = Pattern.compile("(^.{4})(.*)(.{4}$)");

    public static String de(String content) {
        return CENTER.matcher(content).replaceAll("$1******$3");
    }

    public static String comDe(String content) {
        if (StringUtils.isBlank(content)) return content;
        int i = content.length() / 3;
        return DesensitizationUtil.around(content, i, i);
    }

    public static String left(String str, int index) {
        if (StringUtils.isBlank(str)) {
            return "";
        }
        String name = StringUtils.left(str, index);
        return StringUtils.rightPad(name, StringUtils.length(str), "*");
    }


    public static String around(String str, int index, int end) {
        if (StringUtils.isBlank(str)) {
            return "";
        }
        return StringUtils.left(str, index).concat(StringUtils.removeStart(StringUtils.leftPad(StringUtils.right(str, end), StringUtils.length(str), "*"), "***"));
    }

    public static String right(String str, int end) {
        if (StringUtils.isBlank(str)) {
            return "";
        }
        return StringUtils.leftPad(StringUtils.right(str, end), StringUtils.length(str), "*");
    }

}
