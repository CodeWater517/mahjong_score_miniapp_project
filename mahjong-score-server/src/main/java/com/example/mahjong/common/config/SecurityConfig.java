package com.example.mahjong.common.config;

import com.example.mahjong.common.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
// Spring Security 配置：把 JWT 过滤器加入请求链。
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            // 小程序接口不使用传统浏览器表单 CSRF 防护。
            .csrf(AbstractHttpConfigurer::disable)
            // 后端不保存登录 Session，所有登录态都靠 JWT。
            .sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 具体是否登录由 JwtAuthFilter 判断，这里先允许请求进入过滤器链。
            .authorizeHttpRequests(registry -> registry.anyRequest().permitAll())
            // 在用户名密码过滤器之前解析 JWT。
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
