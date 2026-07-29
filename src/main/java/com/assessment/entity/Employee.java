package com.assessment.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Сотрудник, проходящий оценку компетенций через голосовое интервью.
 * Каждый сотрудник может иметь несколько сессий оценки и привязан к одной компетенции.
 */
@Entity
@Table(name = "employees")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"sessions"})
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** ФИО сотрудника. */
    @Column(name = "full_name", nullable = false)
    private String fullName;

    /** Должность сотрудника. */
    private String position;

    /** Отдел или подразделение, в котором работает сотрудник. */
    private String department;

    /** Компетенция, по которой проводится оценка сотрудника. */
    @ManyToOne
    @JoinColumn(name = "competency_id")
    @JsonIgnoreProperties({"sections", "questionBanks"})
    private Competency competency;

    /** Список сессий оценки, связанных с сотрудником. */
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Session> sessions = new ArrayList<>();

    /**
     * Принимает плоское поле {@code competencyId} из JSON и преобразует его
     * в связанную сущность {@link Competency}. Используется для создания
     * сотрудника с компетенцией через API.
     *
     * @param id идентификатор компетенции
     */
    @JsonProperty("competencyId")
    public void setCompetencyId(UUID id) {
        if (id != null) {
            this.competency = Competency.builder().id(id).build();
        }
    }

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
