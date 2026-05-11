package com.example.mahjong.common.constant;

// 房间、轮次、提交状态等字符串常量集中放在这里，避免各处手写字符串导致拼错。
public final class RoomConstants {

    // 房间等待中：还没有开始计分。
    public static final String ROOM_WAITING = "WAITING";
    // 房间计分中。
    public static final String ROOM_PLAYING = "PLAYING";
    // 房间已关闭。
    public static final String ROOM_CLOSED = "CLOSED";

    // 当前轮正在收集各玩家提交。
    public static final String ROUND_SUBMITTING = "SUBMITTING";
    // 当前轮已经结算完成。
    public static final String ROUND_SETTLED = "SETTLED";
    // 历史轮次逻辑删除。
    public static final String ROUND_DELETED = "DELETED";

    // 玩家还未提交。
    public static final String SUBMIT_PENDING = "PENDING";
    // 玩家本人已提交。
    public static final String SUBMIT_SUBMITTED = "SUBMITTED";
    // 房主代玩家提交。
    public static final String SUBMIT_OWNER_SUBMITTED = "OWNER_SUBMITTED";
    // 房主强制该玩家本轮不输不赢。
    public static final String SUBMIT_FORCED_SUBMITTED = "FORCED_SUBMITTED";

    // 房主主动关闭房间。
    public static final String CLOSE_OWNER = "OWNER_CLOSE";
    // 系统定时任务自动关闭房间。
    public static final String CLOSE_AUTO = "AUTO_CLOSE";

    // 工具类不允许实例化。
    private RoomConstants() {
    }
}
