package com.xinsulu.common.enums;

/**
 * 风险等级枚举
 *
 * @author xinsulu-team
 */
public enum RiskLevel {

    /** 低风险 - 评分 >= 80 */
    LOW("LOW", "低风险", 80, 100),

    /** 正常 - 评分 60-79 */
    NORMAL("NORMAL", "正常", 60, 79),

    /** 关注 - 评分 40-59 */
    ATTENTION("ATTENTION", "关注", 40, 59),

    /** 高风险 - 评分 20-39 */
    HIGH("HIGH", "高风险", 20, 39),

    /** 危机 - 评分 < 20 */
    CRITICAL("CRITICAL", "危机", 0, 19);

    private final String code;
    private final String description;
    private final int minScore;
    private final int maxScore;

    RiskLevel(String code, String description, int minScore, int maxScore) {
        this.code = code;
        this.description = description;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public int getMinScore() {
        return minScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    /**
     * 根据健康评分获取风险等级
     *
     * @param score 健康评分
     * @return 风险等级
     */
    public static RiskLevel fromScore(int score) {
        if (score >= LOW.minScore) {
            return LOW;
        } else if (score >= NORMAL.minScore) {
            return NORMAL;
        } else if (score >= ATTENTION.minScore) {
            return ATTENTION;
        } else if (score >= HIGH.minScore) {
            return HIGH;
        } else {
            return CRITICAL;
        }
    }

    /**
     * 根据编码获取枚举
     */
    public static RiskLevel fromCode(String code) {
        for (RiskLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        return null;
    }
}
