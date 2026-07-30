package com.assessment.dto.mapper;

import com.assessment.dto.CreateEmployeeRequestDto;
import com.assessment.dto.EmployeeDto;
import com.assessment.dto.UpdateEmployeeRequestDto;
import com.assessment.entity.Employee;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct-маппер для {@link Employee} ↔ DTO.
 *
 * <p>{@link Employee#getCompetency()} — это JPA-связь с компетенцией.
 * В DTO {@link EmployeeDto} она попадает как {@link com.assessment.dto.CompetencyRefDto}
 * (только {@code id} и {@code name}) — MapStruct сам подберёт
 * {@link CompetencyMapper#toCompetencyRefDto} через {@code uses = CompetencyMapper.class}.
 *
 * <p>Связывание сотрудника с компетенцией по {@code competencyId} из запроса
 * НЕ выполняется в маппере — это остаётся ответственностью контроллера,
 * который через {@link com.assessment.repository.CompetencyRepository} загружает
 * настоящую сущность компетенции (а не суррогат без данных).
 */
@Mapper(
        componentModel = "spring",
        uses = {MapperSupport.class, CompetencyMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EmployeeMapper {

    /**
     * Преобразует сущность {@link Employee} в {@link EmployeeDto}.
     * JPA-связь {@code competency} нужна как {@link com.assessment.dto.CompetencyRefDto};
     * список {@code sessions} в DTO не входит.
     *
     * @param entity сущность сотрудника (может быть {@code null})
     * @return DTO сотрудника или {@code null}
     */
    @Mapping(target = "competency", source = "competency")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    EmployeeDto toDto(Employee entity);

    /**
     * Создаёт новую сущность {@link Employee} из DTO-запроса на создание.
     * Поля {@code competency} и {@code competencyId} намеренно игнорируются:
     * контроллер отдельно загружает {@link com.assessment.entity.Competency}
     * через репозиторий и привязывает её к сотруднику.
     *
     * @param dto данные сотрудника (ФИО, должность, отдел)
     * @return новая сущность сотрудника или {@code null}
     */
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "competency",  ignore = true)
    @Mapping(target = "sessions",    ignore = true)
    @Mapping(target = "createdAt",   ignore = true)
    @Mapping(target = "updatedAt",   ignore = true)
    Employee toEntity(CreateEmployeeRequestDto dto);

    /**
     * Изменяет существующую сущность {@link Employee} полями из DTO.
     * {@code competency} и {@code competencyId} игнорируются по той же причине,
     * что и при создании: контроллер отдельно управляет связью с компетенцией.
     *
     * @param entity изменяемая сущность сотрудника (не {@code null})
     * @param dto    новые значения (может быть {@code null})
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "competency",  ignore = true)
    @Mapping(target = "sessions",    ignore = true)
    @Mapping(target = "createdAt",   ignore = true)
    @Mapping(target = "updatedAt",   ignore = true)
    void updateEntity(@MappingTarget Employee entity, UpdateEmployeeRequestDto dto);
}