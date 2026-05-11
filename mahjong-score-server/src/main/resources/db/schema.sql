-- 数据库建表脚本：执行后会创建麻将计分小程序后端需要的全部业务表。
-- 说明：每个字段后的 COMMENT 是数据库层面的注释，方便用数据库工具查看表结构。

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    openid VARCHAR(128) NOT NULL COMMENT '微信openid',
    phone VARCHAR(32) DEFAULT NULL COMMENT '绑定手机号，第一版不可换绑',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '全局昵称',
    avatar_url VARCHAR(512) DEFAULT NULL COMMENT '微信头像',
    total_score INT NOT NULL DEFAULT 0 COMMENT '累计净分，冗余统计字段',
    total_rounds INT NOT NULL DEFAULT 0 COMMENT '累计参与轮数，冗余统计字段',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_openid (openid),
    UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS room (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '房间ID',
    room_code VARCHAR(16) NOT NULL COMMENT '6位数字房间号',
    room_name VARCHAR(64) NOT NULL COMMENT '房间名称，默认麻将房间+房间号',
    owner_user_id BIGINT NOT NULL COMMENT '房主用户ID',
    player_count TINYINT NOT NULL COMMENT '房间人数：2-4',
    initial_score INT NOT NULL DEFAULT 0 COMMENT '初始分',
    status VARCHAR(16) NOT NULL COMMENT 'WAITING/PLAYING/CLOSED',
    current_round_no INT NOT NULL DEFAULT 0 COMMENT '已完成的最大轮次号',
    last_score_time DATETIME DEFAULT NULL COMMENT '最后一轮成功结算时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_room_code (room_code),
    KEY idx_owner_user_id (owner_user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间表';

CREATE TABLE IF NOT EXISTS room_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '房间开启段ID',
    room_id BIGINT NOT NULL COMMENT '房间ID',
    session_no INT NOT NULL COMMENT '第几次开启',
    start_time DATETIME NOT NULL COMMENT '开启时间',
    end_time DATETIME DEFAULT NULL COMMENT '关闭时间',
    start_round_no INT NOT NULL DEFAULT 0 COMMENT '开启时已完成轮次',
    end_round_no INT DEFAULT NULL COMMENT '关闭时已完成轮次',
    close_reason VARCHAR(32) DEFAULT NULL COMMENT 'OWNER_CLOSE/AUTO_CLOSE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_room_id (room_id),
    KEY idx_room_session (room_id, session_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间开启时间段表';

CREATE TABLE IF NOT EXISTS room_seat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '座位ID',
    room_id BIGINT NOT NULL COMMENT '房间ID',
    seat_no TINYINT NOT NULL COMMENT '座位序号：1-4',
    seat_name VARCHAR(16) NOT NULL COMMENT 'EAST/SOUTH/WEST/NORTH',
    current_user_id BIGINT DEFAULT NULL COMMENT '当前座位用户，为空表示空位',
    current_score INT NOT NULL DEFAULT 0 COMMENT '当前座位分数，用于继承',
    joined_at DATETIME DEFAULT NULL COMMENT '当前用户加入该座位时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_room_seat (room_id, seat_no),
    KEY idx_room_id (room_id),
    KEY idx_current_user_id (current_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间座位表';

CREATE TABLE IF NOT EXISTS room_seat_user_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '座位用户历史ID',
    room_id BIGINT NOT NULL COMMENT '房间ID',
    seat_id BIGINT NOT NULL COMMENT '座位ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    room_nickname VARCHAR(64) DEFAULT NULL COMMENT '本房间昵称',
    join_round_no INT NOT NULL COMMENT '从哪一轮之后开始统计',
    leave_round_no INT DEFAULT NULL COMMENT '离开时最后参与轮次',
    join_time DATETIME NOT NULL COMMENT '加入时间',
    leave_time DATETIME DEFAULT NULL COMMENT '离开时间',
    leave_reason VARCHAR(32) DEFAULT NULL COMMENT 'QUIT/KICKED/OWNER_TRANSFER_QUIT',
    active TINYINT NOT NULL DEFAULT 1 COMMENT '是否当前仍在座位',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_room_id (room_id),
    KEY idx_user_id (user_id),
    KEY idx_seat_id (seat_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位用户历史表';

CREATE TABLE IF NOT EXISTS game_round (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '轮次ID',
    room_id BIGINT NOT NULL COMMENT '房间ID',
    session_id BIGINT NOT NULL COMMENT '所属开启段ID',
    round_no INT NOT NULL COMMENT '轮次编号，从1递增',
    status VARCHAR(16) NOT NULL COMMENT 'SUBMITTING/SETTLED/DELETED',
    settled_at DATETIME DEFAULT NULL COMMENT '结算时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_room_round (room_id, round_no),
    KEY idx_room_id (room_id),
    KEY idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮次表';

CREATE TABLE IF NOT EXISTS round_participant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '轮次参与者ID',
    room_id BIGINT NOT NULL,
    round_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    user_id BIGINT DEFAULT NULL COMMENT '该轮对应用户，退出者当前轮仍可保留',
    submit_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUBMITTED/OWNER_SUBMITTED/FORCED_SUBMITTED',
    submitted_by BIGINT DEFAULT NULL COMMENT '提交人用户ID，代提交时为房主ID',
    submitted_at DATETIME DEFAULT NULL,
    net_score INT NOT NULL DEFAULT 0 COMMENT '本轮净分',
    active_for_stats TINYINT NOT NULL DEFAULT 1 COMMENT '是否计入个人战绩',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_round_seat (round_id, seat_id),
    KEY idx_round_id (round_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮次参与者表';

CREATE TABLE IF NOT EXISTS score_payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '输分记录ID',
    room_id BIGINT NOT NULL,
    round_id BIGINT NOT NULL,
    from_user_id BIGINT NOT NULL COMMENT '输钱用户',
    from_seat_id BIGINT NOT NULL COMMENT '输钱座位',
    to_user_id BIGINT NOT NULL COMMENT '收钱用户',
    to_seat_id BIGINT NOT NULL COMMENT '收钱座位',
    score INT NOT NULL COMMENT '输分，正整数',
    remark VARCHAR(255) DEFAULT NULL COMMENT '可选备注',
    created_by BIGINT NOT NULL COMMENT '创建人，自己提交或房主代提交',
    is_owner_submit TINYINT NOT NULL DEFAULT 0 COMMENT '是否房主代提交',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_round_id (round_id),
    KEY idx_from_user_id (from_user_id),
    KEY idx_to_user_id (to_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='输分记录表';

CREATE TABLE IF NOT EXISTS user_room_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户房间统计ID',
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    total_score INT NOT NULL DEFAULT 0 COMMENT '该用户在该房间实际参与净分',
    total_rounds INT NOT NULL DEFAULT 0 COMMENT '实际参与轮数',
    win_rounds INT NOT NULL DEFAULT 0,
    lose_rounds INT NOT NULL DEFAULT 0,
    draw_rounds INT NOT NULL DEFAULT 0,
    positive_score_sum INT NOT NULL DEFAULT 0,
    negative_score_sum INT NOT NULL DEFAULT 0,
    highest_round_score INT DEFAULT NULL,
    lowest_round_score INT DEFAULT NULL,
    first_join_time DATETIME DEFAULT NULL,
    last_play_time DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_room_user (room_id, user_id),
    KEY idx_user_id (user_id),
    KEY idx_room_id (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户房间统计表';

CREATE TABLE IF NOT EXISTS user_daily_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户每日统计ID',
    user_id BIGINT NOT NULL,
    stat_date DATE NOT NULL COMMENT '统计日期',
    total_score INT NOT NULL DEFAULT 0,
    total_rounds INT NOT NULL DEFAULT 0,
    win_rounds INT NOT NULL DEFAULT 0,
    positive_score_sum INT NOT NULL DEFAULT 0,
    negative_score_sum INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_date (user_id, stat_date),
    KEY idx_stat_date (stat_date),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户每日统计表';

CREATE TABLE IF NOT EXISTS room_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '操作日志ID',
    room_id BIGINT NOT NULL,
    operator_user_id BIGINT NOT NULL COMMENT '操作人',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    target_user_id BIGINT DEFAULT NULL COMMENT '目标用户',
    target_round_id BIGINT DEFAULT NULL COMMENT '目标轮次',
    before_data JSON DEFAULT NULL COMMENT '操作前数据',
    after_data JSON DEFAULT NULL COMMENT '操作后数据',
    remark VARCHAR(255) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_room_id (room_id),
    KEY idx_operator_user_id (operator_user_id),
    KEY idx_operation_type (operation_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间操作日志表';
