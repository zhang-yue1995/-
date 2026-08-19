package com.xinsulu.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * BigDecimal工具类
 * 用于金额计算、百分比计算等金融运算
 * 遵循阿里巴巴Java手册规范：禁止使用Double进行金额计算
 *
 * @author xinsulu-team
 */
public final class BigDecimalUtil {

    private BigDecimalUtil() {
        // 私有构造函数防止实例化
    }

    /** 默认精度 - 2位小数（用于金额） */
    public static final int DEFAULT_SCALE = 2;

    /** 百分比精度 - 4位小数 */
    public static final int PERCENTAGE_SCALE = 4;

    /** 指标值精度 - 4位小数 */
    public static final int INDICATOR_SCALE = 4;

    /** 默认舍入模式 - 四舍五入 */
    private static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * 加法运算
     *
     * @param v1 被加数
     * @param v2 加数
     * @return 和
     */
    public static BigDecimal add(BigDecimal v1, BigDecimal v2) {
        if (v1 == null) v1 = BigDecimal.ZERO;
        if (v2 == null) v2 = BigDecimal.ZERO;
        return v1.add(v2);
    }

    /**
     * 加法运算（多参数）
     *
     * @param values 加数数组
     * @return 和
     */
    public static BigDecimal add(BigDecimal... values) {
        BigDecimal result = BigDecimal.ZERO;
        if (values != null) {
            for (BigDecimal value : values) {
                result = result.add(value != null ? value : BigDecimal.ZERO);
            }
        }
        return result;
    }

    /**
     * 减法运算
     *
     * @param v1 被减数
     * @param v2 减数
     * @return 差
     */
    public static BigDecimal subtract(BigDecimal v1, BigDecimal v2) {
        if (v1 == null) v1 = BigDecimal.ZERO;
        if (v2 == null) v2 = BigDecimal.ZERO;
        return v1.subtract(v2);
    }

    /**
     * 乘法运算
     *
     * @param v1 被乘数
     * @param v2 乘数
     * @param scale 保留小数位数
     * @return 积
     */
    public static BigDecimal multiply(BigDecimal v1, BigDecimal v2, int scale) {
        if (v1 == null || v2 == null) return BigDecimal.ZERO;
        return v1.multiply(v2).setScale(scale, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 乘法运算（默认精度）
     */
    public static BigDecimal multiply(BigDecimal v1, BigDecimal v2) {
        return multiply(v1, v2, DEFAULT_SCALE);
    }

    /**
     * 除法运算（处理除零异常）
     *
     * @param v1 被除数
     * @param v2 除数
     * @param scale 保留小数位数
     * @return 商，如果除数为零返回null
     */
    public static BigDecimal divide(BigDecimal v1, BigDecimal v2, int scale) {
        if (v1 == null || v2 == null || v2.compareTo(BigDecimal.ZERO) == 0) {
            return null; // 除零或空值时返回null，调用方需判断
        }
        return v1.divide(v2, scale, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 除法运算（默认精度）
     */
    public static BigDecimal divide(BigDecimal v1, BigDecimal v2) {
        return divide(v1, v2, DEFAULT_SCALE);
    }

    /**
     * 计算百分比（结果乘以100）
     *
     * @param part 部分
     * @param total 总量
     * @return 百分比值（如 33.3333 表示 33.3333%）
     */
    public static BigDecimal percentage(BigDecimal part, BigDecimal total) {
        if (part == null || total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return part.multiply(new BigDecimal("100"))
                   .divide(total, PERCENTAGE_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 计算增长率
     *
     * @param currentValue 当前值
     * @param previousValue 上期值
     * @return 增长率（%），上期为零时返回null
     */
    public static BigDecimal growthRate(BigDecimal currentValue, BigDecimal previousValue) {
        if (currentValue == null || previousValue == null ||
            previousValue.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return subtract(currentValue, previousValue)
               .multiply(new BigDecimal("100"))
               .divide(previousValue, PERCENTAGE_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 安全转换字符串为BigDecimal
     *
     * @param value 字符串值
     * @return BigDecimal对象，转换失败返回null
     */
    public static BigDecimal toBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            // 去除千分位逗号
            String cleaned = value.replaceAll(",", "");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 设置精度并四舍五入
     *
     * @param value 原始值
     * @param scale 精度
     * @return 处理后的值
     */
    public static BigDecimal setScale(BigDecimal value, int scale) {
        if (value == null) return null;
        return value.setScale(scale, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 判断是否为空或零
     *
     * @param value 值
     * @return 是否为空或零
     */
    public static boolean isNullOrEmptyOrZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * 判断是否为正数
     *
     * @param value 值
     * @return 是否大于零
     */
    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 判断是否为负数
     *
     * @param value 值
     * @return 是否小于零
     */
    public static boolean isNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * 取绝对值
     *
     * @param value 值
     * @return 绝对值
     */
    public static BigDecimal abs(BigDecimal value) {
        if (value == null) return null;
        return value.abs();
    }

    /**
     * 比较两个值是否相等（在指定精度范围内）
     *
     * @param v1 值1
     * @param v2 值2
     * @param scale 精度
     * @return 是否相等
     */
    public static boolean isEqual(BigDecimal v1, BigDecimal v2, int scale) {
        if (v1 == null && v2 == null) return true;
        if (v1 == null || v2 == null) return false;
        return v1.setScale(scale, DEFAULT_ROUNDING_MODE)
              .compareTo(v2.setScale(scale, DEFAULT_ROUNDING_MODE)) == 0;
    }

    /**
     * 格式化显示金额（带单位）
     *
     * @param value 金额（万元）
     * @return 格式化字符串，如 "1,234.56万元"
     */
    public static String formatAmount(BigDecimal value) {
        if (value == null) return "0.00万元";
        return String.format("%,.2f万元", value.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE));
    }

    /**
     * 格式化显示百分比
     *
     * @param value 百分比值
     * @return 格式化字符串，如 "33.33%"
     */
    public static String formatPercentage(BigDecimal value) {
        if (value == null) return "N/A";
        return String.format("%.2f%%", value.setScale(2, DEFAULT_ROUNDING_MODE));
    }
}
