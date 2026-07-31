package com.assessment.assessment.domain;

import java.util.UUID;

/**
 * Доменная модель темы оценки.
 *
 * <p>Неизменяемая, не содержит JPA/Spring-зависимостей. Несёт иерархию
 * тема → раздел → компетенция для фильтрации тем по компетенции сотрудника.
 */
public final class TopicInfo {

    private final UUID id;
    private final String name;
    private final String sectionName;
    private final UUID competencyId;
    private final String competencyName;

    private TopicInfo(UUID id, String name, String sectionName, UUID competencyId, String competencyName) {
        this.id = id;
        this.name = name;
        this.sectionName = sectionName;
        this.competencyId = competencyId;
        this.competencyName = competencyName;
    }

    public static TopicInfo of(UUID id, String name, String sectionName, UUID competencyId, String competencyName) {
        return new TopicInfo(id, name, sectionName, competencyId, competencyName);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSectionName() {
        return sectionName;
    }

    public UUID getCompetencyId() {
        return competencyId;
    }

    public String getCompetencyName() {
        return competencyName;
    }
}
