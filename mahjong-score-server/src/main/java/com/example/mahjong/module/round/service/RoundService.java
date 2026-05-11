package com.example.mahjong.module.round.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mahjong.common.api.ErrorCode;
import com.example.mahjong.common.constant.RoomConstants;
import com.example.mahjong.common.exception.BizException;
import com.example.mahjong.module.room.dto.RoomDtos;
import com.example.mahjong.module.room.entity.Room;
import com.example.mahjong.module.room.entity.RoomOperationLog;
import com.example.mahjong.module.room.entity.RoomSeat;
import com.example.mahjong.module.room.entity.RoomSession;
import com.example.mahjong.module.room.mapper.RoomMapper;
import com.example.mahjong.module.room.mapper.RoomOperationLogMapper;
import com.example.mahjong.module.room.mapper.RoomSeatMapper;
import com.example.mahjong.module.room.mapper.RoomSessionMapper;
import com.example.mahjong.module.round.dto.RoundDtos;
import com.example.mahjong.module.round.entity.GameRound;
import com.example.mahjong.module.round.entity.RoundParticipant;
import com.example.mahjong.module.round.entity.ScorePayment;
import com.example.mahjong.module.round.mapper.GameRoundMapper;
import com.example.mahjong.module.round.mapper.RoundParticipantMapper;
import com.example.mahjong.module.round.mapper.ScorePaymentMapper;
import com.example.mahjong.module.stats.service.StatsRecalculationService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
// 单局业务服务：负责创建提交轮次、保存提交、自动结算、历史局修改和重算通知。
public class RoundService {

    private final GameRoundMapper gameRoundMapper;
    private final RoundParticipantMapper participantMapper;
    private final ScorePaymentMapper paymentMapper;
    private final RoomMapper roomMapper;
    private final RoomSeatMapper seatMapper;
    private final RoomSessionMapper sessionMapper;
    private final RoomOperationLogMapper operationLogMapper;
    private final SysUserMapper sysUserMapper;
    private final StatsRecalculationService statsRecalculationService;
    private final RoomWebSocketSessionManager socketSessionManager;
    private final ObjectMapper objectMapper;

    // 创建一个新的“正在提交”轮次，并为当前在座玩家创建参与记录。
    @Transactional
    public GameRound createSubmittingRound(Room room, Long sessionId) {
        GameRound round = new GameRound();
        round.setRoomId(room.getId());
        round.setSessionId(sessionId);
        round.setRoundNo(nextRoundNo(room));
        round.setStatus(RoomConstants.ROUND_SUBMITTING);
        round.setDeleted(0);
        gameRoundMapper.insert(round);

        // 当前有人的座位才会成为本局参与者。
        List<RoomSeat> seats = seatMapper.selectList(new LambdaQueryWrapper<RoomSeat>()
            .eq(RoomSeat::getRoomId, room.getId())
            .isNotNull(RoomSeat::getCurrentUserId)
            .orderByAsc(RoomSeat::getSeatNo));
        for (RoomSeat seat : seats) {
            RoundParticipant participant = new RoundParticipant();
            participant.setRoomId(room.getId());
            participant.setRoundId(round.getId());
            participant.setSeatId(seat.getId());
            participant.setUserId(seat.getCurrentUserId());
            participant.setSubmitStatus(RoomConstants.SUBMIT_PENDING);
            participant.setNetScore(0);
            participant.setActiveForStats(1);
            participantMapper.insert(participant);
        }
        return round;
    }

    // 计算下一局局号：同时参考房间 currentRoundNo 和数据库已有最大局号，避免历史修改后重复。
    private Integer nextRoundNo(Room room) {
        Integer maxExistingRoundNo = gameRoundMapper.selectList(new LambdaQueryWrapper<GameRound>().eq(GameRound::getRoomId, room.getId()))
            .stream()
            .map(GameRound::getRoundNo)
            .max(Integer::compareTo)
            .orElse(0);
        return Math.max(room.getCurrentRoundNo() + 1, maxExistingRoundNo + 1);
    }

