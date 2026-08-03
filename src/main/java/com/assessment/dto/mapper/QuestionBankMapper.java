package com.assessment.dto.mapper;

import com.assessment.dto.QuestionBankItemDto;
import com.assessment.entity.QuestionBank;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * MapStruct-маппер для {@link QuestionBank} → {@link QuestionBankItemDto}.
 *
 * <p>Идентификаторы {@code competencyId} и {@code topicId} берутся из JPA-связей
 * {@link QuestionBank#getCompetency()} и {@link QuestionBank#getTopic()} —
 * это вложенные источники (nested-source mapping).
 *
 * <p>Поле {@code difficulty} в сущности хранится как {@link String},
 * а в DTO — как {@link QuestionBankItemDto.DifficultyEnum};
 * конвертация выполняется методом {@link #stringToDifficulty(String)}.
 */
@Mapper(
        componentModel = "spring",
        uses = {MapperSupport.class})
public interface QuestionBankMapper {

    /**
     * Преобразует сущность {@link QuestionBank} в {@link QuestionBankItemDto}.
     *
     * @param entity сущность вопроса (может быть {@code null})
     * @return DTO записи банка вопросов или {@code null}
     */
    @Mapping(target = "competencyId", source = "competency.id")
    @Mapping(target = "topicId",      source = "topic.id")
    @Mapping(target = "difficulty",   source = "difficulty", qualifiedByName = "stringToDifficulty")
    @Mapping(target = "createdAt",    source = "createdAt")
    @Mapping(target = "updatedAt",    source = "updatedAt")
    QuestionBankItemDto toDto(QuestionBank entity);

    /**
     * Конвертирует строковое значение сложности в значение enum DTO.
     *
     * @param difficulty строковая сложность (ALL, JUNIOR, MIDDLE, SENIOR) или {@code null}
     * @return соответствующее перечисление {@link QuestionBankItemDto.DifficultyEnum} или {@code null}
     */
    @Named("stringToDifficulty")
    default QuestionBankItemDto.DifficultyEnum stringToDifficulty(String difficulty) {
        return difficulty == null ? null : QuestionBankItemDto.DifficultyEnum.fromValue(difficulty);
    }
}