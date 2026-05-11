package com.example.mahjong.module.stats.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mahjong.common.constant.RoomConstants;
import com.example.mahjong.module.room.entity.Room;
import com.example.mahjong.module.room.mapper.RoomMapper;
import com.example.mahjong.module.round.entity.GameRound;
import com.example.mahjong.module.round.entity.RoundParticipant;
import com.example.mahjong.module.round.mapper.GameRoundMapper;
import com.example.mahjong.module.round.mapper.RoundParticipantMapper;
import com.example.mahjong.module.stats.dto.StatsDtos;
import com.example.mahjong.module.stats.entity.UserRoomStats;
import com.example.mahjong.module.stats.mapper.UserRoomStatsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
// 战绩查询服务：根据已经重算好的统计表和轮次数据，组装个人战绩页。
public class StatsService {

    private final UserRoomStatsMapper userRoomStatsMapper;
    private final RoomMapper roomMapper;
    private final GameRoundMapper gameRoundMapper;
    private final RoundParticipantMapper participantMapper;

    // 查询当前用户个人战绩，range 可为 ALL、MONTH、WEEK。
    public StatsDtos.MyStatsResponse getMyStats(Long userId, String range) {
        LocalDateTime start = resolveStart(range);
        // 先找出该用户参与过、且需要计入统计的轮次，再按时间范围过滤。
        List<RoundParticipant> myParticipants = participantMapper.selectList(new LambdaQueryWrapper<RoundParticipant>()
                .eq(RoundParticipant::getUserId, userId)
                .eq(RoundParticipant::getActiveForStats, 1))
            .stream()
            .filter(participant -> roundInRange(participant.getRoundId(), start))
            .toList();

        // 从轮次参与记录中计算总局数、总净分、胜局等基础指标。
        int totalRounds = myParticipants.size();
        int totalScore = myParticipants.stream().mapToInt(participant -> participant.getNetScore() == null ? 0 : participant.getNetScore()).sum();
        int wins = (int) myParticipants.stream().filter(participant -> safeNet(participant) > 0).count();
        int positive = myParticipants.stream().mapToInt(this::safeNet).filter(value -> value > 0).sum();
        int negative = myParticipants.stream().mapToInt(this::safeNet).filter(value -> value < 0).map(Math::abs).sum();
        Integer highest = myParticipants.stream().map(this::safeNet).max(Integer::compareTo).orElse(null);
        Integer lowest = myParticipants.stream().map(this::safeNet).min(Integer::compareTo).orElse(null);

        // 组装前端个人战绩页面需要的响应。
        StatsDtos.MyStatsResponse response = new StatsDtos.MyStatsResponse();
        response.setTotalRooms((int) myParticipants.stream().map(RoundParticipant::getRoomId).distinct().count());
        response.setTotalRounds(totalRounds);
        response.setTotalScore(totalScore);
        response.setAvgRoundScore(totalRounds == 0 ? 0D : round2((double) totalScore / totalRounds));
        response.setHighestRoundScore(highest);
        response.setLowestRoundScore(lowest);
        response.setRoundWinRate(totalRounds == 0 ? 0D : round2((double) wins / totalRounds));
        response.setRoomWinRate(calculateRoomWinRate(userId, start));
        response.setProfitLossRatio(formatProfitLossRatio(positive, negative));
        response.setRecentRooms(buildRecentRooms(userId, start));
        return response;
    }

