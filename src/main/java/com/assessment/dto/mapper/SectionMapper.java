package com.assessment.dto.mapper;

import com.assessment.dto.CreateSectionRequestDto;
import com.assessment.dto.SectionDto;
import com.assessment.dto.UpdateSectionRequestDto;
import com.assessment.entity.Competency;
import com.assessment.entity.Section;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.UUID;

/**
 * MapStruct-маппер для {@link Section} ↔ DTO.
 *
 * <p>{@link Section#competency} (JPA-связь с {@link Competency}) не входит в DTO —
 * на уровне API интересует только идентификатор компетенции через путь URL.
 * Аналогично игнорируется вложенный список {@code topics}, чтобы не тащить граф связей
 * в ответ на запрос раздела.
 *
 * <p>{@link #toEntity(UUID, CreateSectionRequestDto)} строит новую сущность раздела,
 * привязывая её к компетенции по {@code competencyId} (path-параметр запроса).
 * Конвертация UUID → {@link Competency} идёт через {@link #uuidToCompetency(UUID)}.
 */
@Mapper(
        componentModel = "spring",
        uses = {MapperSupport.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SectionMapper {

    /**
     * Преобразует сущность {@link Section} в {@link SectionDto}.
     * Игнорирует JPA-связи с компетенцией и списком тем — их нет в DTO.
     *
     * @param entity сущность раздела (может быть {@code null})
     * @return DTO раздела или {@code null}
     */
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "description", source = "description")
    SectionDto toDto(Section entity);

    /**
     * Создаёт новую сущность {@link Section} из DTO-запроса и идентификатора компетенции.
     * Привязка к компетенции выполняется через {@link #uuidToCompetency(UUID)},
     * что эквивалентно {@code Competency.builder().id(competencyId).build()}.
     *
     * @param competencyId идентификатор родительской компетенции (из URL)
     * @param dto          тело запроса с названием и порядковым номером (может быть {@code null})
     * @return новая сущность раздела или {@code null}
     */
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "name",        source = "dto.name")
    @Mapping(target = "competency",  source = "competencyId", qualifiedByName = "uuidToCompetency")
    @Mapping(target = "sortOrder",   source = "dto.sortOrder")
    @Mapping(target = "topics",      ignore = true)
    @Mapping(target = "createdAt",   ignore = true)
    @Mapping(target = "updatedAt",   ignore = true)
    Section toEntity(UUID competencyId, CreateSectionRequestDto dto);

    /**
     * Изменяет существующую сущность {@link Section} полями из DTO.
     * {@code null}-значения в DTO не затирают соответствующие поля сущности
     * (стратегия {@link NullValuePropertyMappingStrategy#IGNORE} на уровне {@link Mapper}).
     *
     * @param entity изменяемая сущность раздела (не {@code null})
     * @param dto    новые значения (может быть {@code null})
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "competency",  ignore = true)
    @Mapping(target = "topics",      ignore = true)
    @Mapping(target = "createdAt",   ignore = true)
    @Mapping(target = "updatedAt",   ignore = true)
    void updateEntity(@MappingTarget Section entity, UpdateSectionRequestDto dto);

    /**
     * Создаёт заглушку сущности {@link Competency} по идентификатору.
     * Используется для привязки раздела к компетенции без загрузки полной сущности.
     *
     * @param id идентификатор компетенции (может быть {@code null})
     * @return суррогат {@link Competency} с заполненным {@code id} или {@code null}
     */
    @Named("uuidToCompetency")
    static Competency uuidToCompetency(UUID id) {
        return id == null ? null : Competency.builder().id(id).build();
    }
}