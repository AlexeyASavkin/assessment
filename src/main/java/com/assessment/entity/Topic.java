package com.assessment.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Тема внутри секции компетенции.
 * Вопросы для оценки генерируются по темам с учётом их веса.
 */
@Entity
@Table(name = "topics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Секция, к которой относится тема. */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    /** Название темы. */
    @Column(nullable = false)
    private String name;

    /** Описание темы. */
    private String description;

    /** Порядковый номер для сортировки тем внутри секции. */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    /** Вес темы при формировании сессии оценки (влияет на количество вопросов). */
    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal weight = BigDecimal.ONE;

    /** Список вопросов в банке, связанных с темой. */
    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QuestionBank> questionBanks = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
