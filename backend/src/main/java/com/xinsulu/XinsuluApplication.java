package com.xinsulu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 鑫速录-企业财务报表自动化填报与智能分析系统
 * 应用程序主入口类
 *
 * @author xinsulu-team
 * @version 1.0.0
 * @since 2026-07-16
 */
@SpringBootApplication
public class XinsuluApplication {

    /**
     * 应用程序主方法，启动Spring Boot应用
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(XinsuluApplication.class, args);
        System.out.println("========================================");
        System.out.println("  鑫速录系统启动成功!");
        System.out.println("  访问地址: http://localhost:8080");
        System.out.println("  Swagger文档: http://localhost:8080/swagger-ui/");
        System.out.println("  H2控制台: http://localhost:8080/h2-console");
        System.out.println("========================================");
    }
}
