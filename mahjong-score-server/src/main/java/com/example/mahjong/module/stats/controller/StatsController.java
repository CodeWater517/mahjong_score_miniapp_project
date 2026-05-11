package com.example.mahjong.module.stats.controller;

import com.example.mahjong.common.api.ApiResponse;
import com.example.mahjong.common.security.UserContext;
import com.example.mahjong.module.stats.dto.StatsDtos;
import com.example.mahjong.module.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stats")
// 战绩统计接口控制器。
public class StatsController {

    private final StatsService statsService;

    // 当前用户个人战绩。range 可选 ALL、MONTH、WEEK。
    @GetMapping("/me")
    public ApiResponse<StatsDtos.MyStatsResponse> myStats(@RequestParam(defaultValue = "ALL") String range) {
        return ApiResponse.success(statsService.getMyStats(UserContext.requireUserId(), range));
    }
}