    // 普通玩家提交本轮输分记录。
    @Transactional
    public RoundDtos.SubmitRoundResponse submitRound(Long userId, Long roundId, RoundDtos.SubmitRoundRequest request) {
        GameRound round = requireSubmittingRound(roundId);
        Room room = requirePlayingRoom(round.getRoomId());
        RoundParticipant participant = findParticipant(roundId, userId);
        if (participant == null) {
            throw new BizException("你不是本轮参与者");
        }
        // 如果不是第一次提交，通知类型会变成 ROUND_SUBMIT_MODIFIED。
        boolean modified = !RoomConstants.SUBMIT_PENDING.equals(participant.getSubmitStatus());
        savePayments(round, participant, userId, request.getPayments(), false);
        participant.setSubmitStatus(RoomConstants.SUBMIT_SUBMITTED);
        participant.setSubmittedBy(userId);
        participant.setSubmittedAt(LocalDateTime.now());
        participantMapper.updateById(participant);

        // 如果所有人都提交了，提交后立即结算本局。
        boolean allSubmitted = allSubmitted(roundId);
        socketSessionManager.broadcastToRoom(room.getId(), WsMessage.of(modified ? WsMessageType.ROUND_SUBMIT_MODIFIED : WsMessageType.ROUND_SUBMITTED, room.getId(), Map.of(
            "roundId", roundId,
            "roundNo", round.getRoundNo(),
            "userId", userId,
            "submitStatus", participant.getSubmitStatus()
        )));
        if (allSubmitted) {
            settleRound(roundId);
        }
        return submitResponse(roundId, participant.getSubmitStatus(), allSubmitted);
    }

    // 房主代替未提交玩家提交本轮输分记录。
    @Transactional
    public RoundDtos.SubmitRoundResponse ownerSubmit(Long ownerUserId, Long roundId, RoundDtos.OwnerSubmitRequest request) {
        GameRound round = requireSubmittingRound(roundId);
        Room room = requirePlayingRoom(round.getRoomId());
        assertOwner(room, ownerUserId);
        RoundParticipant target = findParticipant(roundId, request.getTargetUserId());
        if (target == null) {
            throw new BizException("目标玩家不是本轮参与者");
        }
        if (!RoomConstants.SUBMIT_PENDING.equals(target.getSubmitStatus())) {
            throw new BizException("不能覆盖已提交玩家的内容");
        }
        // ownerSubmit=true 会在 score_payment 里留下代提交标记。
        savePayments(round, target, ownerUserId, request.getPayments(), true);
        target.setSubmitStatus(RoomConstants.SUBMIT_OWNER_SUBMITTED);
        target.setSubmittedBy(ownerUserId);
        target.setSubmittedAt(LocalDateTime.now());
        participantMapper.updateById(target);

        boolean allSubmitted = allSubmitted(roundId);
        writeLog(room.getId(), ownerUserId, "OWNER_SUBMIT", request.getTargetUserId(), roundId, null, request, "房主代提交");
        socketSessionManager.broadcastToRoom(room.getId(), WsMessage.of(WsMessageType.ROUND_SUBMITTED, room.getId(), Map.of(
            "roundId", roundId,
            "roundNo", round.getRoundNo(),
            "userId", request.getTargetUserId(),
            "submitStatus", target.getSubmitStatus()
        )));
        if (allSubmitted) {
            settleRound(roundId);
        }
        return submitResponse(roundId, target.getSubmitStatus(), allSubmitted);
    }

