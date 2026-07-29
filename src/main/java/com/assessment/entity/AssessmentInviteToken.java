package com.assessment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Токен приглашения для сотрудника.
 * Одноразовая ссылка, по которой сотрудник может пройти оценку компетенций.
 * Токен подписан HMAC и имеет срок действия.
 */
@Entity
@Table(name = "assessment_invite_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentInviteToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Хеш токена приглашения, используемый для проверки подлинности ссылки. */
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    /** Сотрудник, для которого создано приглашение. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** Сессия оценки, созданная при открытии пригласительной ссылки. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;

    /** Флаг использования токена. true — ссылка уже была открыта. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean used = false;

    /** Дата и время открытия пригласительной ссылки. */
    @Column(name = "used_at")
    private Instant usedAt;

    /** Дата и время истечения срока действия токена. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
