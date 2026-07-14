package com.assessment.controller;

import com.assessment.entity.*;
import com.assessment.repository.*;
import com.assessment.security.HmacTokenValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CompetencyRepository competencyRepository;
    private final CriteriaRepository criteriaRepository;
    private final CriteriaLevelRepository criteriaLevelRepository;
    private final EmployeeRepository employeeRepository;
    private final AssessmentInviteTokenRepository tokenRepository;
    private final HmacTokenValidator hmacValidator;

    public AdminController(CompetencyRepository competencyRepository,
                           CriteriaRepository criteriaRepository,
                           CriteriaLevelRepository criteriaLevelRepository,
                           EmployeeRepository employeeRepository,
                           AssessmentInviteTokenRepository tokenRepository,
                           HmacTokenValidator hmacValidator) {
        this.competencyRepository = competencyRepository;
        this.criteriaRepository = criteriaRepository;
        this.criteriaLevelRepository = criteriaLevelRepository;
        this.employeeRepository = employeeRepository;
        this.tokenRepository = tokenRepository;
        this.hmacValidator = hmacValidator;
    }

    @PostMapping("/competencies")
    public ResponseEntity<Competency> createCompetency(@RequestBody Competency competency) {
        return ResponseEntity.status(HttpStatus.CREATED).body(competencyRepository.save(competency));
    }

    @GetMapping("/competencies")
    public ResponseEntity<List<Competency>> listCompetencies() {
        return ResponseEntity.ok(competencyRepository.findAll());
    }

    @GetMapping("/competencies/{id}")
    public ResponseEntity<Competency> getCompetency(@PathVariable UUID id) {
        return competencyRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/competencies/{id}")
    public ResponseEntity<Competency> updateCompetency(@PathVariable UUID id, @RequestBody Competency updated) {
        return competencyRepository.findById(id)
                .map(competency -> {
                    competency.setName(updated.getName());
                    competency.setDescription(updated.getDescription());
                    return ResponseEntity.ok(competencyRepository.save(competency));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/competencies/{id}")
    public ResponseEntity<Void> deleteCompetency(@PathVariable UUID id) {
        competencyRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/competencies/{competencyId}/criteria")
    public ResponseEntity<Criteria> createCriteria(@PathVariable UUID competencyId, @RequestBody Criteria criteria) {
        return competencyRepository.findById(competencyId)
                .map(competency -> {
                    criteria.setCompetency(competency);
                    return ResponseEntity.status(HttpStatus.CREATED).body(criteriaRepository.save(criteria));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/competencies/{competencyId}/criteria")
    public ResponseEntity<List<Criteria>> listCriteria(@PathVariable UUID competencyId) {
        return ResponseEntity.ok(criteriaRepository.findByCompetencyId(competencyId));
    }

    @PutMapping("/criteria/{id}")
    public ResponseEntity<Criteria> updateCriteria(@PathVariable UUID id, @RequestBody Criteria updated) {
        return criteriaRepository.findById(id)
                .map(criteria -> {
                    criteria.setName(updated.getName());
                    criteria.setDescription(updated.getDescription());
                    criteria.setWeight(updated.getWeight());
                    return ResponseEntity.ok(criteriaRepository.save(criteria));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/criteria/{id}")
    public ResponseEntity<Void> deleteCriteria(@PathVariable UUID id) {
        criteriaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/criteria/{criteriaId}/levels")
    public ResponseEntity<CriteriaLevel> createLevel(@PathVariable UUID criteriaId, @RequestBody CriteriaLevel level) {
        return criteriaRepository.findById(criteriaId)
                .map(criteria -> {
                    level.setCriteria(criteria);
                    return ResponseEntity.status(HttpStatus.CREATED).body(criteriaLevelRepository.save(level));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/criteria/{criteriaId}/levels")
    public ResponseEntity<List<CriteriaLevel>> listLevels(@PathVariable UUID criteriaId) {
        return ResponseEntity.ok(criteriaLevelRepository.findByCriteriaId(criteriaId));
    }

    @PutMapping("/criteria/levels/{id}")
    public ResponseEntity<CriteriaLevel> updateLevel(@PathVariable UUID id, @RequestBody CriteriaLevel updated) {
        return criteriaLevelRepository.findById(id)
                .map(level -> {
                    level.setLevel(updated.getLevel());
                    level.setRequirements(updated.getRequirements());
                    return ResponseEntity.ok(criteriaLevelRepository.save(level));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/criteria/levels/{id}")
    public ResponseEntity<Void> deleteLevel(@PathVariable UUID id) {
        criteriaLevelRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/employees")
    public ResponseEntity<Employee> createEmployee(@RequestBody Employee employee) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeRepository.save(employee));
    }

    @GetMapping("/employees")
    public ResponseEntity<List<Employee>> listEmployees() {
        return ResponseEntity.ok(employeeRepository.findAll());
    }

    @GetMapping("/employees/{id}")
    public ResponseEntity<Employee> getEmployee(@PathVariable UUID id) {
        return employeeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable UUID id, @RequestBody Employee updated) {
        return employeeRepository.findById(id)
                .map(employee -> {
                    employee.setFullName(updated.getFullName());
                    employee.setPosition(updated.getPosition());
                    employee.setDepartment(updated.getDepartment());
                    return ResponseEntity.ok(employeeRepository.save(employee));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/employees/{employeeId}/invite")
    public ResponseEntity<String> generateInviteLink(@PathVariable UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .map(employee -> {
                    String token = hmacValidator.generateToken(employeeId.toString());
                    String hash = hmacValidator.generateToken(token);

                    AssessmentInviteToken inviteToken = AssessmentInviteToken.builder()
                            .tokenHash(hash)
                            .employee(employee)
                            .expiresAt(LocalDateTime.now().plusHours(72))
                            .build();
                    tokenRepository.save(inviteToken);

                    String inviteUrl = "/api/employee/invite/" + token;
                    return ResponseEntity.ok(inviteUrl);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tokens")
    public ResponseEntity<List<AssessmentInviteToken>> listTokens() {
        return ResponseEntity.ok(tokenRepository.findAll());
    }
}
