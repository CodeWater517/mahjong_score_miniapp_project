package com.example.mahjong.module.stats.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mahjong.common.constant.RoomConstants;
import com.example.mahjong.module.room.entity.Room;
import com.example.mahjong.module.room.entity.RoomSeat;
import com.example.mahjong.module.room.mapper.RoomMapper;
import com.example.mahjong.module.room.mapper.RoomSeatMapper;
import com.example.mahjong.module.round.entity.GameRound;
import com.example.mahjong.module.round.entity.RoundParticipant;
import com.example.mahjong.module.round.entity.ScorePayment;
import com.example.mahjong.module.round.mapper.GameRoundMapper;
import com.example.mahjong.module.round.mapper.RoundParticipantMapper;
import com.example.mahjong.module.round.mapper.ScorePaymentMapper;
import com.example.mahjong.module.stats.entity.UserDailyStats;
import com.example.mahjong.module.stats.entity.UserRoomStats;
import com.example.mahjong.module.stats.mapper.UserDailyStatsMapper;
import com.example.mahjong.module.stats.mapper.UserRoomStatsMapper;
import com.example.mahjong.module.user.entity.SysUser;
import com.example.mahjong.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
// 统计重算服务：从已结算的输分流水重新推导座位分、个人房间统计、全局统计和每日统计。
public class StatsRecalculationService {

    private final RoomMapper roomMapper;
    private final RoomSeatMapper roomSeatMapper;
    private final GameRoundMapper gameRoundMapper;
    private final RoundParticipantMapper participantMapper;
    private final ScorePaymentMapper paymentMapper;
    private final UserRoomStatsMapper userRoomStatsMapper;
    private final UserDailyStatsMapper userDailyStatsMapper;
    private final SysUserMapper sysUserMapper;

