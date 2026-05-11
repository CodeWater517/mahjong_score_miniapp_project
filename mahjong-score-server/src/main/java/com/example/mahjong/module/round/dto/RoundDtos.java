package com.example.mahjong.module.round.dto;

import com.example.mahjong.module.room.dto.RoomDtos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 单局计分模块 DTO 集合。
public final class RoundDtos {

    // 只作为静态内部类容器，不允许实例化。
    private RoundDtos() {
    }

    @Data
    // 普通提交时的一条输分记录：我输给谁多少分。
    public static class PaymentRequest {
        // 收分玩家 ID。
        @NotNull
        private Long toUserId;
        // 输分分值，必须大于等于 1。
        @NotNull
        @Min(1)
        private Integer score;
        // 可选备注。
        private String remark;
    }

    @Data
    // 普通玩家提交本轮请求。
    public static class SubmitRoundRequest {
        @Valid
        private List<PaymentRequest> payments = new ArrayList<>();
    }

    @Data
    // 提交后的响应，告诉前端本轮是否已经全员提交。
    public static class SubmitRoundResponse {
        private Long roundId;
        private String submitStatus;
        private Boolean allSubmitted;
    }

    @Data
    // 房主代提交请求。
    public static class OwnerSubmitRequest {
        // 被代交的玩家 ID。
        @NotNull
        private Long targetUserId;
        @Valid
        private List<PaymentRequest> payments = new ArrayList<>();
    }

    @Data
    // 房主强制某位未提交玩家不输不赢。
    public static class ForceNeutralRequest {
        @NotNull
        private Long targetUserId;
    }

    @Data
    // 修改历史局时的一条输分记录，需要同时指定输家和赢家。
    public static class HistoryPaymentRequest {
        @NotNull
        private Long fromUserId;
        @NotNull
        private Long toUserId;
        @NotNull
        @Min(1)
        private Integer score;
        private String remark;
    }

    @Data
    // 修改历史局请求：用传入列表整体替换该局原有输分明细。
    public static class UpdateHistoryRoundRequest {
        @Valid
        private List<HistoryPaymentRequest> payments = new ArrayList<>();
    }

    @Data
    // 单局结算摘要里的一个玩家结果。
    public static class RoundSummaryItem {
        private Long userId;
        private String nickname;
        private Integer netScore;
    }

    @Data
    // 历史局列表项。
    public static class RoundHistoryItem {
        private Long roundId;
        private Integer roundNo;
        private LocalDateTime settledAt;
        private List<RoundSummaryItem> summary = new ArrayList<>();
    }

    @Data
    // 历史局详情响应，包含摘要和完整输分明细。
    public static class RoundDetailResponse {
        private Long roundId;
        private Long roomId;
        private Integer roundNo;
        private String status;
        private List<RoomDtos.PaymentView> payments = new ArrayList<>();
        private List<RoundSummaryItem> summary = new ArrayList<>();
    }
}
