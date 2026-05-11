package com.example.mahjong.module.ranking.controller;

import com.example.mahjong.common.api.ApiResponse;
import com.example.mahjong.module.ranking.dto.RankingDtos;
import com.example.mahjong.module.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
// 排行榜接口控制器：房间榜和首页榜都从这里对外提供。
public class RankingController {

    private final RankingService rankingService;

    // 单个房间内的玩家排行。
    @GetMapping("/api/rooms/{roomId}/rank")
    public ApiResponse<List<RankingDtos.RankItem>> roomRank(@PathVariable Long roomId) {
        return ApiResponse.success(rankingService.getRoomRank(roomId));
    }

    // 首页排行榜：历史总榜和月榜。
    @GetMapping("/api/rankings/home")
    public ApiResponse<RankingDtos.HomeRankingResponse> homeRank() {
        return ApiResponse.success(rankingService.getHomeRanking());
    }
}
