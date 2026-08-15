package com.assessment.management.adapter.in;

import com.assessment.dto.CreateEmployeeRequestDto;
import com.assessment.dto.EmployeeDto;
import com.assessment.dto.UpdateEmployeeRequestDto;
import com.assessment.dto.mapper.EmployeeMapper;
import com.assessment.management.application.EmployeeCrudUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Тонкий HTTP-адаптер CRUD-операций над сотрудниками (путь {@code /api/admin/employees}).
 * Требует роль ADMIN, делегирует бизнес-логику {@link EmployeeCrudUseCase}, маппит DTO через {@link EmployeeMapper}.
 * Привязка к компетенции выполняется внутри use case по {@code competencyId} из DTO.
 */
@RestController
@RequestMapping("/api/admin")
public class EmployeeController {

    private final EmployeeCrudUseCase employeeCrud;
    private final EmployeeMapper employeeMapper;

    public EmployeeController(EmployeeCrudUseCase employeeCrud, EmployeeMapper employeeMapper) {
        this.employeeCrud = employeeCrud;
        this.employeeMapper = employeeMapper;
    }

    /**
     * Создает нового сотрудника. При указании компетенции разрешает ее по идентификатору.
     *
     * @param dto данные сотрудника из тела запроса
     * @return созданный сотрудник с HTTP 201
     */
    @PostMapping("/employees")
    public ResponseEntity<EmployeeDto> createEmployee(@Valid @RequestBody CreateEmployeeRequestDto dto) {
        var employee = employeeMapper.toEntity(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeMapper.toDto(employeeCrud.createEmployee(employee, dto.getCompetencyId())));
    }

    /**
     * Возвращает список всех сотрудников, отсортированных по дате создания.
     *
     * @return список сотрудников с HTTP 200
     */
    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeDto>> listEmployees() {
        return ResponseEntity.ok(employeeCrud.listEmployees().stream()
                .map(employeeMapper::toDto)
                .collect(Collectors.toList()));
    }

    /**
     * Возвращает сотрудника по идентификатору.
     *
     * @param id идентификатор сотрудника
     * @return сотрудник с HTTP 200 или HTTP 404, если не найден
     */
    @GetMapping("/employees/{id}")
    public ResponseEntity<EmployeeDto> getEmployee(@PathVariable UUID id) {
        return employeeCrud.getEmployee(id)
                .map(employeeMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Обновляет данные сотрудника, включая ФИО, должность, отдел и компетенцию.
     *
     * @param id  идентификатор обновляемого сотрудника
     * @param dto новые данные сотрудника из тела запроса
     * @return обновленный сотрудник с HTTP 200 или HTTP 404
     */
    @PutMapping("/employees/{id}")
    public ResponseEntity<EmployeeDto> updateEmployee(@PathVariable UUID id,
                                                      @Valid @RequestBody UpdateEmployeeRequestDto dto) {
        return employeeCrud.updateEmployee(id, e -> {
                    employeeMapper.updateEntity(e, dto);
                    return e;
                }, dto.getCompetencyId())
                .map(employeeMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Удаляет сотрудника вместе с его пригласительными токенами и сессиями.
     *
     * @param id идентификатор удаляемого сотрудника
     * @return HTTP 204 при успешном удалении или HTTP 404
     */
    @DeleteMapping("/employees/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable UUID id) {
        if (employeeCrud.deleteEmployee(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}