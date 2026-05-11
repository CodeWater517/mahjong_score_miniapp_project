package com.example.mahjong.module.room.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mahjong.common.api.ErrorCode;
import com.example.mahjong.common.constant.RoomConstants;
import com.example.mahjong.common.constant.SeatNames;
import com.example.mahjong.common.exception.BizException;
import com.example.mahjong.module.ranking.service.RankingService;
import com.example.mahjong.module.room.dto.RoomDtos;
import com.example.mahjong.module.room.entity.Room;
import com.example.mahjong.module.room.entity.RoomOperationLog;
import com.example.mahjong.module.room.entity.RoomSeat;
import com.example.mahjong.module.room.entity.RoomSeatUserHistory;
import com.example.mahjong.module.room.entity.RoomSession;
import com.example.mahjong.module.room.mapper.RoomMapper;
import com.example.mahjong.module.room.mapper.RoomOperationLogMapper;
import com.example.mahjong.module.room.mapper.RoomSeatMapper;
import com.example.mahjong.module.room.mapper.RoomSeatUserHistoryMapper;
import com.example.mahjong.module.room.mapper.RoomSessionMapper;
import com.example.mahjong.module.round.entity.GameRound;
import com.example.mahjong.module.round.entity.RoundParticipant;
import com.example.mahjong.module.round.entity.ScorePayment;
import com.example.mahjong.module.round.mapper.GameRoundMapper;
import com.example.mahjong.module.round.mapper.RoundParticipantMapper;
import com.example.mahjong.module.round.mapper.ScorePaymentMapper;
import com.example.mahjong.module.round.service.RoundService;
import com.example.mahjong.module.user.entity.SysUser;
import com.example.mahjong.module.user.mapper.SysUserMapper;
import com.example.mahjong.websocket.RoomWebSocketSessionManager;
import com.example.mahjong.websocket.WsMessage;
import com.example.mahjong.websocket.WsMessageType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
// 房间业务服务：负责房间创建、加入、开始、关闭、重开、座位流转和快照组装。
public class RoomService {

    private final RoomMapper roomMapper;
    private final RoomSeatMapper roomSeatMapper;
    private final RoomSeatUserHistoryMapper seatHistoryMapper;
    private final RoomSessionMapper roomSessionMapper;
    private final RoomOperationLogMapper operationLogMapper;
    private final SysUserMapper sysUserMapper;
    private final GameRoundMapper gameRoundMapper;
    private final RoundParticipantMapper roundParticipantMapper;
    private final ScorePaymentMapper scorePaymentMapper;
    private final RankingService rankingService;
    private final ObjectMapper objectMapper;
    private final RoomWebSocketSessionManager socketSessionManager;
    private final RoundService roundService;

    private final Random random = new Random();

    // 创建房间：生成房间号、创建房间、创建座位，并让房主自动坐到第一个座位。
    @Transactional
    public RoomDtos.CreateRoomResponse createRoom(Long userId, RoomDtos.CreateRoomRequest request) {
        int playerCount = request.getPlayerCount();
        int initialScore = request.getInitialScore() == null ? 0 : request.getInitialScore();
        String roomCode = generateUniqueRoomCode();

        // 先插入房间主记录。
        Room room = new Room();
        room.setRoomCode(roomCode);
        room.setRoomName("麻将房间 " + roomCode);
        room.setOwnerUserId(userId);
        room.setPlayerCount(playerCount);
        room.setInitialScore(initialScore);
        room.setStatus(RoomConstants.ROOM_WAITING);
        room.setCurrentRoundNo(0);
        roomMapper.insert(room);

        // 再按照人数创建座位，座位 1 默认给房主。
        for (int i = 1; i <= playerCount; i++) {
            RoomSeat seat = new RoomSeat();
            seat.setRoomId(room.getId());
            seat.setSeatNo(i);
            seat.setSeatName(SeatNames.code(i));
            seat.setCurrentScore(initialScore);
            roomSeatMapper.insert(seat);
            if (i == 1) {
                occupySeat(room, seat, userId, null, "CREATE");
            }
        }
        writeLog(room.getId(), userId, "CREATE_ROOM", userId, null, null, room, "创建房间");

        // 返回前端进入等待页需要的基本信息。
        RoomDtos.CreateRoomResponse response = new RoomDtos.CreateRoomResponse();
        response.setRoomId(room.getId());
        response.setRoomCode(room.getRoomCode());
        response.setRoomName(room.getRoomName());
        response.setStatus(room.getStatus());
        response.setOwnerUserId(room.getOwnerUserId());
        return response;
    }

