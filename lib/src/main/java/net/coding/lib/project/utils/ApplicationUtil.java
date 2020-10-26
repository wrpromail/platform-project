package net.coding.lib.project.utils;

import net.coding.common.util.ApplicationHelper;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class ApplicationUtil implements ApplicationContextAware, EnvironmentAware {

    private static Environment env;

    private static ApplicationContext context;

    public static Object get(String bean) {
        return context.getBean(bean);
    }

    public static <T> T get(Class<T> type) {
        return context.getBean(type);
    }

    public static ConcurrentMap<String, String> envMap = new ConcurrentHashMap<>(1 << 8);

    @Deprecated
    public static String env(String name) {
        if (!envMap.containsKey(name)) {
            String value = env.getProperty(name);
            if (value == null) {
                return null;
            }
            envMap.put(name, value);
        }
        return envMap.get(name);
    }

    @Deprecated
    /**
     * still slow for not exist env value
     */
    public static String env(String name, String defaultValue) {
        if (!envMap.containsKey(name)) {
            String value = env.getProperty(name);
            if (value == null) {
                return defaultValue;
            }
            envMap.put(name, value);
        }
        return envMap.get(name);
    }

    @Deprecated
    public static int env(String name, int defaultValue) {
        return env.getProperty(name, Integer.class, defaultValue);
    }

    @Deprecated
    public static boolean env(String name, boolean defaultValue) {
        return env.getProperty(name, Boolean.class, defaultValue);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        env = environment;
    }

    public static String internalInterfaceCode(String str, String time) {
        return DigestUtils.md5Hex(
                str + time + ApplicationHelper.env("internal_interface_salt")
        ).substring(20, 30);
    }

    public static String hostWithProtocol() {
        return ApplicationHelper.env("host_with_protocol");
    }
}
