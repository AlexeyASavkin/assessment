package com.assessment.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Критерий оценки внутри компетенции.
 * Определяет конкретный навык или знание, которое проверяется у сотрудника.
 */
@Entity
@Table(name = "criteria")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Criteria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Компетенция, к которой относится критерий. */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competency_id", nullable = false)
    private Competency competency;

    /** Название критерия. */
    @Column(nullable = false)
    private String name;

    /** Описание критерия. */
    private String description;

    /** Вес критерия при расчёте итоговой оценки. */
    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal weight = BigDecimal.ONE;

    /** Список уровней требований для данного критерия (JUNIOR, MIDDLE, SENIOR). */
    @OneToMany(mappedBy = "criteria", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CriteriaLevel> levels = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
