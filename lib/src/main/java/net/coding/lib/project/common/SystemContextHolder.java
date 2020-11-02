package net.coding.lib.project.common;


import proto.platform.user.UserProto;
import proto.platform.user.UserProto.User;

public class SystemContextHolder {

    private static final ThreadLocal<UserProto.User> threadLocal = new ThreadLocal<>();

    public static void set(User user) {
        threadLocal.set(user);
    }

    public static User get() {
        return threadLocal.get();
    }

    public static void remove() {
        threadLocal.remove();
    }
}
