package net.coding.lib.project.common;

public class GRpcMetadataContextHolder {
    private static final ThreadLocal<DeployTokenHeader> threadLocal = new ThreadLocal<>();

    public static void set(DeployTokenHeader deployTokenHeader) {
        threadLocal.set(deployTokenHeader);
    }

    public static DeployTokenHeader get() {
        return threadLocal.get();
    }

    public static void remove() {
        threadLocal.remove();
    }
}