    // 房主把某个未提交玩家强制设置为“不输不赢”。
    @Transactional
    public RoundDtos.SubmitRoundResponse forceNeutral(Long ownerUserId, Long roundId, RoundDtos.ForceNeutralRequest request) {
        GameRound round = requireSubmittingRound(roundId);
        Room room = requirePlayingRoom(round.getRoomId());
        assertOwner(room, ownerUserId);
        RoundParticipant target = findParticipant(roundId, request.getTargetUserId());
        if (target == null) {
            throw new BizException("目标玩家不是本轮参与者");
        }
        if (!RoomConstants.SUBMIT_PENDING.equals(target.getSubmitStatus())) {
            throw new BizException("只能强制未提交玩家");
        }
        // 删除该玩家作为输家的记录，即代表本轮没有输给任何人。
        paymentMapper.delete(new LambdaQueryWrapper<ScorePayment>()
            .eq(ScorePayment::getRoundId, roundId)
            .eq(ScorePayment::getFromUserId, request.getTargetUserId()));
        target.setSubmitStatus(RoomConstants.SUBMIT_FORCED_SUBMITTED);
        target.setSubmittedBy(ownerUserId);
        target.setSubmittedAt(LocalDateTime.now());
        participantMapper.updateById(target);

        boolean allSubmitted = allSubmitted(roundId);
        writeLog(room.getId(), ownerUserId, "FORCE_NEUTRAL", request.getTargetUserId(), roundId, null, request, "强制不输不赢");
        if (allSubmitted) {
            settleRound(roundId);
        }
        return submitResponse(roundId, target.getSubmitStatus(), allSubmitted);
    }

    // 结算本局：汇总所有输分流水，更新参与者净分、座位分、房间轮次，并创建下一局。
    @Transactional
    public void settleRound(Long roundId) {
        GameRound round = gameRoundMapper.selectById(roundId);
        if (round == null || !RoomConstants.ROUND_SUBMITTING.equals(round.getStatus())) {
            // 已经结算或不存在时直接返回，避免重复结算。
            return;
        }
        Room room = roomMapper.selectById(round.getRoomId());
        List<RoundParticipant> participants = participantMapper.selectList(new LambdaQueryWrapper<RoundParticipant>().eq(RoundParticipant::getRoundId, roundId));
        List<ScorePayment> payments = paymentMapper.selectList(new LambdaQueryWrapper<ScorePayment>().eq(ScorePayment::getRoundId, roundId));
        Map<Long, Integer> userNet = new HashMap<>();
        Map<Long, Integer> seatNet = new HashMap<>();
        // 同一条输分流水同时影响“用户个人分”和“座位继承分”，两个口径必须分开累计。
        for (ScorePayment payment : payments) {
            userNet.merge(payment.getFromUserId(), -payment.getScore(), Integer::sum);
            userNet.merge(payment.getToUserId(), payment.getScore(), Integer::sum);
            seatNet.merge(payment.getFromSeatId(), -payment.getScore(), Integer::sum);
            seatNet.merge(payment.getToSeatId(), payment.getScore(), Integer::sum);
        }
        for (RoundParticipant participant : participants) {
            // 每个参与者写入本局个人净分。
            participant.setNetScore(participant.getUserId() == null ? 0 : userNet.getOrDefault(participant.getUserId(), 0));
            participantMapper.updateById(participant);
        }
        for (Map.Entry<Long, Integer> entry : seatNet.entrySet()) {
            // 座位分按座位净分累加，给后续加入者继承。
            RoomSeat seat = seatMapper.selectById(entry.getKey());
            seat.setCurrentScore(seat.getCurrentScore() + entry.getValue());
            seatMapper.updateById(seat);
        }
        round.setStatus(RoomConstants.ROUND_SETTLED);
        round.setSettledAt(LocalDateTime.now());
        gameRoundMapper.updateById(round);

        room.setCurrentRoundNo(round.getRoundNo());
        room.setLastScoreTime(round.getSettledAt());
        roomMapper.updateById(room);

        // 结算后也走一次重算通道，保证历史修改和正常结算使用同一套统计口径。
        statsRecalculationService.recalculateRoom(room.getId());
        GameRound nextRound = null;
        RoomSession session = getCurrentSession(room.getId());
        if (session != null && RoomConstants.ROOM_PLAYING.equals(room.getStatus())) {
            // 房间仍在进行中时，自动创建下一局提交轮次。
            nextRound = createSubmittingRound(room, session.getId());
        }
        socketSessionManager.broadcastToRoom(room.getId(), WsMessage.of(WsMessageType.ROUND_SETTLED, room.getId(), buildSettledPayload(round, nextRound)));
    }

