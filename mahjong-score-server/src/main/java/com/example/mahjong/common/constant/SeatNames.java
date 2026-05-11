package com.example.mahjong.common.constant;

import java.util.List;

// 麻将座位工具类：数据库保存英文编码，页面展示中文东南西北。
public final class SeatNames {

    // 座位编码，seatNo 从 1 开始，对应 EAST/SOUTH/WEST/NORTH。
    public static final List<String> CODES = List.of("EAST", "SOUTH", "WEST", "NORTH");
    // 给用户看的中文座位名。
    public static final List<String> LABELS = List.of("东", "南", "西", "北");

    // 根据座位序号得到座位编码，例如 1 -> EAST。
    public static String code(int seatNo) {
        return CODES.get(seatNo - 1);
    }

    // 根据座位编码得到中文名，未知编码则原样返回。
    public static String label(String code) {
        int index = CODES.indexOf(code);
        return index >= 0 ? LABELS.get(index) : code;
    }

    // 工具类不允许实例化。
    private SeatNames() {
    }
}
