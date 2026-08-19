package com.xinsulu.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "health_weight_config")
public class HealthWeightConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String dimensionCode;

    @Column(nullable = false, length = 60)
    private String label;

    @Column(nullable = false)
    private Integer weight;

    @Column(length = 20)
    private String color;

    @Column(nullable = false)
    private LocalDateTime updatedTime;
}
