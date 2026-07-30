package com.assessment.dto;

import com.assessment.entity.*;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class DtoMapper {

    public static CompetencyDto toDto(Competency e) {
        if (e == null) return null;
        return new CompetencyDto()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().atOffset(java.time.ZoneOffset.UTC) : null)
                .sections(e.getSections() != null
                        ? e.getSections().stream().map(DtoMapper::toDto).collect(Collectors.toList())
                        : Collections.emptyList())
                .questionBanks(e.getQuestionBanks() != null
                        ? e.getQuestionBanks().stream().map(DtoMapper::toDto).collect(Collectors.toList())
                        : Collections.emptyList());
    }

    public static CompetencyRefDto toRefDto(Competency e) {
        if (e == null) return null;
        return new CompetencyRefDto()
                .id(e.getId())
                .name(e.getName());
    }

    public static SectionDto toDto(Section e) {
        if (e == null) return null;
        return new SectionDto()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .sortOrder(e.getSortOrder())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().atOffset(java.time.ZoneOffset.UTC) : null);
    }

    public static TopicDto toDto(Topic e) {
        if (e == null) return null;
        return new TopicDto()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .sortOrder(e.getSortOrder())
                .weight(e.getWeight() != null ? e.getWeight().floatValue() : null)
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().atOffset(java.time.ZoneOffset.UTC) : null);
    }

    public static EmployeeDto toDto(Employee e) {
        if (e == null) return null;
        return new EmployeeDto()
                .id(e.getId())
                .fullName(e.getFullName())
                .position(e.getPosition())
                .department(e.getDepartment())
                .competency(toRefDto(e.getCompetency()))
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().atOffset(java.time.ZoneOffset.UTC) : null);
    }

    public static QuestionBankItemDto toDto(QuestionBank e) {
        if (e == null) return null;
        return new QuestionBankItemDto()
                .id(e.getId())
                .competencyId(e.getCompetency() != null ? e.getCompetency().getId() : null)
                .topicId(e.getTopic() != null ? e.getTopic().getId() : null)
                .questionText(e.getQuestionText())
                .difficulty(e.getDifficulty() != null
                        ? QuestionBankItemDto.DifficultyEnum.fromValue(e.getDifficulty()) : null)
                .sortOrder(e.getSortOrder())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().atOffset(java.time.ZoneOffset.UTC) : null);
    }

    public static AssessmentInviteTokenDto toDto(AssessmentInviteToken e) {
        if (e == null) return null;
        return new AssessmentInviteTokenDto()
                .id(e.getId())
                .tokenHash(e.getTokenHash())
                .employee(toDto(e.getEmployee()))
                .session(e.getSession() != null
                        ? new AssessmentInviteTokenSessionDto()
                                .id(e.getSession().getId())
                                .status(e.getSession().getStatus())
                        : null)
                .used(e.getUsed())
                .usedAt(e.getUsedAt() != null ? e.getUsedAt().atOffset(java.time.ZoneOffset.UTC) : null)
                .expiresAt(e.getExpiresAt() != null ? e.getExpiresAt().atOffset(java.time.ZoneOffset.UTC) : null)
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null);
    }

    public static Competency toEntity(CreateCompetencyRequestDto dto) {
        if (dto == null) return null;
        return Competency.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .build();
    }

    public static void updateEntity(Competency entity, UpdateCompetencyRequestDto dto) {
        if (dto == null) return;
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
    }

    public static Section toEntity(UUID competencyId, CreateSectionRequestDto dto) {
        if (dto == null) return null;
        return Section.builder()
                .competency(Competency.builder().id(competencyId).build())
                .name(dto.getName())
                .sortOrder(dto.getSortOrder())
                .build();
    }

    public static void updateEntity(Section entity, UpdateSectionRequestDto dto) {
        if (dto == null) return;
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getSortOrder() != null) entity.setSortOrder(dto.getSortOrder());
    }

    public static Topic toEntity(UUID sectionId, CreateTopicRequestDto dto) {
        if (dto == null) return null;
        return Topic.builder()
                .section(Section.builder().id(sectionId).build())
                .name(dto.getName())
                .weight(dto.getWeight() != null ? java.math.BigDecimal.valueOf(dto.getWeight()) : java.math.BigDecimal.ONE)
                .sortOrder(dto.getSortOrder())
                .build();
    }

    public static void updateEntity(Topic entity, UpdateTopicRequestDto dto) {
        if (dto == null) return;
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getWeight() != null) entity.setWeight(java.math.BigDecimal.valueOf(dto.getWeight()));
        if (dto.getSortOrder() != null) entity.setSortOrder(dto.getSortOrder());
    }

    public static Employee toEntity(CreateEmployeeRequestDto dto) {
        if (dto == null) return null;
        return Employee.builder()
                .fullName(dto.getFullName())
                .position(dto.getPosition())
                .department(dto.getDepartment())
                .build();
    }

    public static void updateEntity(Employee entity, UpdateEmployeeRequestDto dto) {
        if (dto == null) return;
        if (dto.getFullName() != null) entity.setFullName(dto.getFullName());
        if (dto.getPosition() != null) entity.setPosition(dto.getPosition());
        if (dto.getDepartment() != null) entity.setDepartment(dto.getDepartment());
    }
}
