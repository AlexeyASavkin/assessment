package com.assessment.management.application;

import com.assessment.common.NotFoundException;
import com.assessment.entity.Competency;
import com.assessment.entity.Employee;
import com.assessment.management.port.out.CompetencyRepositoryPort;
import com.assessment.management.port.out.EmployeeRepositoryPort;
import com.assessment.management.port.out.TokenRepositoryPort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Реализация use case CRUD-операций над сотрудниками.
 *
 * <p>Воспроизводит бизнес-логику {@code AdminController}: при создании и обновлении
 * связь с компетенцией разрешается по идентификатору (не найдена — исключение),
 * при обновлении {@code null} снимает связь; удаление сотрудника каскадно удаляет
 * его пригласительные токены, сессии и попытки ответов (каскады JPA на
 * {@code Employee.sessions} и {@code Session.attempts}). Зависит от выходных портов
 * {@link EmployeeRepositoryPort}, {@link CompetencyRepositoryPort} и
 * {@link TokenRepositoryPort}.
 */
@Service
public class EmployeeCrudUseCaseImpl implements EmployeeCrudUseCase {

    private final EmployeeRepositoryPort employeeRepositoryPort;
    private final CompetencyRepositoryPort competencyRepositoryPort;
    private final TokenRepositoryPort tokenRepositoryPort;

    public EmployeeCrudUseCaseImpl(EmployeeRepositoryPort employeeRepositoryPort,
                                   CompetencyRepositoryPort competencyRepositoryPort,
                                   TokenRepositoryPort tokenRepositoryPort) {
        this.employeeRepositoryPort = employeeRepositoryPort;
        this.competencyRepositoryPort = competencyRepositoryPort;
        this.tokenRepositoryPort = tokenRepositoryPort;
    }

    @Override
    @Transactional
    public Employee createEmployee(Employee employee, UUID competencyId) {
        if (competencyId != null) {
            Competency comp = competencyRepositoryPort.findById(competencyId)
                    .orElseThrow(() -> new NotFoundException("Компетенция не найдена: " + competencyId));
            employee.setCompetency(comp);
        }
        return employeeRepositoryPort.save(employee);
    }

    @Override
    public List<Employee> listEmployees() {
        return employeeRepositoryPort.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Optional<Employee> getEmployee(UUID id) {
        return employeeRepositoryPort.findById(id);
    }

    @Override
    @Transactional
    public Optional<Employee> updateEmployee(UUID id, UnaryOperator<Employee> mutator, UUID competencyId) {
        return employeeRepositoryPort.findById(id)
                .map(employee -> {
                    mutator.apply(employee);
                    if (competencyId != null) {
                        Competency comp = competencyRepositoryPort.findById(competencyId)
                                .orElseThrow(() -> new NotFoundException("Компетенция не найдена: " + competencyId));
                        employee.setCompetency(comp);
                    } else {
                        employee.setCompetency(null);
                    }
                    return employeeRepositoryPort.save(employee);
                });
    }

    @Override
    @Transactional
    public boolean deleteEmployee(UUID id) {
        return employeeRepositoryPort.findById(id)
                .map(employee -> {
                    tokenRepositoryPort.deleteByEmployeeId(employee.getId());
                    // Сессии и попытки ответов удаляются каскадно через JPA-связи
                    // (Employee.sessions → Session.attempts).
                    employeeRepositoryPort.delete(employee);
                    return true;
                })
                .orElse(false);
    }
}
