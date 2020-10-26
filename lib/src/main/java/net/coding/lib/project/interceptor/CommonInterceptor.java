package net.coding.lib.project.interceptor;

import net.coding.lib.project.common.SystemContextHolder;
import net.coding.lib.project.grpc.client.UserGrpcClient;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.platform.user.UserProto;

@Slf4j
@Component
@AllArgsConstructor
public class CommonInterceptor extends HandlerInterceptorAdapter {

    public static final String GATEWAY_HEADER_USER_ID = "CODING-USER-ID";

    private final UserGrpcClient userGrpcClient;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        int userId = NumberUtils.toInt(request.getHeader(GATEWAY_HEADER_USER_ID));
        if (userId > 0) {
            UserProto.User user = userGrpcClient.getUserById(userId);
            SystemContextHolder.set(user);
        }
        return super.preHandle(request, response, handler);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        SystemContextHolder.remove();
    }

}
