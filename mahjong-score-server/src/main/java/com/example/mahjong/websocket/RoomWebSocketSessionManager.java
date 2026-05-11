package com.example.mahjong.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
// 房间 WebSocket 会话管理器：记录每个房间有哪些在线连接，并负责群发消息。
public class RoomWebSocketSessionManager {

    private final ObjectMapper objectMapper;
    // key 是 roomId，value 是该房间所有 WebSocketSession；ConcurrentHashMap 适合多线程访问。
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    // 用户进入房间 WebSocket 后，把连接加入该房间集合。
    public void add(Long roomId, WebSocketSession session) {
        roomSessions.computeIfAbsent(roomId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    // 连接关闭时移除；如果房间没有连接了，就清理整个集合。
    public void remove(Long roomId, WebSocketSession session) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            roomSessions.remove(roomId);
        }
    }

    // 给一个房间内所有在线客户端广播消息。
    public void broadcastToRoom(Long roomId, Object payload) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        try {
            // 先把业务对象序列化成 JSON 字符串，再作为文本消息发送。
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(payload));
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (IOException ex) {
            // WebSocket 只是通知通道；发送失败时客户端仍会通过快照轮询拿到最终状态。
        }
    }
}