    // 获取房间快照：把房间、座位、当前轮、排行榜组装成前端可直接展示的数据。
    public RoomDtos.RoomSnapshotResponse getSnapshot(Long roomId) {
        Room room = requireRoom(roomId);
        List<RoomSeat> seats = listSeats(roomId);
        // 批量加载座位上的用户资料和活跃座位历史。
        Map<Long, SysUser> users = loadUsers(seats.stream().map(RoomSeat::getCurrentUserId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, RoomSeatUserHistory> activeHistoryBySeat = listActiveHistories(roomId).stream()
            .collect(Collectors.toMap(RoomSeatUserHistory::getSeatId, Function.identity(), (a, b) -> a));

        RoomDtos.RoomSnapshotResponse response = new RoomDtos.RoomSnapshotResponse();
        response.setRoomId(room.getId());
        response.setRoomCode(room.getRoomCode());
        response.setRoomName(room.getRoomName());
        response.setStatus(room.getStatus());
        response.setOwnerUserId(room.getOwnerUserId());
        response.setPlayerCount(room.getPlayerCount());
        response.setCurrentRoundNo(room.getCurrentRoundNo());
        response.setSeats(seats.stream().map(seat -> toSeatResponse(seat, users.get(seat.getCurrentUserId()), activeHistoryBySeat.get(seat.getId()))).toList());

        // 进行中的房间会有一个 SUBMITTING 轮次，前端据此显示提交状态。
        GameRound currentRound = getCurrentSubmittingRound(roomId);
        if (currentRound != null) {
            response.setCurrentSubmittingRoundNo(currentRound.getRoundNo());
            response.setCurrentRound(buildCurrentRound(currentRound));
        }
        response.setRankList(rankingService.getRoomRank(roomId));
        return response;
    }

    // 根据 6 位房间号查找房间，并返回当前空座位。
    public RoomDtos.RoomCodeResponse findByCode(String roomCode) {
        Room room = roomMapper.selectOne(new LambdaQueryWrapper<Room>().eq(Room::getRoomCode, roomCode));
        if (room == null) {
            throw new BizException(ErrorCode.ROOM_NOT_FOUND, "房间不存在");
        }
        RoomDtos.RoomCodeResponse response = new RoomDtos.RoomCodeResponse();
        response.setRoomId(room.getId());
        response.setRoomCode(room.getRoomCode());
        response.setRoomName(room.getRoomName());
        response.setStatus(room.getStatus());
        response.setEmptySeats(listSeats(room.getId()).stream()
            .filter(seat -> seat.getCurrentUserId() == null)
            .map(seat -> toSeatResponse(seat, null, null))
            .toList());
        return response;
    }

    // 玩家加入房间：校验房间和座位后，占用座位并广播座位更新。
    @Transactional
    public RoomDtos.JoinRoomResponse joinRoom(Long userId, Long roomId, RoomDtos.JoinRoomRequest request) {
        Room room = requireRoom(roomId);
        if (RoomConstants.ROOM_CLOSED.equals(room.getStatus())) {
            throw new BizException(ErrorCode.ROOM_STATE_ERROR, "房间已关闭，不能加入");
        }
        if (findSeatByUser(roomId, userId) != null) {
            throw new BizException("你已在该房间中");
        }
        // 只能加入属于该房间且当前为空的座位。
        RoomSeat seat = roomSeatMapper.selectById(request.getSeatId());
        if (seat == null || !Objects.equals(seat.getRoomId(), roomId)) {
            throw new BizException("座位不存在");
        }
        if (seat.getCurrentUserId() != null) {
            throw new BizException("该座位已有人");
        }
        occupySeat(room, seat, userId, request.getRoomNickname(), "JOIN");
        socketSessionManager.broadcastToRoom(roomId, WsMessage.of(WsMessageType.SEAT_UPDATED, roomId, getSnapshot(roomId)));

        RoomDtos.JoinRoomResponse response = new RoomDtos.JoinRoomResponse();
        response.setRoomId(roomId);
        response.setSeatId(seat.getId());
        response.setSeatName(seat.getSeatName());
        response.setInheritedScore(seat.getCurrentScore());
        return response;
    }

    // 房主开始游戏：要求等待中且满员，然后创建 session 和第一局提交轮次。
    @Transactional
    public RoomDtos.StartRoomResponse startRoom(Long userId, Long roomId) {
        Room room = requireRoom(roomId);
        assertOwner(room, userId);
        if (!RoomConstants.ROOM_WAITING.equals(room.getStatus())) {
            throw new BizException(ErrorCode.ROOM_STATE_ERROR, "只有等待中的房间可以开始");
        }
        List<RoomSeat> seats = listSeats(roomId);
        if (seats.size() != room.getPlayerCount() || seats.stream().anyMatch(seat -> seat.getCurrentUserId() == null)) {
            throw new BizException(ErrorCode.ROOM_STATE_ERROR, "必须满员才能开始游戏");
        }

        // session 表示一次从开始到关闭的房间开启段。
        RoomSession session = createSession(room, RoomConstants.ROOM_PLAYING);
        room.setStatus(RoomConstants.ROOM_PLAYING);
        roomMapper.updateById(room);
        // 开始游戏后马上创建第一局 SUBMITTING 轮次。
        GameRound round = roundService.createSubmittingRound(room, session.getId());
        writeLog(roomId, userId, "START_ROOM", null, null, null, room, "开始游戏");
        socketSessionManager.broadcastToRoom(roomId, WsMessage.of(WsMessageType.GAME_STARTED, roomId, getSnapshot(roomId)));

        RoomDtos.StartRoomResponse response = new RoomDtos.StartRoomResponse();
        response.setRoomId(roomId);
        response.setStatus(room.getStatus());
        response.setRoundNo(round.getRoundNo());
        return response;
    }

    // 房主手动关闭房间。
    @Transactional
    public void closeRoom(Long userId, Long roomId, String reason) {
        Room room = requireRoom(roomId);
        assertOwner(room, userId);
        closeRoomInternal(room, userId, StringUtils.hasText(reason) ? reason : RoomConstants.CLOSE_OWNER);
    }

    // 定时任务自动关闭房间。
    @Transactional
    public void autoCloseRoom(Room room) {
        closeRoomInternal(room, room.getOwnerUserId(), RoomConstants.CLOSE_AUTO);
    }

    // 重新打开已关闭房间，并创建新的提交轮次。
    @Transactional
    public RoomDtos.ReopenRoomResponse reopenRoom(Long userId, Long roomId) {
        Room room = requireRoom(roomId);
        assertOwner(room, userId);
        if (!RoomConstants.ROOM_CLOSED.equals(room.getStatus())) {
            throw new BizException(ErrorCode.ROOM_STATE_ERROR, "只有已关闭房间可以重新打开");
        }
        // 重新打开会开启新的 session，但保留历史局和座位分。
        RoomSession session = createSession(room, RoomConstants.ROOM_PLAYING);
        room.setStatus(RoomConstants.ROOM_PLAYING);
        roomMapper.updateById(room);
        GameRound round = roundService.createSubmittingRound(room, session.getId());
        writeLog(roomId, userId, "REOPEN_ROOM", null, null, null, room, "重新打开房间");
        socketSessionManager.broadcastToRoom(roomId, WsMessage.of(WsMessageType.ROOM_REOPENED, roomId, getSnapshot(roomId)));

        RoomDtos.ReopenRoomResponse response = new RoomDtos.ReopenRoomResponse();
        response.setRoomId(roomId);
        response.setStatus(room.getStatus());
        response.setCurrentRoundNo(room.getCurrentRoundNo());
        response.setCurrentSubmittingRoundNo(round.getRoundNo());
        return response;
    }

    // 房主转让：目标用户必须还在房间里。
    @Transactional
    public void transferOwner(Long userId, Long roomId, Long targetUserId) {
        Room room = requireRoom(roomId);
        assertOwner(room, userId);
        RoomSeat targetSeat = findSeatByUser(roomId, targetUserId);
        if (targetSeat == null) {
            throw new BizException("目标用户不在房间内");
        }
        Long before = room.getOwnerUserId();
        room.setOwnerUserId(targetUserId);
        roomMapper.updateById(room);
        writeLog(roomId, userId, "TRANSFER_OWNER", targetUserId, null, before, targetUserId, "转让房主");
        socketSessionManager.broadcastToRoom(roomId, WsMessage.of(WsMessageType.OWNER_CHANGED, roomId, getSnapshot(roomId)));
    }

    // 房主踢出玩家。
    @Transactional
    public void kick(Long userId, Long roomId, Long targetUserId) {
        Room room = requireRoom(roomId);
        assertOwner(room, userId);
        if (Objects.equals(userId, targetUserId)) {
            throw new BizException("房主不能踢出自己，请使用退出房间");
        }
        leaveSeat(room, targetUserId, "KICKED");
        writeLog(roomId, userId, "KICK_USER", targetUserId, null, null, null, "踢出玩家");
        socketSessionManager.broadcastToRoom(roomId, WsMessage.of(WsMessageType.USER_KICKED, roomId, Map.of("targetUserId", targetUserId)));
        socketSessionManager.broadcastToRoom(roomId, WsMessage.of(WsMessageType.SEAT_UPDATED, roomId, getSnapshot(roomId)));
    }

    // 玩家退出房间。房主退出时会自动转让给最早加入的其他玩家。
    @Transactional
    public void quit(Long userId, Long roomId) {
        Room room = requireRoom(roomId);
        boolean ownerQuit = Objects.equals(room.getOwnerUserId(), userId);
        if (ownerQuit) {
            // 按加入时间找最早的其他在线玩家接任房主。
            List<RoomSeatUserHistory> others = listActiveHistories(roomId).stream()
                .filter(history -> !Objects.equals(history.getUserId(), userId))
                .sorted(Comparator.comparing(RoomSeatUserHistory::getJoinTime))
                .toList();
            if (!others.isEmpty()) {
                room.setOwnerUserId(others.get(0).getUserId());
                roomMapper.updateById(room);
            }
        }
        leaveSeat(room, userId, ownerQuit ? "OWNER_TRANSFER_QUIT" : "QUIT");
        if (ownerQuit && listSeats(roomId).stream().noneMatch(seat -> seat.getCurrentUserId() != null)) {
            // 房主退出且房间没人了，直接关闭房间。
            closeRoomInternal(room, userId, RoomConstants.CLOSE_OWNER);
        } else {
            writeLog(roomId, userId, "QUIT_ROOM", userId, null, null, null, "退出房间");
            socketSessionManager.broadcastToRoom(roomId, WsMessage.of(WsMessageType.SEAT_UPDATED, roomId, getSnapshot(roomId)));
        }
    }

    // 查房间，不存在时抛业务异常。
    public Room requireRoom(Long roomId) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw new BizException(ErrorCode.ROOM_NOT_FOUND, "房间不存在");
        }
        return room;
    }

