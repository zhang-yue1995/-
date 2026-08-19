package com.xinsulu.common.constant;

/**
 * 系统全局常量定义
 *
 * @author xinsulu-team
 */
public final class Constants {

    private Constants() {
        // 私有构造函数防止实例化
    }

    /** 成功状态码 */
    public static final int SUCCESS_CODE = 200;

    /** 失败状态码 */
    public static final int ERROR_CODE = 500;

    /** 参数校验失败状态码 */
    public static final int VALIDATION_ERROR_CODE = 400;

    /** 未授权状态码 */
    public static final int UNAUTHORIZED_CODE = 401;

    /** 禁止访问状态码 */
    public static final int FORBIDDEN_CODE = 403;

    /** 资源未找到状态码 */
    public static final int NOT_FOUND_CODE = 404;

    /** 默认页大小 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /** 最大页大小 */
    public static final int MAX_PAGE_SIZE = 100;

    /** 默认密码加密盐值（生产环境应使用配置文件） */
    public static final String PASSWORD_SALT = "xinsulu_2026";

    /** Token过期时间（小时）- 演示用，实际应使用JWT等 */
    public static final long TOKEN_EXPIRE_HOURS = 24;

    /** 文件上传最大大小（MB） */
    public static final long MAX_FILE_SIZE_MB = 50;

    /** OCR识别高置信度阈值 */
    public static final double OCR_HIGH_CONFIDENCE_THRESHOLD = 0.9;

    /** OCR识别中置信度阈值 */
    public static final double OCR_MEDIUM_CONFIDENCE_THRESHOLD = 0.7;

    /** 财务数据默认精度 - 保留2位小数 */
    public static final int FINANCIAL_SCALE = 2;

    /** 百分比精度 - 保留4位小数 */
    public static final int PERCENTAGE_SCALE = 4;

    /** BigDecimal舍入模式 - 四舍五入 */
    public static final int ROUNDING_MODE = java.math.RoundingMode.HALF_UP.ordinal();

    /** 健康评分满分 */
    public static final int HEALTH_SCORE_MAX = 100;

    /** 健康评分最低分 */
    public static final int HEALTH_SCORE_MIN = 0;
}