    // 查询房间历史局列表，只返回已结算且未删除的局。
    public List<RoundDtos.RoundHistoryItem> listHistory(Long roomId) {
        List<GameRound> rounds = gameRoundMapper.selectList(new LambdaQueryWrapper<GameRound>()
            .eq(GameRound::getRoomId, roomId)
            .eq(GameRound::getStatus, RoomConstants.ROUND_SETTLED)
            .eq(GameRound::getDeleted, 0)
            .orderByDesc(GameRound::getRoundNo));
        return rounds.stream().map(round -> {
            RoundDtos.RoundHistoryItem item = new RoundDtos.RoundHistoryItem();
            item.setRoundId(round.getId());
            item.setRoundNo(round.getRoundNo());
            item.setSettledAt(round.getSettledAt());
            item.setSummary(buildSummary(round.getId()));
            return item;
        }).toList();
    }

    // 查询某一局详情，包括结算摘要和输分明细。
    public RoundDtos.RoundDetailResponse getDetail(Long roundId) {
        GameRound round = requireRound(roundId);
        RoundDtos.RoundDetailResponse response = new RoundDtos.RoundDetailResponse();
        response.setRoundId(round.getId());
        response.setRoomId(round.getRoomId());
        response.setRoundNo(round.getRoundNo());
        response.setStatus(round.getStatus());
        response.setSummary(buildSummary(roundId));
        response.setPayments(buildPaymentViews(roundId));
        return response;
    }

    // 房主修改历史局：替换原有输分流水并触发整房间重算。
    @Transactional
    public void updateHistoryRound(Long ownerUserId, Long roundId, RoundDtos.UpdateHistoryRoundRequest request) {
        GameRound round = requireRound(roundId);
        Room room = roomMapper.selectById(round.getRoomId());
        assertOwner(room, ownerUserId);
        if (RoomConstants.ROOM_WAITING.equals(room.getStatus())) {
            throw new BizException(ErrorCode.ROOM_STATE_ERROR, "等待中的房间没有历史局可修改");
        }
        // 先保存修改前快照，方便操作日志追踪。
        Object before = getDetail(roundId);
        // 修改历史局采用整体替换策略：先删除旧明细，再保存新明细。
        paymentMapper.delete(new LambdaQueryWrapper<ScorePayment>().eq(ScorePayment::getRoundId, roundId));
        saveHistoryPayments(round, ownerUserId, request.getPayments());
        round.setStatus(RoomConstants.ROUND_SETTLED);
        round.setDeleted(0);
        if (round.getSettledAt() == null) {
            round.setSettledAt(LocalDateTime.now());
        }
        gameRoundMapper.updateById(round);
        statsRecalculationService.recalculateRoom(room.getId());
        writeLog(room.getId(), ownerUserId, "UPDATE_HISTORY_ROUND", null, roundId, before, request, "修改历史局");
        broadcastRecalculated(room.getId(), round.getRoundNo());
    }

    // 房主删除历史局：逻辑删除后触发整房间重算。
    @Transactional
    public void deleteHistoryRound(Long ownerUserId, Long roundId) {
        GameRound round = requireRound(roundId);
        Room room = roomMapper.selectById(round.getRoomId());
        assertOwner(room, ownerUserId);
        Object before = getDetail(roundId);
        round.setStatus(RoomConstants.ROUND_DELETED);
        round.setDeleted(1);
        gameRoundMapper.updateById(round);
        statsRecalculationService.recalculateRoom(room.getId());
        writeLog(room.getId(), ownerUserId, "DELETE_HISTORY_ROUND", null, roundId, before, null, "删除历史局");
        broadcastRecalculated(room.getId(), round.getRoundNo());
    }

