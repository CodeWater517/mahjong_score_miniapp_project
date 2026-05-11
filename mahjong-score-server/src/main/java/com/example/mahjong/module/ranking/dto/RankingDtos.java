package com.example.mahjong.module.ranking.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

// 排行榜模块 DTO 集合。
public final class RankingDtos {

    // 只作为静态内部类容器，不允许实例化。
    private RankingDtos() {
    }

    @Data
    // 排行榜中的一行。
    public static class RankItem {
        // 名次；同分时名次相同。
        private Integer rank;
        // 用户 ID。
        private Long userId;
        // 昵称。
        private String nickname;
        // 头像。
        private String avatarUrl;
        // 排行用分数。
        private Integer score;
        // 参与局数，月榜暂时可为空。
        private Integer rounds;
    }

    @Data
    // 首页排行榜响应：总榜和月榜一起返回，减少前端请求次数。
    public static class HomeRankingResponse {
        private List<RankItem> totalRank = new ArrayList<>();
        private List<RankItem> monthlyRank = new ArrayList<>();
    }
}
