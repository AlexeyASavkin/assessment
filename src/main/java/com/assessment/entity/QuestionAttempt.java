package com.assessment.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Попытка ответа на вопрос.
 * Содержит текст вопроса, ответ сотрудника и оценку, выставленную LLM.
 */
@Entity
@Table(name = "question_attempts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Сессия оценки, к которой относится данная попытка. */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    /** Текст вопроса, заданного сотруднику. */
    @Column(name = "question_text", nullable = false)
    private String questionText;

    /** Сырой распознанный текст ответа сотрудника до редактирования. */
    @Column(name = "raw_transcript")
    private String rawTranscript;

    /** Итоговый отредактированный текст ответа сотрудника. */
    @Column(name = "final_transcript")
    private String finalTranscript;

    /** Оценка ответа по шкале 0–5, выставленная LLM. */
    @Column(precision = 3, scale = 2)
    private BigDecimal score;

    /** Уровень уверенности LLM в оценке (например, high, medium, low). */
    private String confidence;

    /**
     * Флаг валидности оценки.
     * Если false, оценка не учитывается при расчёте итогового уровня.
     */
    @Column(name = "valid_judge")
    @Builder.Default
    private Boolean validJudge = true;

    /** Текстовая обратная связь от LLM по ответу сотрудника. */
    private String feedback;

    /**
     * Глубина уточняющего вопроса.
     * 0 — основной вопрос, 1 и более — уточняющий вопрос.
     */
    @Column(name = "followup_depth")
    @Builder.Default
    private Integer followupDepth = 0;

    /** Родительская попытка ответа, если текущий вопрос является уточняющим. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "followup_parent_id")
    private QuestionAttempt followupParent;

    /** Тема, по которой сгенерирован вопрос. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