    // 重算某个房间。历史局修改、删除、正常结算后都会走这里，保证结果一致。
    @Transactional
    public void recalculateRoom(Long roomId) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            return;
        }

        // 座位分从初始分重新开始推导，避免历史修改后只做增量扣减造成误差。
        List<RoomSeat> seats = roomSeatMapper.selectList(new LambdaQueryWrapper<RoomSeat>().eq(RoomSeat::getRoomId, roomId));
        Map<Long, Integer> seatScores = seats.stream().collect(Collectors.toMap(RoomSeat::getId, ignored -> room.getInitialScore()));
        // 按用户累计该房间内的统计。
        Map<Long, UserRoomStats> statsByUser = new HashMap<>();

        // 所有可重算字段都从已结算且未删除的 score_payment 重新推导，避免冗余字段漂移。
        List<GameRound> rounds = gameRoundMapper.selectList(new LambdaQueryWrapper<GameRound>()
            .eq(GameRound::getRoomId, roomId)
            .eq(GameRound::getStatus, RoomConstants.ROUND_SETTLED)
            .eq(GameRound::getDeleted, 0)
            .orderByAsc(GameRound::getRoundNo));

        for (GameRound round : rounds) {
            // 先算出这一局里每个用户/座位的净分变化。
            RoundScore score = calculateRoundScore(round);
            for (Map.Entry<Long, Integer> entry : score.seatNet.entrySet()) {
                seatScores.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
            for (RoundParticipant participant : score.participants) {
                // 回写该局每个参与者的净分。
                int net = participant.getUserId() == null ? 0 : score.userNet.getOrDefault(participant.getUserId(), 0);
                participant.setNetScore(net);
                participantMapper.updateById(participant);
                if (participant.getUserId() != null && Objects.equals(participant.getActiveForStats(), 1)) {
                    // 只有 activeForStats 为 1 的参与者才计入个人战绩。
                    UserRoomStats userStats = statsByUser.computeIfAbsent(participant.getUserId(), id -> newStats(roomId, id));
                    addRoundToStats(userStats, net, round.getSettledAt());
                }
            }
        }

        // 用重算出的座位分覆盖数据库当前座位分。
        for (RoomSeat seat : seats) {
            seat.setCurrentScore(seatScores.getOrDefault(seat.getId(), room.getInitialScore()));
            roomSeatMapper.updateById(seat);
        }
        // 当前完成局号也从有效历史局里重新取最大值。
        room.setCurrentRoundNo(rounds.stream().map(GameRound::getRoundNo).max(Integer::compareTo).orElse(0));
        roomMapper.updateById(room);

        // 该房间统计先删除再插入，逻辑简单且不会残留旧用户记录。
        userRoomStatsMapper.delete(new LambdaQueryWrapper<UserRoomStats>().eq(UserRoomStats::getRoomId, roomId));
        statsByUser.values().forEach(userRoomStatsMapper::insert);
        recalculateGlobalAggregates();
    }

    // 重算全局累计字段和每日统计。
    @Transactional
    public void recalculateGlobalAggregates() {
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().isNotNull(SysUser::getId));
        Map<Long, SysUser> userMap = users.stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));
        // 先把所有用户累计值清零，再从 user_room_stats 累加。
        userMap.values().forEach(user -> {
            user.setTotalScore(0);
            user.setTotalRounds(0);
        });

        for (UserRoomStats stats : userRoomStatsMapper.selectList(new LambdaQueryWrapper<UserRoomStats>().isNotNull(UserRoomStats::getId))) {
            SysUser user = userMap.get(stats.getUserId());
            if (user != null) {
                user.setTotalScore(user.getTotalScore() + stats.getTotalScore());
                user.setTotalRounds(user.getTotalRounds() + stats.getTotalRounds());
            }
        }
        userMap.values().forEach(sysUserMapper::updateById);

        // 每日统计不存 room_id，因此全量重建比局部扣减更可靠；MVP 阶段数据量可接受。
        userDailyStatsMapper.delete(new LambdaQueryWrapper<UserDailyStats>().isNotNull(UserDailyStats::getId));
        Map<String, UserDailyStats> dailyMap = new HashMap<>();
        // 遍历所有有效已结算局，按用户+日期聚合。
        List<GameRound> rounds = gameRoundMapper.selectList(new LambdaQueryWrapper<GameRound>()
            .eq(GameRound::getStatus, RoomConstants.ROUND_SETTLED)
            .eq(GameRound::getDeleted, 0));
        for (GameRound round : rounds) {
            if (round.getSettledAt() == null) {
                continue;
            }
            LocalDate date = round.getSettledAt().toLocalDate();
            for (RoundParticipant participant : participantMapper.selectList(new LambdaQueryWrapper<RoundParticipant>().eq(RoundParticipant::getRoundId, round.getId()))) {
                if (participant.getUserId() == null || !Objects.equals(participant.getActiveForStats(), 1)) {
                    continue;
                }
                String key = participant.getUserId() + ":" + date;
                UserDailyStats daily = dailyMap.computeIfAbsent(key, ignored -> newDaily(participant.getUserId(), date));
                int net = participant.getNetScore() == null ? 0 : participant.getNetScore();
                daily.setTotalScore(daily.getTotalScore() + net);
                daily.setTotalRounds(daily.getTotalRounds() + 1);
                if (net > 0) {
                    daily.setWinRounds(daily.getWinRounds() + 1);
                    daily.setPositiveScoreSum(daily.getPositiveScoreSum() + net);
                } else if (net < 0) {
                    daily.setNegativeScoreSum(daily.getNegativeScoreSum() + Math.abs(net));
                }
            }
        }
        dailyMap.values().forEach(userDailyStatsMapper::insert);
    }

    // 计算某一局的用户净分和座位净分。
    private RoundScore calculateRoundScore(GameRound round) {
        List<RoundParticipant> participants = participantMapper.selectList(new LambdaQueryWrapper<RoundParticipant>().eq(RoundParticipant::getRoundId, round.getId()));
        List<ScorePayment> payments = paymentMapper.selectList(new LambdaQueryWrapper<ScorePayment>().eq(ScorePayment::getRoundId, round.getId()));
        Map<Long, Integer> userNet = new HashMap<>();
        Map<Long, Integer> seatNet = new HashMap<>();
        for (ScorePayment payment : payments) {
            // 输家扣分，赢家加分；用户分和座位分分开统计。
            userNet.merge(payment.getFromUserId(), -payment.getScore(), Integer::sum);
            userNet.merge(payment.getToUserId(), payment.getScore(), Integer::sum);
            seatNet.merge(payment.getFromSeatId(), -payment.getScore(), Integer::sum);
            seatNet.merge(payment.getToSeatId(), payment.getScore(), Integer::sum);
        }
        return new RoundScore(participants, userNet, seatNet);
    }

    // 创建一个用户在房间内的统计初始对象。
    private UserRoomStats newStats(Long roomId, Long userId) {
        UserRoomStats stats = new UserRoomStats();
        stats.setRoomId(roomId);
        stats.setUserId(userId);
        stats.setTotalScore(0);
        stats.setTotalRounds(0);
        stats.setWinRounds(0);
        stats.setLoseRounds(0);
        stats.setDrawRounds(0);
        stats.setPositiveScoreSum(0);
        stats.setNegativeScoreSum(0);
        return stats;
    }

    // 把一局净分累加到用户房间统计里。
    private void addRoundToStats(UserRoomStats stats, int net, LocalDateTime settledAt) {
        stats.setTotalScore(stats.getTotalScore() + net);
        stats.setTotalRounds(stats.getTotalRounds() + 1);
        if (net > 0) {
            stats.setWinRounds(stats.getWinRounds() + 1);
            stats.setPositiveScoreSum(stats.getPositiveScoreSum() + net);
        } else if (net < 0) {
            stats.setLoseRounds(stats.getLoseRounds() + 1);
            stats.setNegativeScoreSum(stats.getNegativeScoreSum() + Math.abs(net));
        } else {
            stats.setDrawRounds(stats.getDrawRounds() + 1);
        }
        stats.setHighestRoundScore(stats.getHighestRoundScore() == null ? net : Math.max(stats.getHighestRoundScore(), net));
        stats.setLowestRoundScore(stats.getLowestRoundScore() == null ? net : Math.min(stats.getLowestRoundScore(), net));
        if (settledAt != null) {
            stats.setFirstJoinTime(stats.getFirstJoinTime() == null ? settledAt : min(stats.getFirstJoinTime(), settledAt));
            stats.setLastPlayTime(stats.getLastPlayTime() == null ? settledAt : max(stats.getLastPlayTime(), settledAt));
        }
    }

    // 创建一个用户每日统计初始对象。
    private UserDailyStats newDaily(Long userId, LocalDate date) {
        UserDailyStats stats = new UserDailyStats();
        stats.setUserId(userId);
        stats.setStatDate(date);
        stats.setTotalScore(0);
        stats.setTotalRounds(0);
        stats.setWinRounds(0);
        stats.setPositiveScoreSum(0);
        stats.setNegativeScoreSum(0);
        return stats;
    }

    // 返回两个时间中更早的一个。
    private LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        return Comparator.<LocalDateTime>naturalOrder().compare(a, b) <= 0 ? a : b;
    }

    // 返回两个时间中更晚的一个。
    private LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        return Comparator.<LocalDateTime>naturalOrder().compare(a, b) >= 0 ? a : b;
    }

    // 小型不可变数据载体，保存一局计算后的参与者、用户净分、座位净分。
    private record RoundScore(List<RoundParticipant> participants, Map<Long, Integer> userNet, Map<Long, Integer> seatNet) {
    }
}
