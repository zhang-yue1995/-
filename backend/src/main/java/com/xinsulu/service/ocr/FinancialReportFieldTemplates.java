package com.xinsulu.service.ocr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 三张企业会计准则报表的标准字段清单。
 *
 * <p>所有识别结果都必须经过该模板补全，保证复核页始终展示
 * 资产负债表 65 项、利润表 37 项、现金流量表 38 项。</p>
 */
public final class FinancialReportFieldTemplates {

    public static final String BALANCE_SHEET = "BALANCE_SHEET";
    public static final String INCOME_STATEMENT = "INCOME_STATEMENT";
    public static final String CASH_FLOW_STATEMENT = "CASH_FLOW_STATEMENT";

    private static final List<FieldDefinition> DEFINITIONS;

    static {
        List<FieldDefinition> fields = new ArrayList<>();
        add(fields, "BS", BALANCE_SHEET, 1, new String[]{
                "货币资金", "以公允价值计量且其变动计入当期损益的金融资产", "衍生金融资产", "应收票据", "应收账款",
                "预付款项", "其他应收款", "存货", "持有待售资产", "一年内到期的非流动资产", "其他流动资产", "流动资产合计",
                "可供出售金融资产", "持有至到期投资", "长期应收款", "长期股权投资", "投资性房地产", "固定资产", "在建工程",
                "生产性生物资产", "油气资产", "无形资产", "开发支出", "商誉", "长期待摊费用", "递延所得税资产",
                "其他非流动资产", "非流动资产合计", "资产总计", "短期借款", "以公允价值计量且其变动计入当期损益的金融负债",
                "衍生金融负债", "应付票据", "应付账款", "预收款项", "应付职工薪酬", "应交税费", "其他应付款", "持有待售负债",
                "一年内到期的非流动负债", "其他流动负债", "流动负债合计", "长期借款", "应付债券", "其中：优先股", "永续债",
                "长期应付款", "预计负债", "递延收益", "递延所得税负债", "其他非流动负债", "非流动负债合计", "负债合计",
                "实收资本（或股本）", "其他权益工具", "其中：优先股", "永续债", "资本公积", "减：库存股", "其他综合收益",
                "专项储备", "盈余公积", "未分配利润", "所有者权益（或股东权益）合计", "负债和所有者权益（或股东权益）合计"
        });
        add(fields, "IS", INCOME_STATEMENT, 2, new String[]{
                "营业收入", "营业成本", "税金及附加", "销售费用", "管理费用", "研发费用", "财务费用", "其中：利息费用", "利息收入",
                "其他收益", "投资收益", "其中：对联营企业和合营企业的投资收益", "公允价值变动收益", "资产减值损失", "资产处置收益",
                "营业利润", "营业外收入", "营业外支出", "利润总额", "所得税费用", "净利润", "持续经营净利润", "终止经营净利润",
                "其他综合收益的税后净额", "以后不能重分类进损益的其他综合收益", "重新计量设定收益计划净负债或净资产的变动",
                "权益法下在被投资单位不能重分类进损益的其他综合收益中享有的份额", "以后将重分类进损益的其他综合收益",
                "权益法下在被投资单位以后将重分类进损益的其他综合收益中享有的份额", "可供出售金融资产公允价值变动损益",
                "持有至到期投资重分类为可供出售金融资产损益", "现金流量套期损益的有效部分", "外币财务报表折算差额",
                "综合收益总额", "每股收益", "基本每股收益", "稀释每股收益"
        });
        add(fields, "CF", CASH_FLOW_STATEMENT, 3, new String[]{
                "经营活动产生的现金流量", "销售产成品、商品、提供劳务收到的现金", "收到的税费返还", "收到的其他与经营活动有关的现金",
                "经营活动现金流入小计", "购买商品、接受劳务支付的现金", "支付给职工以及为职工支付的现金", "支付的各项税费",
                "支付的其他与经营活动有关的现金", "经营活动现金流出小计", "经营活动产生的现金流量净额", "投资活动产生的现金流量",
                "收回投资收到的现金", "取得投资收益收到的现金", "处置固定资产、无形资产和其他长期资产收回的现金净额",
                "处置子公司及其他营业单位收到的现金净额", "收到其他与投资活动有关的现金", "投资活动现金流入小计",
                "购建固定资产、无形资产和其他长期资产所支付的现金", "投资支付的现金", "取得子公司及其他营业单位支付的现金净额",
                "支付其他与投资活动有关的现金", "投资活动现金流出小计", "投资活动产生的现金流量净额", "筹资活动产生的现金流量",
                "吸收投资收到的现金", "取得借款收到的现金", "收到其他与筹资活动有关的现金", "筹资活动现金流入小计",
                "偿还债务支付的现金", "分配股利、利润或偿付利息支付的现金", "支付其他与筹资活动有关的现金",
                "筹资活动现金流出小计", "筹资活动产生的现金流量净额", "汇率变动对现金及现金等价物的影响",
                "现金及现金等价物净增加额", "期初现金及现金等价物余额", "期末现金及现金等价物余额"
        });
        DEFINITIONS = Collections.unmodifiableList(fields);
    }

