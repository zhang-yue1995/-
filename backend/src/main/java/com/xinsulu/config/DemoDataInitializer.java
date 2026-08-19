package com.xinsulu.config;

import com.xinsulu.entity.BalanceSheet;
import com.xinsulu.entity.BalanceSheetItem;
import com.xinsulu.entity.CashFlowStatement;
import com.xinsulu.entity.CashFlowStatementItem;
import com.xinsulu.entity.Enterprise;
import com.xinsulu.entity.FinancialHealthScore;
import com.xinsulu.entity.FinancialReportArchive;
import com.xinsulu.entity.IncomeStatement;
import com.xinsulu.entity.IncomeStatementItem;
import com.xinsulu.repository.BalanceSheetItemRepository;
import com.xinsulu.repository.BalanceSheetRepository;
import com.xinsulu.repository.CashFlowStatementItemRepository;
import com.xinsulu.repository.CashFlowStatementRepository;
import com.xinsulu.repository.EnterpriseRepository;
import com.xinsulu.repository.FinancialHealthScoreRepository;
import com.xinsulu.repository.FinancialReportArchiveRepository;
import com.xinsulu.repository.IncomeStatementItemRepository;
import com.xinsulu.repository.IncomeStatementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 首次启动时导入项目随附的演示数据。
 *
 * <p>数据取自项目内的财务报表原件和旧版 data.sql，只有数据库从未创建过企业记录时才执行。
 * 生产环境可通过 DEMO_DATA_ENABLED=false 关闭。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.demo-data", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataInitializer implements ApplicationRunner {

    private final EnterpriseRepository enterpriseRepository;
    private final FinancialReportArchiveRepository archiveRepository;
    private final BalanceSheetRepository balanceSheetRepository;
    private final BalanceSheetItemRepository balanceSheetItemRepository;
    private final IncomeStatementRepository incomeStatementRepository;
    private final IncomeStatementItemRepository incomeStatementItemRepository;
    private final CashFlowStatementRepository cashFlowStatementRepository;
    private final CashFlowStatementItemRepository cashFlowStatementItemRepository;
    private final FinancialHealthScoreRepository healthScoreRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 逻辑删除的企业仍然占用统一社会信用代码唯一索引，也代表数据库已经初始化过。
        // 此处必须统计全部记录，不能只统计 deleted=0；否则用户删除全部企业后重启，
        // 初始化器会重复 INSERT 演示信用代码并导致整个应用启动失败。
        if (enterpriseRepository.count() > 0) {
            log.info("数据库已有企业历史记录，跳过演示数据初始化");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Enterprise real = saveEnterprise(
                "江苏曼斯特机电科技有限公司", "DEMO-202603-MANSTER",
                "制造业/机电设备", "DANGEROUS", 22, LocalDate.of(2026, 3, 31), now);
        Enterprise technology = saveEnterprise(
                "xxx科技有限公司", "91110108MA01XXXXX1",
                "信息技术/软件开发", "ATTENTION", 71, LocalDate.of(2025, 12, 31), now.minusDays(1));
        Enterprise manufacturing = saveEnterprise(
                "华新制造有限公司", "91320100MA02XXXXX2",
                "制造业/机械设备", "NORMAL", 55, LocalDate.of(2025, 12, 31), now.minusDays(2));

        FinancialReportArchive realReport = saveArchive(
                real, "2026-03", 2026, 3, "APPROVED", "PASSED",
                "7923544.71", "7995201.81", "-71657.10",
                "773769.57", "-9950.59", "-391006.53", "98.60", now);
        saveArchive(
                technology, "2025Q4", 2025, null, "APPROVED", "PASSED",
                "12580.50", "7890.30", "4690.20",
                "8920.60", "456.80", "520.30", "96.20", now.minusDays(1));
        saveArchive(
                manufacturing, "2025Q4", 2025, null, "APPROVED", "PASSED",
                "25680.90", "15230.60", "10450.30",
                "18560.70", "1280.50", "1450.80", "97.50", now.minusDays(2));

        seedBalanceSheet(real, realReport, now);
        seedIncomeStatement(real, realReport, now);
        seedCashFlowStatement(real, realReport, now);
        seedHealthScore(real, realReport, now);

        log.info("演示数据初始化完成：enterprises=3, reports=3, referenceReportId={}", realReport.getId());
    }

    private Enterprise saveEnterprise(String name, String code, String industry, String riskLevel,
                                      Integer healthScore, LocalDate lastReportDate, LocalDateTime createdTime) {
        Enterprise enterprise = new Enterprise();
        enterprise.setEnterpriseName(name);
        enterprise.setEnterpriseCode(code);
        enterprise.setIndustry(industry);
        enterprise.setRiskLevel(riskLevel);
        enterprise.setHealthScore(healthScore);
        enterprise.setLastReportDate(lastReportDate);
        enterprise.setCreatedTime(createdTime);
        enterprise.setUpdatedTime(createdTime);
        enterprise.setDeleted(0);
        return enterpriseRepository.save(enterprise);
    }

    private FinancialReportArchive saveArchive(
            Enterprise enterprise, String period, Integer year, Integer month,
            String filingStatus, String validationStatus,
            String assets, String liabilities, String equity,
            String revenue, String netProfit, String operatingCashFlow,
            String dataQualityScore, LocalDateTime createdTime) {
        FinancialReportArchive archive = new FinancialReportArchive();
        archive.setEnterprise(enterprise);
        archive.setReportType("COMPREHENSIVE");
        archive.setReportPeriod(period);
        archive.setReportYear(year);
        archive.setReportMonth(month);
        archive.setReportQuarter(month == null ? 4 : null);
        archive.setFilingStatus(filingStatus);
        archive.setValidationStatus(validationStatus);
        archive.setTotalAssets(decimal(assets));
        archive.setTotalLiabilities(decimal(liabilities));
        archive.setTotalEquity(decimal(equity));
        archive.setRevenue(decimal(revenue));
        archive.setNetProfit(decimal(netProfit));
        archive.setOperatingCashFlow(decimal(operatingCashFlow));
        archive.setDataSource("REFERENCE_PDF");
        archive.setDataQualityScore(decimal(dataQualityScore));
        archive.setRemarks("项目随附报表原件/历史演示数据");
        archive.setCreatedTime(createdTime);
        archive.setUpdatedTime(createdTime);
        archive.setDeleted(0);
        return archiveRepository.save(archive);
    }

    private void seedBalanceSheet(Enterprise enterprise, FinancialReportArchive archive, LocalDateTime now) {
        BalanceSheet sheet = new BalanceSheet();
        sheet.setArchive(archive);
        sheet.setEnterprise(enterprise);
        sheet.setReportPeriod("2026-03");
        sheet.setReportDate(LocalDate.of(2026, 3, 31));
        sheet.setTotalCurrentAssets(decimal("7464000.88"));
        sheet.setTotalNonCurrentAssets(decimal("459543.83"));
        sheet.setTotalAssets(decimal("7923544.71"));
        sheet.setTotalCurrentLiabilities(decimal("7995201.81"));
        sheet.setTotalNonCurrentLiabilities(BigDecimal.ZERO);
        sheet.setTotalLiabilities(decimal("7995201.81"));
        sheet.setTotalEquity(decimal("-71657.10"));
        sheet.setBalanceCheckResult("PASSED");
        sheet.setBalanceDifference(BigDecimal.ZERO);
        sheet.setCreatedTime(now);
        sheet.setUpdatedTime(now);
        sheet.setDeleted(0);
        sheet = balanceSheetRepository.save(sheet);

        String[][] rows = {
                {"1", "货币资金", "流动资产", "1034959.57", "1425966.10", "0"},
                {"2", "以公允价值计量且其变动计入当期损益的金融资产", "流动资产", null, null, "0"},
                {"3", "衍生金融资产", "流动资产", null, null, "0"},
                {"4", "应收票据", "流动资产", "458000.00", "458000.00", "0"},
                {"5", "应收账款", "流动资产", "1516428.68", "1518475.50", "0"},
                {"6", "预付款项", "流动资产", "3147246.53", "2946126.52", "0"},
                {"7", "其他应收款", "流动资产", "1238174.11", "957304.11", "0"},
                {"8", "存货", "流动资产", null, null, "0"},
                {"9", "持有待售资产", "流动资产", null, null, "0"},
                {"10", "一年内到期的非流动资产", "流动资产", null, null, "0"},
                {"11", "其他流动资产", "流动资产", "69191.99", "55890.50", "0"},
                {"12", "流动资产合计", "流动资产", "7464000.88", "7361762.73", "1"},
                {"13", "可供出售金融资产", "非流动资产", null, null, "0"},
                {"14", "持有至到期投资", "非流动资产", null, null, "0"},
                {"15", "长期应收款", "非流动资产", null, null, "0"},
                {"16", "长期股权投资", "非流动资产", null, null, "0"},
                {"17", "投资性房地产", "非流动资产", null, null, "0"},
                {"18", "固定资产", "非流动资产", "459543.83", "459543.83", "0"},
                {"19", "在建工程", "非流动资产", null, null, "0"},
                {"20", "生产性生物资产", "非流动资产", null, null, "0"},
                {"21", "油气资产", "非流动资产", null, null, "0"},
                {"22", "无形资产", "非流动资产", null, null, "0"},
                {"23", "开发支出", "非流动资产", null, null, "0"},
                {"24", "商誉", "非流动资产", null, null, "0"},
                {"25", "长期待摊费用", "非流动资产", null, null, "0"},
                {"26", "递延所得税资产", "非流动资产", null, null, "0"},
                {"27", "其他非流动资产", "非流动资产", null, null, "0"},
                {"28", "非流动资产合计", "非流动资产", "459543.83", "459543.83", "1"},
                {"29", "资产总计", "资产", "7923544.71", "7821306.56", "1"},
                {"30", "短期借款", "流动负债", null, null, "0"},
                {"31", "以公允价值计量且其变动计入当期损益的金融负债", "流动负债", null, null, "0"},
                {"32", "衍生金融负债", "流动负债", null, null, "0"},
                {"33", "应付票据", "流动负债", null, null, "0"},
                {"34", "应付账款", "流动负债", "2238273.24", "2468393.31", "0"},
                {"35", "预收款项", "流动负债", "4398467.97", "4007430.97", "0"},
                {"36", "应付职工薪酬", "流动负债", "14152.91", "15202.91", "0"},
                {"37", "应交税费", "流动负债", "61928.89", "138439.62", "0"},
                {"38", "其他应付款", "流动负债", "1282378.80", "1253546.26", "0"},
                {"39", "持有待售负债", "流动负债", null, null, "0"},
                {"40", "一年内到期的非流动负债", "流动负债", null, null, "0"},
                {"41", "其他流动负债", "流动负债", null, null, "0"},
                {"42", "流动负债合计", "流动负债", "7995201.81", "7883013.07", "1"},
                {"43", "长期借款", "非流动负债", null, null, "0"},
                {"44", "应付债券", "非流动负债", null, null, "0"},
                {"45", "其中：优先股", "非流动负债", null, null, "0"},
                {"46", "永续债", "非流动负债", null, null, "0"},
                {"47", "长期应付款", "非流动负债", null, null, "0"},
                {"48", "预计负债", "非流动负债", null, null, "0"},
                {"49", "递延收益", "非流动负债", null, null, "0"},
                {"50", "递延所得税负债", "非流动负债", null, null, "0"},
                {"51", "其他非流动负债", "非流动负债", null, null, "0"},
                {"52", "非流动负债合计", "非流动负债", null, null, "1"},
                {"53", "负债合计", "负债", "7995201.81", "7883013.07", "1"},
                {"54", "实收资本（或股本）", "所有者权益", null, null, "0"},
                {"55", "其他权益工具", "所有者权益", null, null, "0"},
                {"56", "其中：优先股", "所有者权益", null, null, "0"},
                {"57", "永续债", "所有者权益", null, null, "0"},
                {"58", "资本公积", "所有者权益", null, null, "0"},
                {"59", "减：库存股", "所有者权益", null, null, "0"},
                {"60", "其他综合收益", "所有者权益", null, null, "0"},
                {"61", "专项储备", "所有者权益", null, null, "0"},
                {"62", "盈余公积", "所有者权益", null, null, "0"},
                {"63", "未分配利润", "所有者权益", "-71657.10", "-61706.51", "0"},
                {"64", "所有者权益（或股东权益）合计", "所有者权益", "-71657.10", "-61706.51", "1"},
                {"65", "负债和所有者权益（或股东权益）合计", "负债和所有者权益", "7923544.71", "7821306.56", "1"}
        };

        List<BalanceSheetItem> items = new ArrayList<>();
        for (String[] row : rows) {
            BalanceSheetItem item = new BalanceSheetItem();
            item.setBalanceSheet(sheet);
            item.setItemCode("BS." + row[0]);
            item.setItemName(row[1]);
            item.setItemCategory(row[2]);
            item.setEndingBalance(decimal(row[3]));
            item.setBeginningBalance(decimal(row[4]));
            item.setSortOrder(Integer.valueOf(row[0]));
            item.setIsTotalRow(Integer.valueOf(row[5]));
            item.setCreatedTime(now);
            item.setUpdatedTime(now);
            item.setDeleted(0);
            items.add(item);
        }
        balanceSheetItemRepository.saveAll(items);
    }

    private void seedIncomeStatement(Enterprise enterprise, FinancialReportArchive archive, LocalDateTime now) {
        IncomeStatement statement = new IncomeStatement();
        statement.setArchive(archive);
        statement.setEnterprise(enterprise);
        statement.setReportPeriod("2026-03");
        statement.setStartDate(LocalDate.of(2026, 1, 1));
        statement.setEndDate(LocalDate.of(2026, 3, 31));
        statement.setTotalOperatingIncome(decimal("773769.57"));
        statement.setTotalOperatingCost(decimal("784216.16"));
        statement.setOperatingProfit(decimal("-10446.59"));
        statement.setTotalProfit(decimal("-9950.59"));
        statement.setNetProfit(decimal("-9950.59"));
        statement.setCrosscheckResult("PASSED");
        statement.setCrosscheckDifference(BigDecimal.ZERO);
        statement.setCreatedTime(now);
        statement.setUpdatedTime(now);
        statement.setDeleted(0);
        statement = incomeStatementRepository.save(statement);

        String[][] rows = {
                {"1", "一、营业收入", "收入", "773769.57", "2351280.19", "1"},
                {"2", "减：营业成本", "成本费用", "447446.55", "2115444.00", "0"},
                {"3", "税金及附加", "成本费用", "2364.72", "2146.79", "0"},
                {"4", "销售费用", "成本费用", null, null, "0"},
                {"5", "管理费用", "成本费用", "333386.19", "249162.91", "0"},
                {"6", "研发费用", "成本费用", null, null, "0"},
                {"7", "财务费用", "成本费用", "1018.70", "-184.22", "0"},
                {"8", "其中：利息费用", "成本费用", "1131.35", null, "0"},
                {"9", "利息收入", "成本费用", "-120.65", "-186.02", "0"},
                {"10", "加：其他收益", "其他收益", null, null, "0"},
                {"11", "投资收益（损失以“-”号填列）", "其他收益", null, null, "0"},
                {"12", "其中：对联营企业和合营企业的投资收益", "其他收益", null, null, "0"},
                {"13", "公允价值变动收益（损失以“-”号填列）", "其他收益", null, null, "0"},
                {"14", "资产减值损失（损失以“-”号填列）", "其他收益", null, null, "0"},
                {"15", "资产处置收益（损失以“-”号填列）", "其他收益", null, null, "0"},
                {"16", "二、营业利润（亏损以“-”号填列）", "利润", "-10446.59", "-15289.29", "1"},
                {"17", "加：营业外收入", "营业外", "496.00", null, "0"},
                {"18", "减：营业外支出", "营业外", null, null, "0"},
                {"19", "三、利润总额（亏损总额以“-”号填列）", "利润", "-9950.59", "-15289.29", "1"},
                {"20", "减：所得税费用", "所得税", null, null, "0"},
                {"21", "四、净利润（净亏损以“-”号填列）", "利润", "-9950.59", "-15289.29", "1"},
                {"22", "（一）持续经营净利润（净亏损以“-”号填列）", "利润", null, null, "0"},
                {"23", "（二）终止经营净利润（净亏损以“-”号填列）", "利润", null, null, "0"},
                {"24", "五、其他综合收益的税后净额", "其他综合收益", null, null, "1"},
                {"25", "（一）以后不能重分类进损益的其他综合收益", "其他综合收益", null, null, "0"},
                {"26", "1.重新计量设定受益计划净负债或净资产的变动", "其他综合收益", null, null, "0"},
                {"27", "2.权益法下在被投资单位不能重分类进损益的其他综合收益中享有的份额", "其他综合收益", null, null, "0"},
                {"28", "（二）以后将重分类进损益的其他综合收益", "其他综合收益", null, null, "0"},
                {"29", "1.权益法下在被投资单位以后将重分类进损益的其他综合收益中享有的份额", "其他综合收益", null, null, "0"},
                {"30", "2.可供出售金融资产公允价值变动损益", "其他综合收益", null, null, "0"},
                {"31", "3.持有至到期投资重分类为可供出售金融资产损益", "其他综合收益", null, null, "0"},
                {"32", "4.现金流量套期损益的有效部分", "其他综合收益", null, null, "0"},
                {"33", "5.外币财务报表折算差额", "其他综合收益", null, null, "0"},
                {"34", "六、综合收益总额", "利润", "-9950.59", "-15289.29", "1"},
                {"35", "七、每股收益", "每股收益", null, null, "1"},
                {"36", "（一）基本每股收益", "每股收益", null, null, "0"},
                {"37", "（二）稀释每股收益", "每股收益", null, null, "0"}
        };

        List<IncomeStatementItem> items = new ArrayList<>();
        for (String[] row : rows) {
            IncomeStatementItem item = new IncomeStatementItem();
            item.setIncomeStatement(statement);
            item.setItemCode("IS." + row[0]);
            item.setItemName(row[1]);
            item.setItemCategory(row[2]);
            item.setCurrentPeriodAmount(decimal(row[3]));
            item.setPreviousPeriodAmount(decimal(row[4]));
            item.setSortOrder(Integer.valueOf(row[0]));
            item.setIsTotalRow(Integer.valueOf(row[5]));
            item.setCreatedTime(now);
            item.setUpdatedTime(now);
            item.setDeleted(0);
            items.add(item);
        }
        incomeStatementItemRepository.saveAll(items);
    }

    private void seedCashFlowStatement(Enterprise enterprise, FinancialReportArchive archive, LocalDateTime now) {
        CashFlowStatement statement = new CashFlowStatement();
        statement.setArchive(archive);
        statement.setEnterprise(enterprise);
        statement.setReportPeriod("2026-03");
        statement.setReportDate(LocalDate.of(2026, 3, 31));
        statement.setCashInflowsOperating(decimal("1256682.47"));
        statement.setCashOutflowsOperating(decimal("1647689.00"));
        statement.setNetCashFlowOperating(decimal("-391006.53"));
        statement.setCashInflowsInvesting(BigDecimal.ZERO);
        statement.setCashOutflowsInvesting(BigDecimal.ZERO);
        statement.setNetCashFlowInvesting(BigDecimal.ZERO);
        statement.setCashInflowsFinancing(BigDecimal.ZERO);
        statement.setCashOutflowsFinancing(BigDecimal.ZERO);
        statement.setNetCashFlowFinancing(BigDecimal.ZERO);
        statement.setExchangeRateEffect(BigDecimal.ZERO);
        statement.setNetIncreaseInCash(decimal("-391006.53"));
        statement.setCashAtBeginning(decimal("1425966.10"));
        statement.setCashAtEnd(decimal("1034959.57"));
        statement.setCreatedTime(now);
        statement.setUpdatedTime(now);
        statement.setDeleted(0);
        statement = cashFlowStatementRepository.save(statement);

        String[][] rows = {
                {"1", "一、经营活动产生的现金流量", "OPERATING", null, "1"},
                {"2", "销售产成品、商品、提供劳务收到的现金", "OPERATING", "1256065.82", "0"},
                {"3", "收到的税费返还", "OPERATING", null, "0"},
                {"4", "收到的其他与经营活动有关的现金", "OPERATING", "616.65", "0"},
                {"5", "经营活动现金流入小计", "OPERATING", "1256682.47", "1"},
                {"6", "购买商品、接受劳务支付的现金", "OPERATING", "1113167.81", "0"},
                {"7", "支付给职工以及为职工支付的现金", "OPERATING", "93800.00", "0"},
                {"8", "支付的各项税费", "OPERATING", "120353.18", "0"},
                {"9", "支付的其他与经营活动有关的现金", "OPERATING", "320368.01", "0"},
                {"10", "经营活动现金流出小计", "OPERATING", "1647689.00", "1"},
                {"11", "经营活动产生的现金流量净额", "OPERATING", "-391006.53", "1"},
                {"12", "二、投资活动产生的现金流量", "INVESTING", null, "1"},
                {"13", "收回投资收到的现金", "INVESTING", null, "0"},
                {"14", "取得投资收益收到的现金", "INVESTING", null, "0"},
                {"15", "处置固定资产、无形资产和其他长期资产收回的现金净额", "INVESTING", null, "0"},
                {"16", "处置子公司及其他营业单位收到的现金净额", "INVESTING", null, "0"},
                {"17", "收到其他与投资活动有关的现金", "INVESTING", null, "0"},
                {"18", "投资活动现金流入小计", "INVESTING", null, "1"},
                {"19", "购建固定资产、无形资产和其他长期资产所支付的现金", "INVESTING", null, "0"},
                {"20", "投资支付的现金", "INVESTING", null, "0"},
                {"21", "取得子公司及其他营业单位支付的现金净额", "INVESTING", null, "0"},
                {"22", "支付其他与投资活动有关的现金", "INVESTING", null, "0"},
                {"23", "投资活动现金流出小计", "INVESTING", null, "1"},
                {"24", "投资活动产生的现金流量净额", "INVESTING", null, "1"},
                {"25", "三、筹资活动产生的现金流量", "FINANCING", null, "1"},
                {"26", "吸收投资收到的现金", "FINANCING", null, "0"},
                {"27", "取得借款收到的现金", "FINANCING", null, "0"},
                {"28", "收到其他与筹资活动有关的现金", "FINANCING", null, "0"},
                {"29", "筹资活动现金流入小计", "FINANCING", null, "1"},
                {"30", "偿还债务支付的现金", "FINANCING", null, "0"},
                {"31", "分配股利、利润或偿付利息支付的现金", "FINANCING", null, "0"},
                {"32", "支付其他与筹资活动有关的现金", "FINANCING", null, "0"},
                {"33", "筹资活动现金流出小计", "FINANCING", null, "1"},
                {"34", "筹资活动产生的现金流量净额", "FINANCING", null, "1"},
                {"35", "四、汇率变动对现金及现金等价物的影响", "SUMMARY", null, "1"},
                {"36", "五、现金及现金等价物净增加额", "SUMMARY", "-391006.53", "1"},
                {"37", "加：期初现金及现金等价物余额", "SUMMARY", "1425966.10", "0"},
                {"38", "六、期末现金及现金等价物余额", "SUMMARY", "1034959.57", "1"}
        };

        List<CashFlowStatementItem> items = new ArrayList<>();
        for (String[] row : rows) {
            CashFlowStatementItem item = new CashFlowStatementItem();
            item.setStatement(statement);
            item.setItemCode("CF." + row[0]);
            item.setItemName(row[1]);
            item.setItemType(row[2]);
            item.setRowNumber(Integer.valueOf(row[0]));
            item.setAmount(decimal(row[3]));
            item.setIsTotalRow("1".equals(row[4]));
            item.setConfidenceLevel("HIGH");
            item.setCreatedTime(now);
            item.setUpdatedTime(now);
            item.setDeleted(0);
            items.add(item);
        }
        cashFlowStatementItemRepository.saveAll(items);
    }

    private void seedHealthScore(Enterprise enterprise, FinancialReportArchive report, LocalDateTime now) {
        FinancialHealthScore score = new FinancialHealthScore();
        score.setReport(report);
        score.setEnterprise(enterprise);
        score.setReportDate(LocalDate.of(2026, 3, 31));
        score.setTotalScore(decimal("22"));
        score.setSolvencyScore(decimal("10"));
        score.setProfitabilityScore(decimal("20"));
        score.setCashFlowScore(decimal("10"));
        score.setOperationScore(decimal("55"));
        score.setGrowthScore(decimal("35"));
        score.setRiskLevel("DANGEROUS");
        score.setSolvencyWeight(decimal("0.30"));
        score.setProfitabilityWeight(decimal("0.25"));
        score.setCashFlowWeight(decimal("0.20"));
        score.setOperationWeight(decimal("0.15"));
        score.setGrowthWeight(decimal("0.10"));
        score.setSummary("资产负债率超过100%、经营现金流为负且本期亏损，按模型规则判定为高风险。");
        score.setCreatedTime(now);
        score.setUpdatedTime(now);
        score.setDeleted(0);
        healthScoreRepository.save(score);
    }

    private BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
