package com.example.mahjong.websocket;

import lombok.Data;

import java.time.LocalDateTime;

@Data
// WebSocket 统一消息格式，和 HTTP 的 ApiResponse 类似，也是前后端约定好的外壳。
public class WsMessage<T> {
    // 消息类型，取值看 WsMessageType。
    private String type;
    // 消息所属房间，前端收到后知道该刷新哪个房间。
    private Long roomId;
    // 生成消息的时间。
    private LocalDateTime timestamp = LocalDateTime.now();
    // 具体业务数据，不同消息类型结构不同。
    private T payload;

    // 快速创建消息对象的工厂方法。
    public static <T> WsMessage<T> of(String type, Long roomId, T payload) {
        WsMessage<T> message = new WsMessage<>();
        message.setType(type);
        message.setRoomId(roomId);
        message.setPayload(payload);
        return message;
    }
}
