package com.assessment.dto.mapper;

import com.assessment.dto.CreateTopicRequestDto;
import com.assessment.dto.TopicDto;
import com.assessment.dto.UpdateTopicRequestDto;
import com.assessment.entity.Section;
import com.assessment.entity.Topic;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * MapStruct-маппер для {@link Topic} ↔ DTO.
 *
 * <p>{@link Topic#weight} хранится в БД как {@link BigDecimal}, а в DTO — как {@link Float}.
 * MapStruct конвертирует эти типы автоматически.
 *
 * <p>При создании темы вес по умолчанию задаётся через {@code defaultValue = "1"}
 * (что эквивалентно {@link BigDecimal#ONE}), если в DTO вес не указан.
 * Привязка темы к разделу выполняется через {@link #uuidToSection(UUID)}.
 */
@Mapper(
        componentModel = "spring",
        uses = {MapperSupport.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TopicMapper {

    /**
     * Преобразует сущность {@link Topic} в {@link TopicDto}.
     * Игнорирует JPA-связи с разделом и списком вопросов.
     *
     * @param entity сущность темы (может быть {@code null})
     * @return DTO темы или {@code null}
     */
    @Mapping(target = "weight",    source = "weight")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "description", source = "description")
    TopicDto toDto(Topic entity);

    /**
     * Создаёт новую сущность {@link Topic} из DTO-запроса и идентификатора раздела.
     * Привязка к разделу идёт через {@link #uuidToSection(UUID)}.
     *
     * @param sectionId идентификатор родительского раздела (из URL)
     * @param dto        тело запроса с названием, весом и порядком (может быть {@code null})
     * @return новая сущность темы или {@code null}
     */
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "name",        source = "dto.name")
    @Mapping(target = "section",     source = "sectionId", qualifiedByName = "uuidToSection")
    @Mapping(target = "weight",      source = "dto.weight", defaultValue = "1")
    @Mapping(target = "sortOrder",   source = "dto.sortOrder")
    @Mapping(target = "questionBanks", ignore = true)
    @Mapping(target = "createdAt",   ignore = true)
    @Mapping(target = "updatedAt",   ignore = true)
    Topic toEntity(UUID sectionId, CreateTopicRequestDto dto);

    /**
     * Изменяет существующую сущность {@link Topic} полями из DTO.
     * {@code null}-значения в DTO не затирают соответствующие поля сущности.
     *
     * @param entity изменяемая сущность темы (не {@code null})
     * @param dto    новые значения (может быть {@code null})
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "section",     ignore = true)
    @Mapping(target = "questionBanks", ignore = true)
    @Mapping(target = "createdAt",   ignore = true)
    @Mapping(target = "updatedAt",   ignore = true)
    void updateEntity(@MappingTarget Topic entity, UpdateTopicRequestDto dto);

    /**
     * Создаёт заглушку сущности {@link Section} по идентификатору.
     * Используется для привязки темы к разделу без загрузки полной сущности.
     *
     * @param id идентификатор раздела (может быть {@code null})
     * @return суррогат {@link Section} с заполненным {@code id} или {@code null}
     */
    @Named("uuidToSection")
    static Section uuidToSection(UUID id) {
        return id == null ? null : Section.builder().id(id).build();
    }
}