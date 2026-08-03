package com.assessment.management.adapter.out;

import com.assessment.entity.Employee;
import com.assessment.management.port.out.EmployeeRepositoryPort;
import com.assessment.repository.EmployeeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-адаптер выходного порта доступа к сотрудникам в management-контексте.
 *
 * <p>Оборачивает {@link EmployeeRepository} и делегирует ему вызовы без
 * дополнительной логики (сущность используется как есть).
 */
@Component
public class EmployeeJpaAdapter implements EmployeeRepositoryPort {

    private final EmployeeRepository employeeRepository;

    public EmployeeJpaAdapter(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public Employee save(Employee employee) {
        return employeeRepository.save(employee);
    }

    @Override
    public Optional<Employee> findById(UUID id) {
        return employeeRepository.findById(id);
    }

    @Override
    public List<Employee> findAllByOrderByCreatedAtDesc() {
        return employeeRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public void delete(Employee employee) {
        employeeRepository.delete(employee);
    }
}
