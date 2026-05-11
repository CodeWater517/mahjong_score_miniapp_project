package com.example.mahjong.module.ranking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mahjong.module.ranking.dto.RankingDtos;
import com.example.mahjong.module.room.entity.RoomSeat;
import com.example.mahjong.module.room.mapper.RoomSeatMapper;
import com.example.mahjong.module.stats.entity.UserDailyStats;
import com.example.mahjong.module.stats.entity.UserRoomStats;
import com.example.mahjong.module.stats.mapper.UserDailyStatsMapper;
import com.example.mahjong.module.stats.mapper.UserRoomStatsMapper;
import com.example.mahjong.module.user.entity.SysUser;
import com.example.mahjong.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
// 排行榜业务服务：计算房间排行、首页总榜和月榜。
public class RankingService {

    private final RoomSeatMapper seatMapper;
    private final UserRoomStatsMapper userRoomStatsMapper;
    private final UserDailyStatsMapper userDailyStatsMapper;
    private final SysUserMapper sysUserMapper;

    // 房间排行：只展示当前还在座位上的玩家，并按该房间总净分排序。
    public List<RankingDtos.RankItem> getRoomRank(Long roomId) {
        List<RoomSeat> seats = seatMapper.selectList(new LambdaQueryWrapper<RoomSeat>()
            .eq(RoomSeat::getRoomId, roomId)
            .isNotNull(RoomSeat::getCurrentUserId));
        // 一次性查出该房间的用户统计，避免循环里重复查库。
        Map<Long, UserRoomStats> statsByUser = userRoomStatsMapper.selectList(new LambdaQueryWrapper<UserRoomStats>().eq(UserRoomStats::getRoomId, roomId))
            .stream()
            .collect(Collectors.toMap(UserRoomStats::getUserId, Function.identity()));
        Map<Long, SysUser> users = loadUsers(seats.stream().map(RoomSeat::getCurrentUserId).collect(Collectors.toList()));
        List<RankingDtos.RankItem> items = seats.stream().map(seat -> {
            // 如果玩家刚加入还没有统计记录，分数和局数按 0 展示。
            UserRoomStats stats = statsByUser.get(seat.getCurrentUserId());
            SysUser user = users.get(seat.getCurrentUserId());
            RankingDtos.RankItem item = new RankingDtos.RankItem();
            item.setUserId(seat.getCurrentUserId());
            item.setNickname(displayName(user));
            item.setAvatarUrl(user == null ? null : user.getAvatarUrl());
            item.setScore(stats == null ? 0 : stats.getTotalScore());
            item.setRounds(stats == null ? 0 : stats.getTotalRounds());
            return item;
        }).sorted(Comparator.comparing(RankingDtos.RankItem::getScore).reversed()).toList();
        return withRanks(items);
    }

    // 首页排行榜：历史总榜直接使用 sys_user 冗余字段，月榜使用每日统计汇总。
    public RankingDtos.HomeRankingResponse getHomeRanking() {
        RankingDtos.HomeRankingResponse response = new RankingDtos.HomeRankingResponse();
        // 历史总榜取 total_score 最高的 10 个用户。
        List<SysUser> totalUsers = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
            .orderByDesc(SysUser::getTotalScore)
            .last("limit 10"));
        response.setTotalRank(withRanks(totalUsers.stream().map(user -> {
            RankingDtos.RankItem item = new RankingDtos.RankItem();
            item.setUserId(user.getId());
            item.setNickname(displayName(user));
            item.setAvatarUrl(user.getAvatarUrl());
            item.setScore(user.getTotalScore());
            item.setRounds(user.getTotalRounds());
            return item;
        }).toList()));

        // 月榜从本月第一天开始，把每日统计按用户累加。
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        Map<Long, Integer> monthlyScore = new HashMap<>();
        for (UserDailyStats stats : userDailyStatsMapper.selectList(new LambdaQueryWrapper<UserDailyStats>().ge(UserDailyStats::getStatDate, monthStart))) {
            monthlyScore.merge(stats.getUserId(), stats.getTotalScore(), Integer::sum);
        }
        Map<Long, SysUser> users = loadUsers(monthlyScore.keySet().stream().toList());
        response.setMonthlyRank(withRanks(monthlyScore.entrySet().stream()
            .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .limit(10)
            .map(entry -> {
                SysUser user = users.get(entry.getKey());
                RankingDtos.RankItem item = new RankingDtos.RankItem();
                item.setUserId(entry.getKey());
                item.setNickname(displayName(user));
                item.setAvatarUrl(user == null ? null : user.getAvatarUrl());
                item.setScore(entry.getValue());
                item.setRounds(null);
                return item;
            })
            .toList()));
        return response;
    }

    // 给排好序的列表补名次；同分并列，下一名跳过对应位次。
    private List<RankingDtos.RankItem> withRanks(List<RankingDtos.RankItem> items) {
        int rank = 1;
        Integer lastScore = null;
        int index = 0;
        for (RankingDtos.RankItem item : items) {
            index++;
            if (lastScore != null && !Objects.equals(lastScore, item.getScore())) {
                rank = index;
            }
            item.setRank(rank);
            lastScore = item.getScore();
        }
        return items;
    }

    // 批量加载用户信息。
    private Map<Long, SysUser> loadUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return sysUserMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));
    }

    // 统一处理用户显示名，不存在或昵称为空时给默认名称。
    private String displayName(SysUser user) {
        if (user == null) {
            return "未知玩家";
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : "微信用户";
    }
}
