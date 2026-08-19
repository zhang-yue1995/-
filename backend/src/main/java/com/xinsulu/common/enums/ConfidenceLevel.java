package com.xinsulu.common.enums;

/**
 * OCR置信度等级枚举
 *
 * @author xinsulu-team
 */
public enum ConfidenceLevel {

    /** 高置信度 - >= 0.9 */
    HIGH("HIGH", "高置信度", 0.9, 1.0),

    /** 中置信度 - 0.7-0.9 */
    MEDIUM("MEDIUM", "中置信度", 0.7, 0.9),

    /** 低置信度 - < 0.7 */
    LOW("LOW", "低置信度", 0.0, 0.7);

    private final String code;
    private final String description;
    private final double minThreshold;
    private final double maxThreshold;

    ConfidenceLevel(String code, String description, double minThreshold, double maxThreshold) {
        this.code = code;
        this.description = description;
        this.minThreshold = minThreshold;
        this.maxThreshold = maxThreshold;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public double getMinThreshold() {
        return minThreshold;
    }

    public double getMaxThreshold() {
        return maxThreshold;
    }

    /**
     * 根据置信度分数获取等级
     *
     * @param confidence 置信度分数
     * @return 置信度等级
     */
    public static ConfidenceLevel fromScore(double confidence) {
        if (confidence >= HIGH.minThreshold) {
            return HIGH;
        } else if (confidence >= MEDIUM.minThreshold) {
            return MEDIUM;
        } else {
            return LOW;
        }
    }
}
