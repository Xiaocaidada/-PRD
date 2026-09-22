package com.booth.interceptor;

import com.booth.common.Result;
import com.booth.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/**
 * 登录拦截器：校验 Authorization: Bearer <token>
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return reject(response, "未登录或登录已过期");
        }
        String token = auth.substring(7);
        if (jwtUtil.parse(token) == null) {
            return reject(response, "未登录或登录已过期");
        }
        request.setAttribute("userId", jwtUtil.getId(token));
        request.setAttribute("role", jwtUtil.getRole(token));
        return true;
    }

    private boolean reject(HttpServletResponse response, String message) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(MAPPER.writeValueAsString(Result.fail(401, message))
                .getBytes(StandardCharsets.UTF_8));
        return false;
    }
}
