package com.example.mahjong.common.security;

import com.example.mahjong.common.api.ApiResponse;
import com.example.mahjong.common.api.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
// JWT 认证过滤器：每个请求进 Controller 前，先在这里解析当前用户。
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 登录、WebSocket 握手和接口文档不走这个过滤器。
        return path.equals("/api/auth/wechat-login")
            || path.startsWith("/ws/")
            || path.startsWith("/doc.html")
            || path.startsWith("/swagger-ui")
            || path.startsWith("/v3/api-docs")
            || path.startsWith("/webjars");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (!StringUtils.hasText(token)) {
                // 没有 token 时，直接返回未登录，不进入后续 Controller。
                writeUnauthorized(response, "请先登录");
                return;
            }
            // 解析成功后把 userId 放进 ThreadLocal，本次请求内随处可取。
            UserContext.setUserId(jwtUtil.parseUserId(token));
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            // token 解析失败、过期或格式错误都当作登录失效。
            writeUnauthorized(response, "登录已失效，请重新登录");
        } finally {
            // 请求结束必须清理 ThreadLocal，防止线程复用时串到其它请求。
            UserContext.clear();
        }
    }

    // token 可以放在 Authorization: Bearer xxx，也可以作为 WebSocket/调试场景的 token 参数。
    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return request.getParameter("token");
    }

    // 统一以 ApiResponse 格式返回未登录错误；HTTP 状态仍为 200，方便前端统一按 code 处理。
    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(ErrorCode.UNAUTHORIZED, message)));
    }
}
