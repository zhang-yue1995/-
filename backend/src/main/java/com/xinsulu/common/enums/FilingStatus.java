package com.xinsulu.common.enums;

/**
 * 报表归档状态枚举
 *
 * @author xinsulu-team
 */
public enum FilingStatus {

    /** 草稿 */
    DRAFT("DRAFT", "草稿"),

    /** 待审核 */
    PENDING_REVIEW("PENDING_REVIEW", "待审核"),

    /** 已审核 */
    REVIEWED("REVIEWED", "已审核"),

    /** 已批准 */
    APPROVED("APPROVED", "已批准"),

    /** 已驳回 */
    REJECTED("REJECTED", "已驳回");

    private final String code;
    private final String description;

    FilingStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据编码获取枚举
     */
    public static FilingStatus fromCode(String code) {
        for (FilingStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
