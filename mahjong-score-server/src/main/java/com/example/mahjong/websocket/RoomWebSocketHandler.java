package com.example.mahjong.websocket;

import com.example.mahjong.common.security.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Component
@RequiredArgsConstructor
// 房间 WebSocket 处理器：握手时校验 token 和 roomId，连接后处理心跳。
public class RoomWebSocketHandler extends TextWebSocketHandler implements HandshakeInterceptor {

    // WebSocketSession 的 attributes key，用来保存房间 ID。
    private static final String ATTR_ROOM_ID = "roomId";
    // WebSocketSession 的 attributes key，用来保存用户 ID。
    private static final String ATTR_USER_ID = "userId";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final RoomWebSocketSessionManager sessionManager;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // 握手 URL 形如 /ws/room?token=xxx&roomId=1。
        URI uri = request.getURI();
        MultiValueMap<String, String> params = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
        String token = params.getFirst("token");
        String roomIdText = params.getFirst("roomId");
        try {
            // 握手阶段校验 token，成功后把 userId 和 roomId 存进 session 属性。
            attributes.put(ATTR_USER_ID, jwtUtil.parseUserId(token));
            attributes.put(ATTR_ROOM_ID, Long.valueOf(roomIdText));
            return true;
        } catch (Exception ex) {
            // 返回 false 会拒绝 WebSocket 连接。
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 当前没有额外收尾逻辑，方法保留是因为实现了 HandshakeInterceptor。
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 连接建立后加入对应房间，后续房间变化会广播给它。
        sessionManager.add((Long) session.getAttributes().get(ATTR_ROOM_ID), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 当前客户端只会主动发 PING，服务端回 PONG 保持连接活跃。
        JsonNode node = objectMapper.readTree(message.getPayload());
        if (WsMessageType.PING.equals(node.path("type").asText())) {
            Long roomId = (Long) session.getAttributes().get(ATTR_ROOM_ID);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(WsMessage.of(WsMessageType.PONG, roomId, Map.of()))));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 连接关闭后从房间集合里移除，防止继续向失效连接发送消息。
        sessionManager.remove((Long) session.getAttributes().get(ATTR_ROOM_ID), session);
    }
}
