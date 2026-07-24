package com.assessment.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Уровень критерия оценки.
 * Описывает требования для конкретного уровня владения критерием
 * (например, "Начинающий", "Продвинутый", "Эксперт").
 */
@Entity
@Table(name = "criteria_levels")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriteriaLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Критерий, к которому относится уровень. */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criteria_id", nullable = false)
    private Criteria criteria;

    /** Название уровня (например, JUNIOR, MIDDLE, SENIOR). */
    @Column(nullable = false)
    private String level;

    /** Описание требований для достижения данного уровня. */
    @Column(nullable = false)
    private String requirements;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
