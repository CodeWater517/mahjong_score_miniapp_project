package com.example.mahjong.module.round.controller;

import com.example.mahjong.common.api.ApiResponse;
import com.example.mahjong.common.security.UserContext;
import com.example.mahjong.module.round.dto.RoundDtos;
import com.example.mahjong.module.round.service.RoundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rounds")
// 单局接口控制器：负责本轮提交、房主代交、历史局详情和修改。
public class RoundController {

    private final RoundService roundService;

    // 普通玩家提交自己的输分记录。
    @PostMapping("/{roundId}/submit")
    public ApiResponse<RoundDtos.SubmitRoundResponse> submit(@PathVariable Long roundId, @Valid @RequestBody RoundDtos.SubmitRoundRequest request) {
        return ApiResponse.success(roundService.submitRound(UserContext.requireUserId(), roundId, request));
    }

    // 房主给未提交玩家代提交。
    @PostMapping("/{roundId}/owner-submit")
    public ApiResponse<RoundDtos.SubmitRoundResponse> ownerSubmit(@PathVariable Long roundId, @Valid @RequestBody RoundDtos.OwnerSubmitRequest request) {
        return ApiResponse.success(roundService.ownerSubmit(UserContext.requireUserId(), roundId, request));
    }

    // 房主把未提交玩家强制设置为不输不赢。
    @PostMapping("/{roundId}/force-neutral")
    public ApiResponse<RoundDtos.SubmitRoundResponse> forceNeutral(@PathVariable Long roundId, @Valid @RequestBody RoundDtos.ForceNeutralRequest request) {
        return ApiResponse.success(roundService.forceNeutral(UserContext.requireUserId(), roundId, request));
    }

    // 查询某一局详情。
    @GetMapping("/{roundId}")
    public ApiResponse<RoundDtos.RoundDetailResponse> detail(@PathVariable Long roundId) {
        return ApiResponse.success(roundService.getDetail(roundId));
    }

    // 房主修改历史局输分明细，后端会重算房间统计。
    @PutMapping("/{roundId}")
    public ApiResponse<Void> updateHistory(@PathVariable Long roundId, @Valid @RequestBody RoundDtos.UpdateHistoryRoundRequest request) {
        roundService.updateHistoryRound(UserContext.requireUserId(), roundId, request);
        return ApiResponse.success();
    }

    // 房主删除历史局，使用逻辑删除并触发重算。
    @DeleteMapping("/{roundId}")
    public ApiResponse<Void> deleteHistory(@PathVariable Long roundId) {
        roundService.deleteHistoryRound(UserContext.requireUserId(), roundId);
        return ApiResponse.success();
    }
}
