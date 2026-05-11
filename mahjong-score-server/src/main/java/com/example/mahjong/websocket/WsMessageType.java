package com.example.mahjong.websocket;

// WebSocket 消息类型常量。前后端用同一批字符串判断要刷新什么。
public final class WsMessageType {

    // 心跳消息。
    public static final String PING = "PING";
    public static final String PONG = "PONG";
    // 房间、座位、房主等状态变化。
    public static final String ROOM_UPDATED = "ROOM_UPDATED";
    public static final String SEAT_UPDATED = "SEAT_UPDATED";
    public static final String OWNER_CHANGED = "OWNER_CHANGED";
    public static final String GAME_STARTED = "GAME_STARTED";
    // 当前轮提交和结算相关消息。
    public static final String ROUND_SUBMITTED = "ROUND_SUBMITTED";
    public static final String ROUND_SUBMIT_MODIFIED = "ROUND_SUBMIT_MODIFIED";
    public static final String ROUND_SETTLED = "ROUND_SETTLED";
    public static final String ROUND_RECALCULATED = "ROUND_RECALCULATED";
    // 房间关闭、重开、踢人等通知。
    public static final String ROOM_CLOSED = "ROOM_CLOSED";
    public static final String ROOM_REOPENED = "ROOM_REOPENED";
    public static final String USER_KICKED = "USER_KICKED";
    // 给前端展示错误或提醒的预留类型。
    public static final String ERROR_NOTICE = "ERROR_NOTICE";

    // 工具类不允许实例化。
    private WsMessageType() {
    }
}
