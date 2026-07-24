package com.assessment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Сессия оценки компетенций.
 * Один сотрудник может иметь несколько сессий, каждая из которых содержит
 * последовательность вопросов и попыток ответа.
 */
@Entity
@Table(name = "sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Сотрудник, проходящий оценку в рамках этой сессии. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** Текущий статус сессии (например, ACTIVE или COMPLETED). */
    @Column(nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    /** Идентификатор текущего вопроса в сессии. */
    @Column(name = "current_question_id")
    private UUID currentQuestionId;

    /** Список попыток ответа на вопросы в рамках сессии. */
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QuestionAttempt> attempts = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
