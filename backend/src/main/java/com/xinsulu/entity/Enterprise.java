package com.xinsulu.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 企业信息实体类
 *
 * @author xinsulu-team
 */
@Data
@Entity
@Table(name = "enterprise")
public class Enterprise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String enterpriseName;

    @Column(unique = true, length = 50)
    private String enterpriseCode;

    @Column(length = 100)
    private String industry;

    @Column(length = 100)
    private String legalPerson;

    @Column(precision = 18, scale = 2)
    private BigDecimal registeredCapital;

    @Column(name = "establish_date")
    private LocalDate establishDate;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String contactPerson;

    @Column(length = 20)
    private String contactPhone;

    @Column(length = 20)
    private String riskLevel;

    @Column(name = "health_score")
    private Integer healthScore;

    @Column(name = "last_report_date")
    private LocalDate lastReportDate;

    @Column(name = "manager_name", length = 100)
    private String managerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @Column(nullable = false)
    private Integer deleted;
}