    // 房主撤销上一局，实际是找到最新已结算局并逻辑删除。
    @Transactional
    public void undoLast(Long ownerUserId, Long roomId) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw new BizException(ErrorCode.ROOM_NOT_FOUND, "房间不存在");
        }
        assertOwner(room, ownerUserId);
        GameRound round = gameRoundMapper.selectOne(new LambdaQueryWrapper<GameRound>()
            .eq(GameRound::getRoomId, roomId)
            .eq(GameRound::getStatus, RoomConstants.ROUND_SETTLED)
            .eq(GameRound::getDeleted, 0)
            .orderByDesc(GameRound::getRoundNo)
            .last("limit 1"));
        if (round == null) {
            throw new BizException("暂无可撤销的历史局");
        }
        deleteHistoryRound(ownerUserId, round.getId());
        writeLog(roomId, ownerUserId, "UNDO_LAST_ROUND", null, round.getId(), null, null, "撤销上一局");
    }

    // 保存普通提交/房主代交的输分记录。
    private void savePayments(GameRound round, RoundParticipant fromParticipant, Long createdBy,
                              List<RoundDtos.PaymentRequest> requests, boolean ownerSubmit) {
        Long fromUserId = fromParticipant.getUserId();
        // 当前局参与者按 userId 建索引，用来校验收分玩家是否合法。
        Map<Long, RoundParticipant> participantByUser = participantMapper.selectList(new LambdaQueryWrapper<RoundParticipant>().eq(RoundParticipant::getRoundId, round.getId()))
            .stream()
            .filter(participant -> participant.getUserId() != null)
            .collect(Collectors.toMap(RoundParticipant::getUserId, Function.identity()));

        // 重新提交时，先删除该玩家之前作为输家的所有记录，再写入新记录。
        paymentMapper.delete(new LambdaQueryWrapper<ScorePayment>()
            .eq(ScorePayment::getRoundId, round.getId())
            .eq(ScorePayment::getFromUserId, fromUserId));

        // 允许前端传多条同一赢家记录，落库前按赢家合并，减少结算和历史展示的歧义。
        Map<Long, MergedPayment> merged = new LinkedHashMap<>();
        for (RoundDtos.PaymentRequest request : requests == null ? List.<RoundDtos.PaymentRequest>of() : requests) {
            if (request.getScore() == null || request.getScore() <= 0) {
                throw new BizException("分数必须为正整数");
            }
            if (Objects.equals(fromUserId, request.getToUserId())) {
                // 不能输给自己。
                throw new BizException("不能输给自己");
            }
            RoundParticipant toParticipant = participantByUser.get(request.getToUserId());
            if (toParticipant == null) {
                // 收分玩家必须是本局参与者。
                throw new BizException("收分玩家不是本轮参与者");
            }
            merged.computeIfAbsent(request.getToUserId(), ignored -> new MergedPayment(toParticipant))
                .add(request.getScore(), request.getRemark());
        }

        for (MergedPayment mergedPayment : merged.values()) {
            ScorePayment payment = new ScorePayment();
            payment.setRoomId(round.getRoomId());
            payment.setRoundId(round.getId());
            payment.setFromUserId(fromUserId);
            payment.setFromSeatId(fromParticipant.getSeatId());
            payment.setToUserId(mergedPayment.toParticipant.getUserId());
            payment.setToSeatId(mergedPayment.toParticipant.getSeatId());
            payment.setScore(mergedPayment.score);
            payment.setRemark(mergedPayment.remark());
            payment.setCreatedBy(createdBy);
            payment.setIsOwnerSubmit(ownerSubmit ? 1 : 0);
            paymentMapper.insert(payment);
        }
    }

    // 保存历史局修改时的输分记录，历史修改需要同时指定输家和赢家。
    private void saveHistoryPayments(GameRound round, Long createdBy, List<RoundDtos.HistoryPaymentRequest> requests) {
        Map<Long, RoundParticipant> participantByUser = participantMapper.selectList(new LambdaQueryWrapper<RoundParticipant>().eq(RoundParticipant::getRoundId, round.getId()))
            .stream()
            .filter(participant -> participant.getUserId() != null)
            .collect(Collectors.toMap(RoundParticipant::getUserId, Function.identity(), (a, b) -> a));
        for (RoundDtos.HistoryPaymentRequest request : requests == null ? List.<RoundDtos.HistoryPaymentRequest>of() : requests) {
            if (Objects.equals(request.getFromUserId(), request.getToUserId())) {
                throw new BizException("不能输给自己");
            }
            // 输家和赢家都必须是这局参与者。
            RoundParticipant from = participantByUser.get(request.getFromUserId());
            RoundParticipant to = participantByUser.get(request.getToUserId());
            if (from == null || to == null) {
                throw new BizException("历史局只能在该局参与者之间修改");
            }
            ScorePayment payment = new ScorePayment();
            payment.setRoomId(round.getRoomId());
            payment.setRoundId(round.getId());
            payment.setFromUserId(from.getUserId());
            payment.setFromSeatId(from.getSeatId());
            payment.setToUserId(to.getUserId());
            payment.setToSeatId(to.getSeatId());
            payment.setScore(request.getScore());
            payment.setRemark(request.getRemark());
            payment.setCreatedBy(createdBy);
            payment.setIsOwnerSubmit(1);
            paymentMapper.insert(payment);
        }
    }

    // 判断一局是否全员完成提交。
    private boolean allSubmitted(Long roundId) {
        return participantMapper.selectList(new LambdaQueryWrapper<RoundParticipant>().eq(RoundParticipant::getRoundId, roundId))
            .stream()
            .allMatch(participant -> !RoomConstants.SUBMIT_PENDING.equals(participant.getSubmitStatus()));
    }

    // 查询轮次，不存在时抛业务异常。
    private GameRound requireRound(Long roundId) {
        GameRound round = gameRoundMapper.selectById(roundId);
        if (round == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "轮次不存在");
        }
        return round;
    }

    // 查询并校验轮次仍处于提交中。
    private GameRound requireSubmittingRound(Long roundId) {
        GameRound round = requireRound(roundId);
        if (!RoomConstants.ROUND_SUBMITTING.equals(round.getStatus())) {
            throw new BizException(ErrorCode.ROUND_STATE_ERROR, "本轮已结算，不能修改提交");
        }
        return round;
    }

    // 查询并校验房间仍在计分中。
    private Room requirePlayingRoom(Long roomId) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw new BizException(ErrorCode.ROOM_NOT_FOUND, "房间不存在");
        }
        if (!RoomConstants.ROOM_PLAYING.equals(room.getStatus())) {
            throw new BizException(ErrorCode.ROOM_STATE_ERROR, "房间未在计分中");
        }
        return room;
    }

    // 校验操作者是否房主。
    private void assertOwner(Room room, Long userId) {
        if (!Objects.equals(room.getOwnerUserId(), userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅房主可操作");
        }
    }

    // 根据 roundId 和 userId 找到本局参与记录。
    private RoundParticipant findParticipant(Long roundId, Long userId) {
        return participantMapper.selectOne(new LambdaQueryWrapper<RoundParticipant>()
            .eq(RoundParticipant::getRoundId, roundId)
            .eq(RoundParticipant::getUserId, userId)
            .last("limit 1"));
    }

    // 组装提交响应。
    private RoundDtos.SubmitRoundResponse submitResponse(Long roundId, String status, boolean allSubmitted) {
        RoundDtos.SubmitRoundResponse response = new RoundDtos.SubmitRoundResponse();
        response.setRoundId(roundId);
        response.setSubmitStatus(status);
        response.setAllSubmitted(allSubmitted);
        return response;
    }

    // 获取房间当前开启段。
    private RoomSession getCurrentSession(Long roomId) {
        return sessionMapper.selectOne(new LambdaQueryWrapper<RoomSession>()
            .eq(RoomSession::getRoomId, roomId)
            .isNull(RoomSession::getEndTime)
            .orderByDesc(RoomSession::getSessionNo)
            .last("limit 1"));
    }

    // 构造 WebSocket 结算消息内容，包含已结算局和下一局信息。
    private Map<String, Object> buildSettledPayload(GameRound settledRound, GameRound nextRound) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("settledRound", Map.of(
            "roundId", settledRound.getId(),
            "roundNo", settledRound.getRoundNo(),
            "summary", buildSummary(settledRound.getId())
        ));
        if (nextRound != null) {
            payload.put("nextRound", Map.of(
                "roundId", nextRound.getId(),
                "roundNo", nextRound.getRoundNo(),
                "status", nextRound.getStatus()
            ));
        }
        return payload;
    }

    // 构建某局结算摘要，按净分从高到低排序。
    private List<RoundDtos.RoundSummaryItem> buildSummary(Long roundId) {
        List<RoundParticipant> participants = participantMapper.selectList(new LambdaQueryWrapper<RoundParticipant>().eq(RoundParticipant::getRoundId, roundId));
        Map<Long, SysUser> users = loadUsers(participants.stream().map(RoundParticipant::getUserId).filter(Objects::nonNull).collect(Collectors.toSet()));
        return participants.stream()
            .sorted(Comparator.comparing((RoundParticipant participant) -> participant.getNetScore() == null ? 0 : participant.getNetScore()).reversed())
            .map(participant -> {
                RoundDtos.RoundSummaryItem item = new RoundDtos.RoundSummaryItem();
                item.setUserId(participant.getUserId());
                SysUser user = users.get(participant.getUserId());
                item.setNickname(user == null ? "离席玩家" : user.getNickname());
                item.setNetScore(participant.getNetScore());
                return item;
            })
            .toList();
    }

    // 构建某局所有输分明细的展示对象。
    private List<RoomDtos.PaymentView> buildPaymentViews(Long roundId) {
        List<ScorePayment> payments = paymentMapper.selectList(new LambdaQueryWrapper<ScorePayment>().eq(ScorePayment::getRoundId, roundId));
        Set<Long> userIds = new HashSet<>();
        Set<Long> seatIds = new HashSet<>();
        payments.forEach(payment -> {
            userIds.add(payment.getFromUserId());
            userIds.add(payment.getToUserId());
            seatIds.add(payment.getFromSeatId());
            seatIds.add(payment.getToSeatId());
        });
        Map<Long, SysUser> users = loadUsers(userIds);
        // 座位信息只用于展示收分玩家当时的座位名。
        Map<Long, RoomSeat> seats = seatIds.isEmpty()
            ? Map.of()
            : seatMapper.selectBatchIds(seatIds).stream().collect(Collectors.toMap(RoomSeat::getId, Function.identity()));
        return payments.stream().map(payment -> {
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
        }).toList();
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

    // 广播历史局被修改/删除后的重算通知。
    private void broadcastRecalculated(Long roomId, Integer fromRoundNo) {
        socketSessionManager.broadcastToRoom(roomId, WsMessage.of(WsMessageType.ROUND_RECALCULATED, roomId, Map.of(
            "fromRoundNo", fromRoundNo,
            "message", "房主修改了第" + fromRoundNo + "局，系统已重算后续分数"
        )));
    }

    // 写入房间操作日志。
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

    // 把对象转成 JSON 供日志保存。
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

    // 临时合并对象：同一个输家输给同一个赢家的多条记录，在落库前合并为一条。
    private static class MergedPayment {
        private final RoundParticipant toParticipant;
        // 合并后的备注列表。
        private final List<String> remarks = new ArrayList<>();
        // 合并后的总分。
        private int score;

        // 保存收分玩家的参与记录，落库时要取 userId 和 seatId。
        private MergedPayment(RoundParticipant toParticipant) {
            this.toParticipant = toParticipant;
        }

        // 累加分数和备注。
        private void add(int value, String remark) {
            score += value;
            if (StringUtils.hasText(remark)) {
                remarks.add(remark);
            }
        }

        // 多条备注用中文分号拼接，没有备注则返回 null。
        private String remark() {
            return remarks.isEmpty() ? null : String.join("；", remarks);
        }
    }
}
