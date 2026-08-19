package com.xinsulu.config;

import com.xinsulu.entity.HealthWeightConfig;
import com.xinsulu.entity.IndicatorRuleConfig;
import com.xinsulu.repository.HealthWeightConfigRepository;
import com.xinsulu.repository.IndicatorRuleConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RuleConfigInitializer implements ApplicationRunner {
    private final IndicatorRuleConfigRepository ruleRepository;
    private final HealthWeightConfigRepository weightRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (ruleRepository.countByDeleted(0) == 0) {
            addRule("debtToAssetRatio", "资产负债率", "总负债 / 总资产 × 100%",
                    "≤60%", "60%-80%", ">80%", "AT_MOST", "60", "80", 15);
            addRule("currentRatio", "流动比率", "流动资产 / 流动负债",
                    "≥1.5", "1.0-1.5", "<1.0", "AT_LEAST", "1.5", "1.0", 10);
            addRule("netProfitMargin", "销售净利率", "净利润 / 营业收入 × 100%",
                    "≥8%", "3%-8%", "<3%", "AT_LEAST", "8", "3", 12);
            addRule("operatingCashToRevenue", "经营现金流比率", "经营活动现金流净额 / 营业收入",
                    "≥10%", "0%-10%", "<0%", "AT_LEAST", "0.1", "0", 13);
            addRule("accountsReceivableTurnover", "应收账款周转率", "营业收入 / 平均应收账款余额",
                    "≥6次", "2-6次", "<2次", "AT_LEAST", "6", "2", 8);
            addRule("inventoryTurnover", "存货周转率", "营业成本 / 平均存货余额",
                    "≥4次", "2-4次", "<2次", "AT_LEAST", "4", "2", 7);
            addRule("grossProfitMargin", "销售毛利率", "(营业收入 - 营业成本) / 营业收入 × 100%",
                    "≥25%", "15%-25%", "<15%", "AT_LEAST", "25", "15", 10);
            addRule("quickRatio", "速动比率", "(流动资产 - 存货) / 流动负债",
                    "≥1.0", "0.8-1.0", "<0.8", "AT_LEAST", "1", "0.8", 8);
            addRule("interestCoverageRatio", "利息保障倍数", "(利润总额 + 利息费用) / 利息费用",
                    "≥3倍", "1.5-3倍", "<1.5倍", "AT_LEAST", "3", "1.5", 11);
        }
        if (weightRepository.count() == 0) {
            addWeight("solvency", "偿债能力", 30, "#e35d6a");
            addWeight("profitability", "盈利能力", 25, "#f3a83b");
            addWeight("cashFlow", "现金流质量", 20, "#3d7cf0");
            addWeight("operation", "运营效率", 15, "#9dd99e");
            addWeight("growth", "成长性", 10, "#6c7d89");
        }
    }

    private void addRule(String code, String name, String formula, String normal, String attention,
                         String highRisk, String direction, String normalValue,
                         String attentionValue, int weight) {
        IndicatorRuleConfig rule = new IndicatorRuleConfig();
        rule.setIndicatorCode(code);
        rule.setIndicatorName(name);
        rule.setFormula(formula);
        rule.setNormalThreshold(normal);
        rule.setAttentionThreshold(attention);
        rule.setHighRiskThreshold(highRisk);
        rule.setThresholdDirection(direction);
        rule.setNormalThresholdValue(new BigDecimal(normalValue));
        rule.setAttentionThresholdValue(new BigDecimal(attentionValue));
        rule.setWeight(weight);
        rule.setApplicableIndustry("全部");
        rule.setIsEnabled(true);
        rule.setCreatedTime(LocalDateTime.now());
        rule.setUpdatedTime(LocalDateTime.now());
        rule.setDeleted(0);
        ruleRepository.save(rule);
    }

    private void addWeight(String code, String label, int weight, String color) {
        HealthWeightConfig config = new HealthWeightConfig();
        config.setDimensionCode(code);
        config.setLabel(label);
        config.setWeight(weight);
        config.setColor(color);
        config.setUpdatedTime(LocalDateTime.now());
        weightRepository.save(config);
    }
}
