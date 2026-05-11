package com.example.mahjong.module.stats.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 战绩统计模块 DTO 集合。
public final class StatsDtos {

    // 只作为静态内部类容器，不允许实例化。
    private StatsDtos() {
    }

    @Data
    // 个人战绩页响应。
    public static class MyStatsResponse {
        // 参与过的房间数。
        private Integer totalRooms;
        // 参与总局数。
        private Integer totalRounds;
        // 总净分。
        private Integer totalScore;
        // 单局平均净分。
        private Double avgRoundScore;
        // 最高单局净分。
        private Integer highestRoundScore;
        // 最低单局净分。
        private Integer lowestRoundScore;
        // 单局胜率，0 到 1。
        private Double roundWinRate;
        // 房间胜率，0 到 1。
        private Double roomWinRate;
        // 盈亏比，正分总和 / 负分绝对值总和。
        private String profitLossRatio;
        // 最近参与的房间。
        private List<RecentRoomItem> recentRooms = new ArrayList<>();
    }

    @Data
    // 最近房间列表项。
    public static class RecentRoomItem {
        private Long roomId;
        private String roomName;
        private String status;
        private Integer myScore;
        private Integer myRank;
        private Integer rounds;
        private LocalDateTime lastPlayTime;
    }
}
