package com.example.mahjong.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mahjong.common.constant.RoomConstants;
import com.example.mahjong.module.room.entity.Room;
import com.example.mahjong.module.room.entity.RoomSession;
import com.example.mahjong.module.room.mapper.RoomMapper;
import com.example.mahjong.module.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
// 房间自动关闭任务：长时间没有计分的进行中房间会被系统关闭。
public class RoomAutoCloseTask {

    private final RoomMapper roomMapper;
    private final RoomService roomService;

    // 每 5 分钟执行一次。
    @Scheduled(cron = "0 */5 * * * ?")
    public void autoCloseInactiveRooms() {
        // 只扫描 PLAYING 房间，等待中或已关闭房间不需要处理。
        for (Room room : roomMapper.selectList(new LambdaQueryWrapper<Room>().eq(Room::getStatus, RoomConstants.ROOM_PLAYING))) {
            RoomSession session = roomService.getCurrentSession(room.getId());
            if (session == null) {
                continue;
            }
            // 优先按最后结算时间判断；还没有结算过时，按本次开启时间判断。
            LocalDateTime baseTime = room.getLastScoreTime() == null ? session.getStartTime() : room.getLastScoreTime();
            if (baseTime != null && Duration.between(baseTime, LocalDateTime.now()).toMinutes() >= 60) {
                // 60 分钟没有成功计分，自动关闭房间并写入关闭原因。
                roomService.autoCloseRoom(room);
            }
        }
    }
}
