package com.tick.entity.dto;

/**
 * 排行榜的排序维度。
 * <p>
 * 每个维度对应一张（或一组）已有的业务表，都是**累计口径**：
 * 积分取积分总账、学习时长取学习流水求和、签到天数取签到记录计数。
 * <p>
 * 新增维度时只要在这里加一个枚举值、在 {@code LeaderboardMapper} 里补一条查询、
 * 并在 {@code LeaderboardServiceImpl} 的分支里接上即可；接口与前端会自动跟着变
 * （前端从 GET /api/leaderboard/metrics 读取可选维度）。
 */
public enum LeaderboardMetric {

    /** 积分总账，每用户一行 */
    POINTS("points", "积分", "分"),
    /** 课程学习流水，按用户求和（单位：秒） */
    STUDY("study", "学习时长", "秒"),
    /** 签到记录，按用户计数 */
    CHECK_IN("check-in", "签到天数", "天");

    private final String code;
    private final String label;
    private final String unit;

    LeaderboardMetric(String code, String label, String unit) {
        this.code = code;
        this.label = label;
        this.unit = unit;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /** 数值单位；学习时长的 value 是秒，由前端格式化成「x 小时 y 分钟」。 */
    public String getUnit() {
        return unit;
    }

    public static LeaderboardMetric fromCode(String code) {
        if (code != null) {
            String normalized = code.trim();
            for (LeaderboardMetric metric : values()) {
                if (metric.code.equalsIgnoreCase(normalized)) {
                    return metric;
                }
            }
        }
        throw new IllegalArgumentException("不支持的排行榜维度，可选值：" + codes());
    }

    public static String codes() {
        StringBuilder builder = new StringBuilder();
        for (LeaderboardMetric metric : values()) {
            if (builder.length() > 0) {
                builder.append('、');
            }
            builder.append(metric.code);
        }
        return builder.toString();
    }
}
