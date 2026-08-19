package com.xinsulu.common.enums;

/**
 * 报表类型枚举
 *
 * @author xinsulu-team
 */
public enum ReportType {

    /** 资产负债表 */
    BALANCE_SHEET("BALANCE_SHEET", "资产负债表"),

    /** 利润表 */
    INCOME_STATEMENT("INCOME_STATEMENT", "利润表"),

    /** 现金流量表 */
    CASH_FLOW_STATEMENT("CASH_FLOW_STATEMENT", "现金流量表");

    private final String code;
    private final String description;

    ReportType(String code, String description) {
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
     *
     * @param code 编码
     * @return 枚举值，未找到返回null
     */
    public static ReportType fromCode(String code) {
        for (ReportType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
