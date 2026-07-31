package com.assessment.management.application;

import com.assessment.entity.Employee;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Входной порт (use case) CRUD-операций над сотрудниками.
 *
 * <p>Работает с JPA-сущностью {@link Employee} как с доменной моделью
 * (pure-CRUD — сущность не имеет бизнес-инвариантов). Маппинг DTO ↔ сущность
 * выполняется на уровне HTTP-адаптера. Привязка к компетенции выполняется
 * внутри use case через порт доступа к компетенциям.
 */
public interface EmployeeCrudUseCase {

    /**
     * Создаёт сотрудника. При указании {@code competencyId} загружает компетенцию
     * и привязывает её к сотруднику.
     *
     * @param employee     новый сотрудник (поля уже заполнены маппером)
     * @param competencyId идентификатор компетенции или {@code null}
     * @return созданный сотрудник
     * @throws java.util.NoSuchElementException если компетенция не найдена
     */
    Employee createEmployee(Employee employee, UUID competencyId);

    /**
     * Возвращает всех сотрудников, отсортированных по дате создания (новые первыми).
     *
     * @return список сотрудников
     */
    List<Employee> listEmployees();

    /**
     * Возвращает сотрудника по идентификатору.
     *
     * @param id идентификатор сотрудника
     * @return сотрудник или пусто, если не найден
     */
    Optional<Employee> getEmployee(UUID id);

    /**
     * Обновляет сотрудника: применяет к найденной сущности переданный мутатор,
     * переустанавливает связь с компетенцией и сохраняет.
     *
     * @param id           идентификатор обновляемого сотрудника
     * @param mutator      функция, изменяющая поля сущности (задаётся адаптером через маппер DTO)
     * @param competencyId идентификатор новой компетенции или {@code null} для снятия связи
     * @return обновлённый сотрудник или пусто, если не найден
     * @throws java.util.NoSuchElementException если компетенция не найдена
     */
    Optional<Employee> updateEmployee(UUID id, UnaryOperator<Employee> mutator, UUID competencyId);

    /**
     * Удаляет сотрудника вместе с его пригласительными токенами и сессиями.
     *
     * @param id идентификатор сотрудника
     * @return {@code true}, если сотрудник удалён; {@code false}, если не найден
     */
    boolean deleteEmployee(UUID id);
}
