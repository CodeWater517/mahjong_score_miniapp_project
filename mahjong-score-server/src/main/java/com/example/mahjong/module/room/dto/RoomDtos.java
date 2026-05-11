package com.example.mahjong.module.room.dto;

import com.example.mahjong.module.ranking.dto.RankingDtos;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 房间模块 DTO 集合，包含创建、加入、快照、座位、当前轮等接口对象。
public final class RoomDtos {

    // 只作为静态内部类容器，不允许实例化。
    private RoomDtos() {
    }

    @Data
    // 创建房间请求。
    public static class CreateRoomRequest {
        // 房间人数，限制 2 到 4。
        @NotNull
        @Min(2)
        @Max(4)
        private Integer playerCount;
        // 每个座位的初始分，默认 0。
        private Integer initialScore = 0;
    }

    @Data
    // 创建房间响应，返回前端进入等待页所需的信息。
    public static class CreateRoomResponse {
        private Long roomId;
        private String roomCode;
        private String roomName;
        private String status;
        private Long ownerUserId;
    }

    @Data
    // 加入房间请求，用户选择一个空座位，并可设置房间内昵称。
    public static class JoinRoomRequest {
        @NotNull
        private Long seatId;
        private String roomNickname;
    }

    @Data
    // 加入房间响应，包含继承到的座位分。
    public static class JoinRoomResponse {
        private Long roomId;
        private Long seatId;
        private String seatName;
        private Integer inheritedScore;
    }

    @Data
    // 按房间号查询房间响应，主要给加入房间页使用。
    public static class RoomCodeResponse {
        private Long roomId;
        private String roomCode;
        private String roomName;
        private String status;
        private List<SeatResponse> emptySeats = new ArrayList<>();
    }

    @Data
    // 座位展示对象，前端 SeatCard 直接使用它。
    public static class SeatResponse {
        // 座位 ID。
        private Long seatId;
        // 座位编码，例如 EAST。
        private String seatName;
        // 用户可读座位名，例如 东。
        private String displayName;
        // 当前座位上的用户 ID，为空表示空位。
        private Long currentUserId;
        // 用户全局昵称。
        private String nickname;
        // 用户在本房间的昵称。
        private String roomNickname;
        // 用户头像。
        private String avatarUrl;
        // 当前座位分。
        private Integer currentScore;
        // 是否空位。
        private Boolean empty;
        // 当前用户加入座位时间。
        private LocalDateTime joinedAt;
    }

    @Data
    // 房间快照响应：房间页面最核心的数据包。
    public static class RoomSnapshotResponse {
        private Long roomId;
        private String roomCode;
        private String roomName;
        private String status;
        private Long ownerUserId;
        private Integer playerCount;
        private Integer currentRoundNo;
        private Integer currentSubmittingRoundNo;
        private List<SeatResponse> seats = new ArrayList<>();
        private CurrentRoundResponse currentRound;
        private List<RankingDtos.RankItem> rankList = new ArrayList<>();
    }

    @Data
    // 当前正在提交的局。
    public static class CurrentRoundResponse {
        private Long roundId;
        private Integer roundNo;
        private String status;
        private List<RoundParticipantResponse> participants = new ArrayList<>();
    }

    @Data
    // 当前局里的一个玩家提交状态。
    public static class RoundParticipantResponse {
        private Long seatId;
        private String seatName;
        private Long userId;
        private String nickname;
        private String avatarUrl;
        private String submitStatus;
        private Integer netScore;
        private List<PaymentView> payments = new ArrayList<>();
    }

    @Data
    // 页面展示用的输分记录。
    public static class PaymentView {
        // 输分玩家 ID 和昵称。
        private Long fromUserId;
        private String fromNickname;
        // 收分玩家 ID 和昵称。
        private Long toUserId;
        private String toNickname;
        // 收分玩家所在座位名称。
        private String toSeatName;
        // 输分分值。
        private Integer score;
        // 可选备注。
        private String remark;
        // 是否房主代提交。
        private Boolean ownerSubmit;
    }

    @Data
    // 开始游戏响应。
    public static class StartRoomResponse {
        private Long roomId;
        private String status;
        private Integer roundNo;
    }

    @Data
    // 重新打开房间响应。
    public static class ReopenRoomResponse {
        private Long roomId;
        private String status;
        private Integer currentRoundNo;
        private Integer currentSubmittingRoundNo;
    }

    @Data
    // 转让房主请求。
    public static class TransferOwnerRequest {
        @NotNull
        private Long targetUserId;
    }

    @Data
    // 踢人请求。
    public static class KickRequest {
        @NotNull
        private Long targetUserId;
    }

    @Data
    // 关闭房间请求，reason 可为空，后端会补默认值。
    public static class CloseRoomRequest {
        private String reason;
    }
}
