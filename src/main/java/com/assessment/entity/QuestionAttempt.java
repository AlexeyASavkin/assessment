package com.assessment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(name = "question_text", nullable = false)
    private String questionText;

    @Column(name = "raw_transcript")
    private String rawTranscript;

    @Column(name = "final_transcript")
    private String finalTranscript;

    @Column(precision = 3, scale = 2)
    private BigDecimal score;

    private String confidence;

    @Column(name = "valid_judge")
    @Builder.Default
    private Boolean validJudge = true;

    private String feedback;

    @Column(name = "followup_depth")
    @Builder.Default
    private Integer followupDepth = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "followup_parent_id")
    private QuestionAttempt followupParent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criteria_id")
    private Criteria criteria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
