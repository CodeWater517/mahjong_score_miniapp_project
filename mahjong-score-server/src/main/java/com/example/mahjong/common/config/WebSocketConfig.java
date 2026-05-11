package com.example.mahjong.common.config;

import com.example.mahjong.websocket.RoomWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
// WebSocket 配置：把房间实时通知接口注册到 /ws/room。
public class WebSocketConfig implements WebSocketConfigurer {

    private final RoomWebSocketHandler roomWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 同一个 handler 既负责握手拦截，也负责消息处理。
        registry.addHandler(roomWebSocketHandler, "/ws/room")
            .addInterceptors(roomWebSocketHandler)
            // 小程序端来源可能不同，MVP 阶段先放开跨域来源。
            .setAllowedOrigins("*");
    }
}