    // 构建最近房间列表，最多返回 10 个。
    private List<StatsDtos.RecentRoomItem> buildRecentRooms(Long userId, LocalDateTime start) {
        List<UserRoomStats> statsList = userRoomStatsMapper.selectList(new LambdaQueryWrapper<UserRoomStats>()
                .eq(UserRoomStats::getUserId, userId))
            .stream()
            .filter(stats -> start == null || (stats.getLastPlayTime() != null && !stats.getLastPlayTime().isBefore(start)))
            .sorted(Comparator.comparing(UserRoomStats::getLastPlayTime, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(10)
            .toList();
        Set<Long> roomIds = statsList.stream().map(UserRoomStats::getRoomId).collect(Collectors.toSet());
        // 批量查房间信息，避免每个最近房间查一次数据库。
        Map<Long, Room> rooms = roomIds.isEmpty()
            ? Map.of()
            : roomMapper.selectBatchIds(roomIds).stream().collect(Collectors.toMap(Room::getId, Function.identity()));
        return statsList.stream().map(stats -> {
            Room room = rooms.get(stats.getRoomId());
            StatsDtos.RecentRoomItem item = new StatsDtos.RecentRoomItem();
            item.setRoomId(stats.getRoomId());
            item.setRoomName(room == null ? null : room.getRoomName());
            item.setStatus(room == null ? null : room.getStatus());
            item.setMyScore(stats.getTotalScore());
            item.setMyRank(calculateMyRank(stats.getRoomId(), userId));
            item.setRounds(stats.getTotalRounds());
            item.setLastPlayTime(stats.getLastPlayTime());
            return item;
        }).toList();
    }

    // 房间胜率：参与过的房间中，自己的总分是否等于该房间最高分。
    private double calculateRoomWinRate(Long userId, LocalDateTime start) {
        List<UserRoomStats> myRooms = userRoomStatsMapper.selectList(new LambdaQueryWrapper<UserRoomStats>().eq(UserRoomStats::getUserId, userId))
            .stream()
            .filter(stats -> start == null || (stats.getLastPlayTime() != null && !stats.getLastPlayTime().isBefore(start)))
            .toList();
        if (myRooms.isEmpty()) {
            return 0D;
        }
        int winRooms = 0;
        for (UserRoomStats myStats : myRooms) {
            int max = userRoomStatsMapper.selectList(new LambdaQueryWrapper<UserRoomStats>().eq(UserRoomStats::getRoomId, myStats.getRoomId()))
                .stream()
                .mapToInt(UserRoomStats::getTotalScore)
                .max()
                .orElse(Integer.MIN_VALUE);
            if (myStats.getTotalScore() == max) {
                winRooms++;
            }
        }
        return round2((double) winRooms / myRooms.size());
    }

    // 计算当前用户在某个房间中的名次，同分并列。
    private int calculateMyRank(Long roomId, Long userId) {
        List<UserRoomStats> roomStats = userRoomStatsMapper.selectList(new LambdaQueryWrapper<UserRoomStats>().eq(UserRoomStats::getRoomId, roomId))
            .stream()
            .sorted(Comparator.comparing(UserRoomStats::getTotalScore).reversed())
            .toList();
        int rank = 1;
        Integer lastScore = null;
        for (int index = 0; index < roomStats.size(); index++) {
            UserRoomStats stats = roomStats.get(index);
            if (lastScore != null && !Objects.equals(lastScore, stats.getTotalScore())) {
                rank = index + 1;
            }
            if (Objects.equals(stats.getUserId(), userId)) {
                return rank;
            }
            lastScore = stats.getTotalScore();
        }
        return roomStats.size();
    }

    // 判断某局是否已经结算、未删除，并且落在查询时间范围内。
    private boolean roundInRange(Long roundId, LocalDateTime start) {
        GameRound round = gameRoundMapper.selectById(roundId);
        if (round == null || !RoomConstants.ROUND_SETTLED.equals(round.getStatus()) || !Objects.equals(round.getDeleted(), 0)) {
            return false;
        }
        return start == null || (round.getSettledAt() != null && !round.getSettledAt().isBefore(start));
    }

    // 空净分按 0 处理。
    private int safeNet(RoundParticipant participant) {
        return participant.getNetScore() == null ? 0 : participant.getNetScore();
    }

    // 把 range 字符串转换成统计起始时间；ALL 返回 null 表示不过滤。
    private LocalDateTime resolveStart(String range) {
        LocalDateTime now = LocalDateTime.now();
        if ("MONTH".equalsIgnoreCase(range)) {
            return now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        }
        if ("WEEK".equalsIgnoreCase(range)) {
            return now.toLocalDate().minusDays(now.getDayOfWeek().getValue() - 1L).atStartOfDay();
        }
        return null;
    }

    // 盈亏比 = 正分总和 / 负分绝对值总和；没有负分时按业务习惯展示特殊值。
    private String formatProfitLossRatio(int positive, int negative) {
        if (negative == 0) {
            return positive == 0 ? "--" : "∞";
        }
        return String.format("%.2f", (double) positive / negative);
    }

    // 保留两位小数。
    private double round2(double value) {
        return Math.round(value * 100D) / 100D;
    }
}
