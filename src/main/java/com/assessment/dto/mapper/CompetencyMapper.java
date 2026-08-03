package com.assessment.dto.mapper;

import com.assessment.dto.CompetencyDto;
import com.assessment.dto.CompetencyRefDto;
import com.assessment.dto.CreateCompetencyRequestDto;
import com.assessment.dto.UpdateCompetencyRequestDto;
import com.assessment.entity.Competency;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct-маппер для {@link Competency} ↔ DTO.
 *
 * <p>{@code uses = {MapperSupport.class}} позволяет MapStruct делегировать конвертацию
 * {@link java.time.Instant} → {@link java.time.OffsetDateTime} общему helper-у.
 * {@link SectionMapper} и {@link QuestionBankMapper} подключаются через {@code uses},
 * чтобы маппинг вложенных коллекций {@code sections} и {@code questionBanks}
 * происходил автоматически — MapStruct подберёт их по типам параметров.
 */
@Mapper(
        componentModel = "spring",
        uses = {MapperSupport.class, SectionMapper.class, QuestionBankMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CompetencyMapper {

    /**
     * Преобразует сущность {@link Competency} в полный {@link CompetencyDto}.
     *
     * <p>Вложенные коллекции {@code sections} и {@code questionBanks} маппятся
     * на соответствующие DTO-списки автоматически через {@code uses}.
     *
     * @param entity сущность компетенции (может быть {@code null})
     * @return DTO компетенции или {@code null}, если на входе {@code null}
     */
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    CompetencyDto toDto(Competency entity);

    /**
     * Преобразует {@link Competency} в облегчённый {@link CompetencyRefDto}
     * (только {@code id} и {@code name}). Используется при маппинге сотрудника,
     * чтобы показывать только ссылку на компетенцию, а не весь граф связанных сущностей.
     *
     * @param entity сущность компетенции (может быть {@code null})
     * @return DTO-ссылка на компетенцию или {@code null}
     */
    @Named("toCompetencyRefDto")
    CompetencyRefDto toCompetencyRefDto(Competency entity);

    /**
     * Создаёт новую сущность {@link Competency} из DTO-запроса на создание.
     * Поля {@code id}, {@code sections}, {@code questionBanks},
     * {@code createdAt} и {@code updatedAt} не устанавливаются —
     * БД/Hibernate присвоит их автоматически при сохранении.
     *
     * @param dto данные для создания компетенции (может быть {@code null})
     * @return новая сущность компетенции или {@code null}
     */
    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "sections",      ignore = true)
    @Mapping(target = "questionBanks", ignore = true)
    @Mapping(target = "createdAt",     ignore = true)
    @Mapping(target = "updatedAt",     ignore = true)
    Competency toEntity(CreateCompetencyRequestDto dto);

    /**
     * Изменяет существующую сущность {@link Competency} полями из DTO-запроса на обновление.
     * Поля со значением {@code null} в DTO не затирают значения сущности
     * (стратегия {@link NullValuePropertyMappingStrategy#IGNORE} на уровне {@link Mapper}).
     *
     * @param entity изменяемая сущность (не {@code null})
     * @param dto    новые значения (может быть {@code null} — тогда изменений нет)
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "sections",      ignore = true)
    @Mapping(target = "questionBanks", ignore = true)
    @Mapping(target = "createdAt",     ignore = true)
    @Mapping(target = "updatedAt",     ignore = true)
    void updateEntity(@MappingTarget Competency entity, UpdateCompetencyRequestDto dto);
}