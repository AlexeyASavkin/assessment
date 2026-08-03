package com.assessment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Учётная запись администратора системы.
 * Используется для аутентификации в панели управления и доступа к админ API.
 */
@Entity
@Table(name = "admin_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Логин администратора. Должен быть уникальным. */
    @Column(name = "username", nullable = false, unique = true)
    private String username;

    /** Хеш пароля администратора. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** Роль администратора в системе (например, ADMIN). */
    @Column(nullable = false)
    private String role;

    /** Флаг активности учётной записи. Если false, вход запрещён. */
    @Column(nullable = false)
    private Boolean enabled;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
