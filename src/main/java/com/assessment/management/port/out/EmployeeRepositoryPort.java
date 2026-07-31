package com.assessment.management.port.out;

import com.assessment.entity.Employee;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Выходной порт доступа к сущности {@link Employee} в management-контексте.
 *
 * <p>Реализуется JPA-адаптером поверх Spring Data репозитория.
 * Сущность используется как есть (pure-CRUD, без отдельной доменной модели).
 */
public interface EmployeeRepositoryPort {

    /**
     * Сохраняет (создаёт или обновляет) сотрудника.
     *
     * @param employee сотрудник
     * @return сохранённый сотрудник
     */
    Employee save(Employee employee);

    /**
     * Возвращает сотрудника по идентификатору.
     *
     * @param id идентификатор сотрудника
     * @return сотрудник или пусто, если не найден
     */
    Optional<Employee> findById(UUID id);

    /**
     * Возвращает всех сотрудников, отсортированных по дате создания (новые первыми).
     *
     * @return список сотрудников
     */
    List<Employee> findAllByOrderByCreatedAtDesc();

    /**
     * Удаляет сотрудника.
     *
     * @param employee сотрудник
     */
    void delete(Employee employee);
}
