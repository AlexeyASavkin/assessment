package com.assessment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Компетенция, оцениваемая у сотрудников (например, "Java", "Коммуникация").
 * Содержит секции, критерии и банк вопросов для проведения оценки.
 */
@Entity
@Table(name = "competencies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Competency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Название компетенции. Должно быть уникальным. */
    @Column(nullable = false, unique = true)
    private String name;

    /** Описание компетенции и области её применения. */
    private String description;

    /** Список секций внутри компетенции. */
    @OneToMany(mappedBy = "competency", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Section> sections = new java.util.ArrayList<>();

    /** Список вопросов в банке вопросов, связанных с компетенцией. */
    @OneToMany(mappedBy = "competency", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QuestionBank> questionBanks = new java.util.ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
