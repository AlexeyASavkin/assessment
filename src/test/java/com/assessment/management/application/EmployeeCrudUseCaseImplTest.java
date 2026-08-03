package com.assessment.management.application;

import com.assessment.common.NotFoundException;
import com.assessment.entity.Competency;
import com.assessment.entity.Employee;
import com.assessment.management.port.out.CompetencyRepositoryPort;
import com.assessment.management.port.out.EmployeeRepositoryPort;
import com.assessment.management.port.out.TokenRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeCrudUseCase: CRUD сотрудников и каскадное удаление")
class EmployeeCrudUseCaseImplTest {

    @Mock
    private EmployeeRepositoryPort employeeRepositoryPort;

    @Mock
    private CompetencyRepositoryPort competencyRepositoryPort;

    @Mock
    private TokenRepositoryPort tokenRepositoryPort;

    private EmployeeCrudUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new EmployeeCrudUseCaseImpl(
                employeeRepositoryPort, competencyRepositoryPort, tokenRepositoryPort);
    }

    @Test
    @DisplayName("createEmployee без competencyId сохраняет сотрудника без привязки к компетенции")
    void createEmployeeWithoutCompetency() {
        Employee employee = Employee.builder().fullName("Петров Пётр").build();
        when(employeeRepositoryPort.save(employee)).thenReturn(employee);

        Employee saved = useCase.createEmployee(employee, null);

        assertSame(employee, saved);
        assertNull(saved.getCompetency());
        verify(employeeRepositoryPort).save(employee);
        verifyNoInteractions(competencyRepositoryPort);
    }

    @Test
    @DisplayName("createEmployee с competencyId привязывает найденную компетенцию")
    void createEmployeeWithCompetency() {
        UUID competencyId = UUID.randomUUID();
        Competency competency = Competency.builder().id(competencyId).name("Java").build();
        Employee employee = Employee.builder().fullName("Петров Пётр").build();
        when(competencyRepositoryPort.findById(competencyId)).thenReturn(Optional.of(competency));
        when(employeeRepositoryPort.save(employee)).thenReturn(employee);

        Employee saved = useCase.createEmployee(employee, competencyId);

        assertSame(competency, saved.getCompetency());
        verify(employeeRepositoryPort).save(employee);
    }

    @Test
    @DisplayName("createEmployee бросает NotFoundException, если компетенция не найдена")
    void createEmployeeCompetencyNotFound() {
        UUID competencyId = UUID.randomUUID();
        Employee employee = Employee.builder().fullName("Петров Пётр").build();
        when(competencyRepositoryPort.findById(competencyId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> useCase.createEmployee(employee, competencyId));

        assertEquals("Компетенция не найдена: " + competencyId, exception.getMessage());
        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("listEmployees делегирует findAllByOrderByCreatedAtDesc")
    void listEmployeesDelegatesToPort() {
        List<Employee> employees = List.of(Employee.builder().fullName("Иванов Иван").build());
        when(employeeRepositoryPort.findAllByOrderByCreatedAtDesc()).thenReturn(employees);

        List<Employee> result = useCase.listEmployees();

        assertSame(employees, result);
        verify(employeeRepositoryPort).findAllByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("getEmployee делегирует findById")
    void getEmployeeDelegatesToPort() {
        UUID id = UUID.randomUUID();
        Employee employee = Employee.builder().id(id).build();
        when(employeeRepositoryPort.findById(id)).thenReturn(Optional.of(employee));

        Optional<Employee> result = useCase.getEmployee(id);

        assertTrue(result.isPresent());
        assertSame(employee, result.get());
        verify(employeeRepositoryPort).findById(id);
    }

    @Test
    @DisplayName("updateEmployee применяет мутатор и привязывает компетенцию")
    void updateEmployeeWithCompetency() {
        UUID id = UUID.randomUUID();
        UUID competencyId = UUID.randomUUID();
        Competency competency = Competency.builder().id(competencyId).name("Java").build();
        Employee employee = Employee.builder().id(id).fullName("Старое Имя").build();
        when(employeeRepositoryPort.findById(id)).thenReturn(Optional.of(employee));
        when(competencyRepositoryPort.findById(competencyId)).thenReturn(Optional.of(competency));
        when(employeeRepositoryPort.save(employee)).thenReturn(employee);

        Optional<Employee> updated = useCase.updateEmployee(id, e -> {
            e.setFullName("Новое Имя");
            return e;
        }, competencyId);

        assertTrue(updated.isPresent());
        assertEquals("Новое Имя", updated.get().getFullName());
        assertSame(competency, updated.get().getCompetency());
        verify(employeeRepositoryPort).save(employee);
    }

    @Test
    @DisplayName("updateEmployee с null competencyId снимает привязку к компетенции")
    void updateEmployeeNullCompetencyClearsLink() {
        UUID id = UUID.randomUUID();
        Employee employee = Employee.builder().id(id)
                .competency(Competency.builder().id(UUID.randomUUID()).build())
                .build();
        when(employeeRepositoryPort.findById(id)).thenReturn(Optional.of(employee));
        when(employeeRepositoryPort.save(employee)).thenReturn(employee);

        Optional<Employee> updated = useCase.updateEmployee(id, e -> e, null);

        assertTrue(updated.isPresent());
        assertNull(updated.get().getCompetency());
        verify(employeeRepositoryPort).save(employee);
        verifyNoInteractions(competencyRepositoryPort);
    }

    @Test
    @DisplayName("updateEmployee возвращает пусто, если сотрудник не найден")
    void updateEmployeeNotFound() {
        UUID id = UUID.randomUUID();
        when(employeeRepositoryPort.findById(id)).thenReturn(Optional.empty());

        Optional<Employee> updated = useCase.updateEmployee(id, e -> e, null);

        assertTrue(updated.isEmpty());
        verify(employeeRepositoryPort, never()).save(any());
        verifyNoInteractions(competencyRepositoryPort);
    }

    @Test
    @DisplayName("updateEmployee бросает NotFoundException, если компетенция не найдена")
    void updateEmployeeCompetencyNotFound() {
        UUID id = UUID.randomUUID();
        UUID competencyId = UUID.randomUUID();
        Employee employee = Employee.builder().id(id).build();
        when(employeeRepositoryPort.findById(id)).thenReturn(Optional.of(employee));
        when(competencyRepositoryPort.findById(competencyId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> useCase.updateEmployee(id, e -> e, competencyId));

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("deleteEmployee удаляет токены и сотрудника, сессии удаляются каскадом JPA")
    void deleteEmployeeSuccess() {
        UUID id = UUID.randomUUID();
        Employee employee = Employee.builder().id(id).build();
        when(employeeRepositoryPort.findById(id)).thenReturn(Optional.of(employee));

        boolean deleted = useCase.deleteEmployee(id);

        assertTrue(deleted);
        verify(tokenRepositoryPort).deleteByEmployeeId(id);
        verify(employeeRepositoryPort).delete(employee);
    }

    @Test
    @DisplayName("deleteEmployee возвращает false, если сотрудник не найден")
    void deleteEmployeeNotFound() {
        UUID id = UUID.randomUUID();
        when(employeeRepositoryPort.findById(id)).thenReturn(Optional.empty());

        boolean deleted = useCase.deleteEmployee(id);

        assertFalse(deleted);
        verify(employeeRepositoryPort, never()).delete(any());
        verifyNoInteractions(tokenRepositoryPort);
    }
}
