package com.xinsulu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.spi.service.contexts.SecurityContext;

/**
 * Swagger2 API文档配置
 * 生成RESTful API接口文档
 *
 * @author xinsulu-team
 */
@Configuration
public class SwaggerConfig {

    @Value("${swagger.enabled:true}")
    private boolean swaggerEnabled;

    /**
     * 配置Swagger Docket
     *
     * @return Docket实例
     */
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.OAS_30)
                .enable(swaggerEnabled)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.xinsulu.controller"))
                .paths(PathSelectors.regex("/api/.*"))
                .build()
                .securitySchemes(securitySchemes())
                .securityContexts(securityContexts());
    }

    /**
     * API基本信息
     *
     * @return ApiInfo
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("鑫速录-企业财务报表自动化填报与智能分析系统API")
                .description("提供财务报表OCR识别、智能填报、财务指标计算、健康评分、风险预警等功能的RESTful API")
                .version("1.0.0")
                .contact(new Contact("鑫速录技术团队", "https://www.xinsulu.com", "support@xinsulu.com"))
                .license("Apache License Version 2.0")
                .licenseUrl("https://www.apache.org/licenses/LICENSE-2.0")
                .build();
    }

    /**
     * 安全方案（JWT Token认证）
     */
    private java.util.List<SecurityScheme> securitySchemes() {
        return java.util.Collections.singletonList(
                new ApiKey("Authorization", "Authorization", "header")
        );
    }

    /**
     * 安全上下文
     */
    private java.util.List<SecurityContext> securityContexts() {
        return java.util.Collections.singletonList(
                SecurityContext.builder()
                        .securityReferences(defaultAuth())
                        .forPaths(PathSelectors.regex("/api/.*"))
                        .build()
        );
    }

    private java.util.List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        return java.util.Collections.singletonList(
                new SecurityReference("Authorization", new AuthorizationScope[]{authorizationScope})
        );
    }
}
