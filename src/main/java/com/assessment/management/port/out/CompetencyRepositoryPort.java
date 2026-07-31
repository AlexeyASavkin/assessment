package com.assessment.management.port.out;

import com.assessment.entity.Competency;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Выходной порт доступа к сущности {@link Competency} в management-контексте.
 *
 * <p>Реализуется JPA-адаптером поверх Spring Data репозитория.
 * Сущность используется как есть (pure-CRUD, без отдельной доменной модели).
 */
public interface CompetencyRepositoryPort {

    /**
     * Сохраняет (создаёт или обновляет) компетенцию.
     *
     * @param competency компетенция
     * @return сохранённая компетенция
     */
    Competency save(Competency competency);

    /**
     * Возвращает все компетенции.
     *
     * @return список компетенций
     */
    List<Competency> findAll();

    /**
     * Возвращает компетенцию по идентификатору.
     *
     * @param id идентификатор компетенции
     * @return компетенция или пусто, если не найдена
     */
    Optional<Competency> findById(UUID id);

    /**
     * Удаляет компетенцию по идентификатору.
     *
     * @param id идентификатор компетенции
     */
    void deleteById(UUID id);
}
