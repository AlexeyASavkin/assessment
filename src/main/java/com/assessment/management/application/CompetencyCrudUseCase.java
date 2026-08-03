package com.assessment.management.application;

import com.assessment.entity.Competency;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Входной порт (use case) CRUD-операций над компетенциями.
 *
 * <p>Работает с JPA-сущностью {@link Competency} как с доменной моделью
 * (pure-CRUD — сущность не имеет бизнес-инвариантов). Маппинг DTO ↔ сущность
 * выполняется на уровне HTTP-адаптера.
 */
public interface CompetencyCrudUseCase {

    /**
     * Создаёт компетенцию.
     *
     * @param competency новая компетенция (без id)
     * @return созданная компетенция
     */
    Competency createCompetency(Competency competency);

    /**
     * Возвращает все компетенции.
     *
     * @return список компетенций
     */
    List<Competency> listCompetencies();

    /**
     * Возвращает компетенцию по идентификатору.
     *
     * @param id идентификатор компетенции
     * @return компетенция или пусто, если не найдена
     */
    Optional<Competency> getCompetency(UUID id);

    /**
     * Обновляет компетенцию: применяет к найденной сущности переданный мутатор
     * и сохраняет её.
     *
     * @param id      идентификатор обновляемой компетенции
     * @param mutator функция, изменяющая поля сущности (задаётся адаптером через маппер DTO)
     * @return обновлённая компетенция или пусто, если не найдена
     */
    Optional<Competency> updateCompetency(UUID id, UnaryOperator<Competency> mutator);

    /**
     * Удаляет компетенцию по идентификатору.
     *
     * @param id идентификатор компетенции
     */
    void deleteCompetency(UUID id);
}
