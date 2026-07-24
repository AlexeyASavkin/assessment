package com.assessment.repository;

import com.assessment.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для управления сущностями сотрудников, проходящих оценку компетенций.
 */
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    /**
     * Возвращает список всех сотрудников, отсортированных по дате создания в порядке возрастания.
     *
     * @return список сотрудников
     */
    List<Employee> findAllByOrderByCreatedAtAsc();
}