    // 校验当前用户是否房主。
    public void assertOwner(Room room, Long userId) {
        if (!Objects.equals(room.getOwnerUserId(), userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅房主可操作");
        }
    }

    // 按座位号顺序列出房间座位。
    public List<RoomSeat> listSeats(Long roomId) {
        return roomSeatMapper.selectList(new LambdaQueryWrapper<RoomSeat>()
            .eq(RoomSeat::getRoomId, roomId)
            .orderByAsc(RoomSeat::getSeatNo));
    }

    // 获取当前未关闭的房间开启段。
    public RoomSession getCurrentSession(Long roomId) {
        return roomSessionMapper.selectOne(new LambdaQueryWrapper<RoomSession>()
            .eq(RoomSession::getRoomId, roomId)
            .isNull(RoomSession::getEndTime)
            .orderByDesc(RoomSession::getSessionNo)
            .last("limit 1"));
    }

    // 关闭房间的内部统一逻辑，手动关闭和自动关闭都会走这里。
    private void closeRoomInternal(Room room, Long operatorUserId, String reason) {
        if (RoomConstants.ROOM_CLOSED.equals(room.getStatus())) {
            return;
        }
        // 关闭房间时移除尚未结算的提交轮次，避免房间关闭后还存在开放提交。
        removeOpenSubmittingRound(room.getId());
        RoomSession session = getCurrentSession(room.getId());
        if (session != null) {
            // 结束当前开启段。
            session.setEndTime(LocalDateTime.now());
            session.setEndRoundNo(room.getCurrentRoundNo());
            session.setCloseReason(reason);
            roomSessionMapper.updateById(session);
        }
        room.setStatus(RoomConstants.ROOM_CLOSED);
        roomMapper.updateById(room);
        writeLog(room.getId(), operatorUserId, "CLOSE_ROOM", null, null, null, reason, "关闭房间");
        socketSessionManager.broadcastToRoom(room.getId(), WsMessage.of(WsMessageType.ROOM_CLOSED, room.getId(), getSnapshot(room.getId())));
    }

    // 删除当前开放中的 SUBMITTING 轮次及其临时提交数据。
    private void removeOpenSubmittingRound(Long roomId) {
        GameRound round = getCurrentSubmittingRound(roomId);
        if (round == null) {
            return;
        }
        scorePaymentMapper.delete(new LambdaQueryWrapper<ScorePayment>().eq(ScorePayment::getRoundId, round.getId()));
        roundParticipantMapper.delete(new LambdaQueryWrapper<RoundParticipant>().eq(RoundParticipant::getRoundId, round.getId()));
        gameRoundMapper.deleteById(round.getId());
    }

    // 创建新的房间开启段，sessionNo 在同一房间内递增。
    private RoomSession createSession(Room room, String nextStatus) {
        Integer maxSessionNo = roomSessionMapper.selectList(new LambdaQueryWrapper<RoomSession>().eq(RoomSession::getRoomId, room.getId()))
            .stream()
            .map(RoomSession::getSessionNo)
            .max(Integer::compareTo)
            .orElse(0);
        RoomSession session = new RoomSession();
        session.setRoomId(room.getId());
        session.setSessionNo(maxSessionNo + 1);
        session.setStartTime(LocalDateTime.now());
        session.setStartRoundNo(room.getCurrentRoundNo());
        roomSessionMapper.insert(session);
        return session;
    }

    // 查询房间当前正在提交的轮次。
    private GameRound getCurrentSubmittingRound(Long roomId) {
        return gameRoundMapper.selectOne(new LambdaQueryWrapper<GameRound>()
            .eq(GameRound::getRoomId, roomId)
            .eq(GameRound::getStatus, RoomConstants.ROUND_SUBMITTING)
            .eq(GameRound::getDeleted, 0)
            .orderByDesc(GameRound::getRoundNo)
            .last("limit 1"));
    }

    // 把当前提交轮次转换成前端需要的 CurrentRoundResponse。
    private RoomDtos.CurrentRoundResponse buildCurrentRound(GameRound round) {
        List<RoundParticipant> participants = roundParticipantMapper.selectList(new LambdaQueryWrapper<RoundParticipant>()
            .eq(RoundParticipant::getRoundId, round.getId()));
        List<ScorePayment> payments = scorePaymentMapper.selectList(new LambdaQueryWrapper<ScorePayment>()
            .eq(ScorePayment::getRoundId, round.getId()));

        // 参与者和付款流水里都可能涉及用户，统一收集后批量加载。
        Set<Long> userIds = new HashSet<>();
        participants.stream().map(RoundParticipant::getUserId).filter(Objects::nonNull).forEach(userIds::add);
        payments.forEach(payment -> {
            userIds.add(payment.getFromUserId());
            userIds.add(payment.getToUserId());
        });
        Map<Long, SysUser> users = loadUsers(userIds);
        Map<Long, RoomSeat> seats = listSeats(round.getRoomId()).stream().collect(Collectors.toMap(RoomSeat::getId, Function.identity()));
        // 按提交人分组，方便放到对应参与者下面展示。
        Map<Long, List<ScorePayment>> paymentsByUser = payments.stream().collect(Collectors.groupingBy(ScorePayment::getFromUserId));

        RoomDtos.CurrentRoundResponse response = new RoomDtos.CurrentRoundResponse();
        response.setRoundId(round.getId());
        response.setRoundNo(round.getRoundNo());
        response.setStatus(round.getStatus());
        response.setParticipants(participants.stream()
            .sorted(Comparator.comparing(p -> seats.get(p.getSeatId()).getSeatNo()))
            .map(participant -> {
                SysUser user = users.get(participant.getUserId());
                RoomSeat seat = seats.get(participant.getSeatId());
                RoomDtos.RoundParticipantResponse item = new RoomDtos.RoundParticipantResponse();
                item.setSeatId(participant.getSeatId());
                item.setSeatName(seat == null ? null : seat.getSeatName());
                item.setUserId(participant.getUserId());
                item.setNickname(user == null ? "离席玩家" : user.getNickname());
                item.setAvatarUrl(user == null ? null : user.getAvatarUrl());
                item.setSubmitStatus(participant.getSubmitStatus());
                item.setNetScore(participant.getNetScore());
                item.setPayments(paymentsByUser.getOrDefault(participant.getUserId(), List.of()).stream()
                    .map(payment -> toPaymentView(payment, users, seats))
                    .toList());
                return item;
            })
            .toList());
        return response;
    }

    // 把输分流水转换成页面展示对象。
    private RoomDtos.PaymentView toPaymentView(ScorePayment payment, Map<Long, SysUser> users, Map<Long, RoomSeat> seats) {
        RoomDtos.PaymentView view = new RoomDtos.PaymentView();
        view.setFromUserId(payment.getFromUserId());
        view.setFromNickname(displayName(users.get(payment.getFromUserId())));
        view.setToUserId(payment.getToUserId());
        view.setToNickname(displayName(users.get(payment.getToUserId())));
        RoomSeat toSeat = seats.get(payment.getToSeatId());
        view.setToSeatName(toSeat == null ? null : toSeat.getSeatName());
        view.setScore(payment.getScore());
        view.setRemark(payment.getRemark());
        view.setOwnerSubmit(Objects.equals(payment.getIsOwnerSubmit(), 1));
        return view;
    }

    // 把座位实体和用户信息转换成前端 SeatCard 使用的数据。
    private RoomDtos.SeatResponse toSeatResponse(RoomSeat seat, SysUser user, RoomSeatUserHistory history) {
        RoomDtos.SeatResponse response = new RoomDtos.SeatResponse();
        response.setSeatId(seat.getId());
        response.setSeatName(seat.getSeatName());
        response.setDisplayName(SeatNames.label(seat.getSeatName()));
        response.setCurrentUserId(seat.getCurrentUserId());
        response.setNickname(user == null ? null : user.getNickname());
        response.setAvatarUrl(user == null ? null : user.getAvatarUrl());
        response.setRoomNickname(history == null ? null : history.getRoomNickname());
        response.setCurrentScore(seat.getCurrentScore());
        response.setEmpty(seat.getCurrentUserId() == null);
        response.setJoinedAt(seat.getJoinedAt());
        return response;
    }

    // 占用座位：更新 room_seat 当前用户，并新增一条活跃的座位历史。
    private void occupySeat(Room room, RoomSeat seat, Long userId, String roomNickname, String reason) {
        LocalDateTime now = LocalDateTime.now();
        // 座位分不随玩家清零；这里只替换当前用户，让新玩家自然继承 current_score。
        seat.setCurrentUserId(userId);
        seat.setJoinedAt(now);
        roomSeatMapper.updateById(seat);

        // 个人统计周期从下一局开始，避免把上一个座位玩家的历史局算到新玩家身上。
        RoomSeatUserHistory history = new RoomSeatUserHistory();
        history.setRoomId(room.getId());
        history.setSeatId(seat.getId());
        history.setUserId(userId);
        history.setRoomNickname(roomNickname);
        history.setJoinRoundNo(room.getCurrentRoundNo() + 1);
        history.setJoinTime(now);
        history.setActive(1);
        seatHistoryMapper.insert(history);
        writeLog(room.getId(), userId, reason + "_SEAT", userId, null, null, seat, "进入座位");
    }

    // 离开座位：结束当前玩家的座位历史，但保留座位分。
    private void leaveSeat(Room room, Long userId, String reason) {
        RoomSeat seat = findSeatByUser(room.getId(), userId);
        if (seat == null) {
            throw new BizException("玩家不在房间内");
        }
        // 离席只结束个人统计周期，不改座位分；座位分继续留给后续加入者继承。
        RoomSeatUserHistory history = seatHistoryMapper.selectOne(new LambdaQueryWrapper<RoomSeatUserHistory>()
            .eq(RoomSeatUserHistory::getRoomId, room.getId())
            .eq(RoomSeatUserHistory::getUserId, userId)
            .eq(RoomSeatUserHistory::getActive, 1)
            .last("limit 1"));
        if (history != null) {
            history.setActive(0);
            history.setLeaveRoundNo(room.getCurrentRoundNo());
            history.setLeaveTime(LocalDateTime.now());
            history.setLeaveReason(reason);
            seatHistoryMapper.updateById(history);
        }
        seat.setCurrentUserId(null);
        seat.setJoinedAt(null);
        roomSeatMapper.updateById(seat);
    }

    // 根据用户 ID 查找他当前所在座位。
    private RoomSeat findSeatByUser(Long roomId, Long userId) {
        return roomSeatMapper.selectOne(new LambdaQueryWrapper<RoomSeat>()
            .eq(RoomSeat::getRoomId, roomId)
            .eq(RoomSeat::getCurrentUserId, userId)
            .last("limit 1"));
    }

    // 查询房间内仍然 active 的座位历史。
    private List<RoomSeatUserHistory> listActiveHistories(Long roomId) {
        return seatHistoryMapper.selectList(new LambdaQueryWrapper<RoomSeatUserHistory>()
            .eq(RoomSeatUserHistory::getRoomId, roomId)
            .eq(RoomSeatUserHistory::getActive, 1));
    }

    // 批量加载用户资料。
    private Map<Long, SysUser> loadUsers(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        return sysUserMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));
    }

    // 统一处理显示名。
    private String displayName(SysUser user) {
        if (user == null) {
            return "未知玩家";
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : "微信用户";
    }

    // 生成不重复的 6 位房间号，最多尝试 20 次。
    private String generateUniqueRoomCode() {
        for (int i = 0; i < 20; i++) {
            String code = String.valueOf(100000 + random.nextInt(900000));
            Long count = roomMapper.selectCount(new LambdaQueryWrapper<Room>().eq(Room::getRoomCode, code));
            if (count == 0) {
                return code;
            }
        }
        throw new BizException("房间号生成失败，请重试");
    }

    // 写入房间操作日志，方便后续追踪房主操作和历史修改。
    private void writeLog(Long roomId, Long operatorUserId, String type, Long targetUserId, Long targetRoundId,
                          Object before, Object after, String remark) {
        RoomOperationLog log = new RoomOperationLog();
        log.setRoomId(roomId);
        log.setOperatorUserId(operatorUserId);
        log.setOperationType(type);
        log.setTargetUserId(targetUserId);
        log.setTargetRoundId(targetRoundId);
        log.setBeforeData(toJson(before));
        log.setAfterData(toJson(after));
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    // 把日志里的 before/after 对象转成 JSON，失败时退化成字符串。
    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }
}
