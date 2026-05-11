package com.example.mahjong.module.room.controller;

import com.example.mahjong.common.api.ApiResponse;
import com.example.mahjong.common.security.UserContext;
import com.example.mahjong.module.room.dto.RoomDtos;
import com.example.mahjong.module.room.service.RoomService;
import com.example.mahjong.module.round.dto.RoundDtos;
import com.example.mahjong.module.round.service.RoundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
// 房间接口控制器：负责创建、加入、开始、关闭、踢人、历史局入口等。
public class RoomController {

    private final RoomService roomService;
    private final RoundService roundService;

    // 创建房间，当前登录用户会成为房主并自动占东位。
    @PostMapping
    public ApiResponse<RoomDtos.CreateRoomResponse> create(@Valid @RequestBody RoomDtos.CreateRoomRequest request) {
        return ApiResponse.success(roomService.createRoom(UserContext.requireUserId(), request));
    }

    // 获取房间完整快照，前端等待页和计分页都靠它展示。
    @GetMapping("/{roomId}/snapshot")
    public ApiResponse<RoomDtos.RoomSnapshotResponse> snapshot(@PathVariable Long roomId) {
        return ApiResponse.success(roomService.getSnapshot(roomId));
    }

    // 通过 6 位房间号查找房间，加入房间页使用。
    @GetMapping("/code/{roomCode}")
    public ApiResponse<RoomDtos.RoomCodeResponse> findByCode(@PathVariable String roomCode) {
        return ApiResponse.success(roomService.findByCode(roomCode));
    }

    // 加入某个房间的某个空座位。
    @PostMapping("/{roomId}/join")
    public ApiResponse<RoomDtos.JoinRoomResponse> join(@PathVariable Long roomId, @Valid @RequestBody RoomDtos.JoinRoomRequest request) {
        return ApiResponse.success(roomService.joinRoom(UserContext.requireUserId(), roomId, request));
    }

    // 房主开始游戏，后端会创建房间开启段和第一局提交轮次。
    @PostMapping("/{roomId}/start")
    public ApiResponse<RoomDtos.StartRoomResponse> start(@PathVariable Long roomId) {
        return ApiResponse.success(roomService.startRoom(UserContext.requireUserId(), roomId));
    }

    // 房主关闭房间。
    @PostMapping("/{roomId}/close")
    public ApiResponse<Void> close(@PathVariable Long roomId, @RequestBody(required = false) RoomDtos.CloseRoomRequest request) {
        roomService.closeRoom(UserContext.requireUserId(), roomId, request == null ? null : request.getReason());
        return ApiResponse.success();
    }

    // 房主重新打开已关闭房间。
    @PostMapping("/{roomId}/reopen")
    public ApiResponse<RoomDtos.ReopenRoomResponse> reopen(@PathVariable Long roomId) {
        return ApiResponse.success(roomService.reopenRoom(UserContext.requireUserId(), roomId));
    }

    // 房主转让给房间内其它玩家。
    @PostMapping("/{roomId}/transfer-owner")
    public ApiResponse<Void> transferOwner(@PathVariable Long roomId, @Valid @RequestBody RoomDtos.TransferOwnerRequest request) {
        roomService.transferOwner(UserContext.requireUserId(), roomId, request.getTargetUserId());
        return ApiResponse.success();
    }

    // 房主踢出玩家。
    @PostMapping("/{roomId}/kick")
    public ApiResponse<Void> kick(@PathVariable Long roomId, @Valid @RequestBody RoomDtos.KickRequest request) {
        roomService.kick(UserContext.requireUserId(), roomId, request.getTargetUserId());
        return ApiResponse.success();
    }

    // 玩家主动退出房间。
    @PostMapping("/{roomId}/quit")
    public ApiResponse<Void> quit(@PathVariable Long roomId) {
        roomService.quit(UserContext.requireUserId(), roomId);
        return ApiResponse.success();
    }

    // 查询房间历史局列表。
    @GetMapping("/{roomId}/rounds")
    public ApiResponse<List<RoundDtos.RoundHistoryItem>> history(@PathVariable Long roomId) {
        return ApiResponse.success(roundService.listHistory(roomId));
    }

    // 房主撤销上一局。
    @PostMapping("/{roomId}/rounds/undo-last")
    public ApiResponse<Void> undoLast(@PathVariable Long roomId) {
        roundService.undoLast(UserContext.requireUserId(), roomId);
        return ApiResponse.success();
    }
}
