package net.coding.lib.project.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ch.qos.logback.classic.ClassicConstants;
import ch.qos.logback.classic.helpers.MDCInsertingServletFilter;

/**
 * Http工具类
 */
public class HttpKit {

    /**
     * 获取response
     */
    public static HttpServletResponse getResponse() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
    }

    /**
     * 获取request
     */
    public static HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    public static ServletRequestAttributes getServletRequestAttributes() {
        return (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    }

    /**
     * 根据 MDC 中的 xForwardedFor 获取 ip
     * <p>
     * 环境中需要注入
     *
     * @see MDCInsertingServletFilter
     */
    public static String getMdcIp() {
        String xForwardedFor = MDC.get(ClassicConstants.REQUEST_X_FORWARDED_FOR);
        if (xForwardedFor != null && xForwardedFor.length() > 15) {
            String[] ips = StringUtils.split(xForwardedFor, ",");
            return Arrays.stream(ips)
                    .filter(s -> !("unknown".equalsIgnoreCase(s)))
                    .findFirst()
                    .orElse("");
        }
        return Optional.ofNullable(getIp())
                .orElse("");

    }


    public static String getIp() {
        return getIpAddress(getRequest());
    }

    public static String getUA() {
        return Optional.ofNullable(getRequest().getHeader(HttpHeaders.USER_AGENT))
                .orElse("");
    }

    /**
     * 获取 MDC 中的 userAgent
     * <p>
     * 环境中需要注入
     *
     * @see MDCInsertingServletFilter
     */
    public static String getMdcUA() {
        return Optional.ofNullable(MDC.get(ClassicConstants.REQUEST_USER_AGENT_MDC_KEY))
                .orElseGet(() -> Optional.ofNullable(getServletRequestAttributes())
                        .map(ServletRequestAttributes::getRequest)
                        .map(p -> p.getHeader(HttpHeaders.USER_AGENT))
                        .orElse(""));
    }

    public static String getIpAddress(HttpServletRequest request) {
        // 获取请求主机IP地址,如果通过代理进来，则透过防火墙获取真实IP地址
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
        } else if (ip.length() > 15) {
            String[] ips = ip.split(",");
            for (String s : ips) {
                if (!("unknown".equalsIgnoreCase((String) s))) {
                    ip = (String) s;
                    break;
                }
            }
        }
        return ip;
    }

}