    private FinancialReportFieldTemplates() {
    }

    public static List<FieldDefinition> all() {
        return DEFINITIONS;
    }

    public static List<FieldDefinition> forType(String type) {
        List<FieldDefinition> result = new ArrayList<>();
        for (FieldDefinition definition : DEFINITIONS) {
            if (definition.getFieldType().equals(type)) {
                result.add(definition);
            }
        }
        return result;
    }

    private static void add(List<FieldDefinition> target, String prefix, String type, int page, String[] names) {
        for (int i = 0; i < names.length; i++) {
            int row = i + 1;
            target.add(new FieldDefinition(
                    String.format("%s.%03d", prefix, row), type, page, row, names[i], aliases(type, row, names[i])));
        }
    }

    private static List<String> aliases(String type, int row, String name) {
        List<String> aliases = new ArrayList<>();
        aliases.add(name);
        if (BALANCE_SHEET.equals(type)) {
            if (row == 2) aliases.addAll(Arrays.asList("短期投资", "交易性金融资产"));
            if (row == 5) aliases.add("应收帐款");
            if (row == 6) aliases.addAll(Arrays.asList("预付帐款", "预付账款"));
            if (row == 10) aliases.add("一年内到期的长期债券投资");
            if (row == 11) aliases.add("待摊费用");
            if (row == 18) aliases.addAll(Arrays.asList("固定资产净额", "固定资产净值"));
            if (row == 26) aliases.add("递延税款借项");
            if (row == 27) aliases.add("其他长期资产");
            if (row == 28) aliases.addAll(Arrays.asList("非流动资产总计", "长期资产合计"));
            if (row == 34) aliases.add("应付帐款");
            if (row == 35) aliases.addAll(Arrays.asList("预收帐款", "预收账款"));
            if (row == 36) aliases.addAll(Arrays.asList("应付工资", "应付福利费"));
            if (row == 37) aliases.add("应交税金");
            if (row == 40) aliases.add("一年内到期的长期负债");
            if (row == 50) aliases.add("递延税款贷项");
            if (row == 51) aliases.add("其他长期负债");
            if (row == 52) aliases.add("长期负债合计");
            if (row == 54) aliases.addAll(Arrays.asList("实收资本", "实收资本或股本", "实收资本（或股本）净额"));
            if (row == 64) aliases.addAll(Arrays.asList("所有者权益合计", "股东权益合计"));
            if (row == 65) aliases.addAll(Arrays.asList("负债和所有者权益合计", "负债及所有者权益合计"));
        } else if (INCOME_STATEMENT.equals(type)) {
            if (row == 1) aliases.add("主营业务收入");
            if (row == 2) aliases.add("主营业务成本");
            if (row == 3) aliases.addAll(Arrays.asList("主营业务税金及附加", "主营业务税金"));
            if (row == 4) aliases.add("营业费用");
            if (row == 10) aliases.add("补贴收入");
            if (row == 20) aliases.add("所得税");
        } else if (CASH_FLOW_STATEMENT.equals(type)) {
            if (row == 2) aliases.add("销售商品收到的现金");
            if (row == 6) aliases.add("购买商品支付的现金");
            if (row == 7) aliases.add("支付给职工的现金");
            if (row == 19) aliases.add("购建固定资产支付的现金");
        }
        return Collections.unmodifiableList(aliases);
    }

    public static final class FieldDefinition {
        private final String code;
        private final String fieldType;
        private final int pageNumber;
        private final int rowNumber;
        private final String name;
        private final List<String> aliases;

        private FieldDefinition(String code, String fieldType, int pageNumber, int rowNumber,
                                String name, List<String> aliases) {
            this.code = code;
            this.fieldType = fieldType;
            this.pageNumber = pageNumber;
            this.rowNumber = rowNumber;
            this.name = name;
            this.aliases = aliases;
        }

        public String getCode() { return code; }
        public String getFieldType() { return fieldType; }
        public int getPageNumber() { return pageNumber; }
        public int getRowNumber() { return rowNumber; }
        public String getName() { return name; }
        public List<String> getAliases() { return aliases; }
    }
}
