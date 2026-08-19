package com.xinsulu.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/** 对旧版 H2 数据库执行幂等的兼容升级。 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class DatabaseSchemaCompatibilityInitializer implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (!"H2".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())) {
                return;
            }
        }

        jdbcTemplate.execute("ALTER TABLE ocr_task ALTER COLUMN average_confidence DECIMAL(7,4)");
        jdbcTemplate.execute("ALTER TABLE ocr_field_result ALTER COLUMN confidence_score DECIMAL(7,4) NOT NULL");
        jdbcTemplate.execute("ALTER TABLE ocr_task ADD COLUMN IF NOT EXISTS source_enterprise_name VARCHAR(200)");
        jdbcTemplate.execute("ALTER TABLE ocr_task ADD COLUMN IF NOT EXISTS source_report_period VARCHAR(20)");
        jdbcTemplate.execute("ALTER TABLE ocr_task ADD COLUMN IF NOT EXISTS source_report_date DATE");
        jdbcTemplate.execute("ALTER TABLE ocr_task ADD COLUMN IF NOT EXISTS source_unit VARCHAR(20)");
        jdbcTemplate.execute("ALTER TABLE enterprise ADD COLUMN IF NOT EXISTS manager_name VARCHAR(100)");
        jdbcTemplate.execute("ALTER TABLE financial_report_archive ADD COLUMN IF NOT EXISTS manager_name VARCHAR(100)");
        jdbcTemplate.execute("ALTER TABLE financial_analysis_report ADD COLUMN IF NOT EXISTS created_by VARCHAR(50)");
        jdbcTemplate.execute("ALTER TABLE financial_analysis_report ADD COLUMN IF NOT EXISTS submitted_by VARCHAR(50)");
        jdbcTemplate.execute("ALTER TABLE financial_analysis_report ADD COLUMN IF NOT EXISTS submitted_time TIMESTAMP");
        jdbcTemplate.execute("ALTER TABLE financial_analysis_report ADD COLUMN IF NOT EXISTS approved_by VARCHAR(50)");
        jdbcTemplate.execute("ALTER TABLE financial_analysis_report ADD COLUMN IF NOT EXISTS approved_time TIMESTAMP");
        log.info("H2 兼容字段升级完成");
    }
}
