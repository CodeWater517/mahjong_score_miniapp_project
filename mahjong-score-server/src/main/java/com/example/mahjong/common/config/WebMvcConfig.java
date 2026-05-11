package com.example.mahjong.common.config;

import com.example.mahjong.common.security.PhoneBindInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
// Spring MVC 扩展配置：这里用来注册手机号绑定拦截器。
public class WebMvcConfig implements WebMvcConfigurer {

    private final PhoneBindInterceptor phoneBindInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 房间、轮次、统计、排行等业务接口要求用户已绑定手机号。
        registry.addInterceptor(phoneBindInterceptor)
            .addPathPatterns("/api/rooms/**", "/api/rounds/**", "/api/stats/**", "/api/rankings/**");
    }
}
