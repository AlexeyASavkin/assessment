package com.assessment.management.port.out;

import com.assessment.entity.Section;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Выходной порт доступа к сущности {@link Section} в management-контексте.
 *
 * <p>Реализуется JPA-адаптером поверх Spring Data репозитория.
 * Сущность используется как есть (pure-CRUD, без отдельной доменной модели).
 */
public interface SectionRepositoryPort {

    /**
     * Сохраняет (создаёт или обновляет) раздел.
     *
     * @param section раздел
     * @return сохранённый раздел
     */
    Section save(Section section);

    /**
     * Возвращает раздел по идентификатору.
     *
     * @param id идентификатор раздела
     * @return раздел или пусто, если не найден
     */
    Optional<Section> findById(UUID id);

    /**
     * Возвращает разделы указанной компетенции.
     *
     * @param competencyId идентификатор компетенции
     * @return список разделов
     */
    List<Section> findByCompetencyId(UUID competencyId);

    /**
     * Удаляет раздел по идентификатору.
     *
     * @param id идентификатор раздела
     */
    void deleteById(UUID id);
}
