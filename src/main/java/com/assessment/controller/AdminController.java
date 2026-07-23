package com.assessment.controller;

import com.assessment.entity.*;
import com.assessment.repository.*;
import com.assessment.security.HmacTokenValidator;
import com.assessment.service.AiProviderService;
import com.assessment.service.QuestionGeneratorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CompetencyRepository competencyRepository;
    private final CriteriaRepository criteriaRepository;
    private final CriteriaLevelRepository criteriaLevelRepository;
    private final SectionRepository sectionRepository;
    private final TopicRepository topicRepository;
    private final EmployeeRepository employeeRepository;
    private final AssessmentInviteTokenRepository tokenRepository;
    private final HmacTokenValidator hmacValidator;
    private final AiProviderService aiProviderService;
    private final QuestionGeneratorService questionGeneratorService;
    private final QuestionBankRepository questionBankRepository;

    public AdminController(CompetencyRepository competencyRepository,
                           CriteriaRepository criteriaRepository,
                           CriteriaLevelRepository criteriaLevelRepository,
                           SectionRepository sectionRepository,
                           TopicRepository topicRepository,
                           EmployeeRepository employeeRepository,
                           AssessmentInviteTokenRepository tokenRepository,
                           HmacTokenValidator hmacValidator,
                           AiProviderService aiProviderService,
                           QuestionGeneratorService questionGeneratorService,
                           QuestionBankRepository questionBankRepository) {
        this.competencyRepository = competencyRepository;
        this.criteriaRepository = criteriaRepository;
        this.criteriaLevelRepository = criteriaLevelRepository;
        this.sectionRepository = sectionRepository;
        this.topicRepository = topicRepository;
        this.employeeRepository = employeeRepository;
        this.tokenRepository = tokenRepository;
        this.hmacValidator = hmacValidator;
        this.aiProviderService = aiProviderService;
        this.questionGeneratorService = questionGeneratorService;
        this.questionBankRepository = questionBankRepository;
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

    // ---- Sections ----

    @PostMapping("/competencies/{competencyId}/sections")
    public ResponseEntity<Section> createSection(@PathVariable UUID competencyId, @RequestBody Section section) {
        return competencyRepository.findById(competencyId)
                .map(competency -> {
                    section.setCompetency(competency);
                    return ResponseEntity.status(HttpStatus.CREATED).body(sectionRepository.save(section));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/competencies/{competencyId}/sections")
    public ResponseEntity<List<Section>> listSections(@PathVariable UUID competencyId) {
        return ResponseEntity.ok(sectionRepository.findByCompetencyId(competencyId));
    }

    @PutMapping("/sections/{id}")
    public ResponseEntity<Section> updateSection(@PathVariable UUID id, @RequestBody Section updated) {
        return sectionRepository.findById(id)
                .map(section -> {
                    section.setName(updated.getName());
                    section.setDescription(updated.getDescription());
                    section.setSortOrder(updated.getSortOrder());
                    return ResponseEntity.ok(sectionRepository.save(section));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/sections/{id}")
    public ResponseEntity<Void> deleteSection(@PathVariable UUID id) {
        sectionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Topics ----

    @PostMapping("/sections/{sectionId}/topics")
    public ResponseEntity<Topic> createTopic(@PathVariable UUID sectionId, @RequestBody Topic topic) {
        return sectionRepository.findById(sectionId)
                .map(section -> {
                    topic.setSection(section);
                    return ResponseEntity.status(HttpStatus.CREATED).body(topicRepository.save(topic));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/sections/{sectionId}/topics")
    public ResponseEntity<List<Topic>> listTopics(@PathVariable UUID sectionId) {
        return ResponseEntity.ok(topicRepository.findBySectionId(sectionId));
    }

    @PutMapping("/topics/{id}")
    public ResponseEntity<Topic> updateTopic(@PathVariable UUID id, @RequestBody Topic updated) {
        return topicRepository.findById(id)
                .map(topic -> {
                    topic.setName(updated.getName());
                    topic.setDescription(updated.getDescription());
                    topic.setWeight(updated.getWeight());
                    topic.setSortOrder(updated.getSortOrder());
                    return ResponseEntity.ok(topicRepository.save(topic));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/topics/{id}")
    public ResponseEntity<Void> deleteTopic(@PathVariable UUID id) {
        topicRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Topic Question Bank ----

    @PostMapping("/topics/{topicId}/questions/generate")
    public ResponseEntity<?> generateTopicQuestions(@PathVariable UUID topicId,
                                                     @RequestBody Map<String, Object> body) {
        Object countObj = body.get("count");
        String difficulty = (String) body.getOrDefault("difficulty", "ALL");

        int count;
        try {
            count = countObj instanceof Number ? ((Number) countObj).intValue() : Integer.parseInt(countObj.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "count должен быть числом от 1 до 10"));
        }

        if (count < 1 || count > 10) {
            return ResponseEntity.badRequest().body(Map.of("error", "count должен быть от 1 до 10"));
        }
        if (!VALID_DIFFICULTIES.contains(difficulty)) {
            return ResponseEntity.badRequest().body(Map.of("error", "difficulty должен быть: ALL, JUNIOR, MIDDLE или SENIOR"));
        }

        try {
            List<QuestionBank> questions = questionGeneratorService.generateAndSaveForTopic(topicId, count, difficulty);
            return ResponseEntity.ok(questions);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Ошибка генерации вопросов: " + e.getMessage()));
        }
    }

    @GetMapping("/topics/{topicId}/questions")
    public ResponseEntity<List<QuestionBank>> listTopicQuestions(@PathVariable UUID topicId) {
        return ResponseEntity.ok(questionBankRepository.findByTopicIdOrderBySortOrderAsc(topicId));
    }

    @PutMapping("/topics/{topicId}/questions/reorder")
    public ResponseEntity<Void> reorderTopicQuestions(@PathVariable UUID topicId,
                                                      @RequestBody List<UUID> orderedIds) {
        List<QuestionBank> questions = questionBankRepository.findByTopicIdOrderBySortOrderAsc(topicId);
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID id = orderedIds.get(i);
            int sortOrder = i;
            questions.stream()
                    .filter(q -> q.getId().equals(id))
                    .findFirst()
                    .ifPresent(q -> q.setSortOrder(sortOrder));
        }
        questionBankRepository.saveAll(questions);
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

    @GetMapping("/settings/ai")
    public ResponseEntity<Map<String, Object>> getAiSettings() {
        String activeProvider = aiProviderService.getActiveProvider();
        Map<String, Object> settings = new java.util.HashMap<>();
        settings.put("activeProvider", activeProvider);
        settings.put("availableProviders", java.util.List.of("gemini", "gigachat"));
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/settings/ai")
    public ResponseEntity<Map<String, Object>> updateAiSettings(@RequestBody Map<String, String> body) {
        String provider = body.get("activeProvider");
        if (provider == null || provider.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        aiProviderService.setActiveProvider(provider);
        return getAiSettings();
    }

    @GetMapping("/settings/ai/keys")
    public ResponseEntity<Map<String, String>> getAiKeys() {
        Map<String, String> keys = new java.util.HashMap<>();
        keys.put("geminiApiKey", aiProviderService.getApiKey("gemini"));
        keys.put("gigachatApiKey", aiProviderService.getApiKey("gigachat"));
        return ResponseEntity.ok(keys);
    }

    @PutMapping("/settings/ai/keys")
    public ResponseEntity<Map<String, String>> updateAiKeys(@RequestBody Map<String, String> body) {
        String geminiKey = body.get("geminiApiKey");
        String gigachatKey = body.get("gigachatApiKey");
        if (geminiKey != null) {
            aiProviderService.setApiKey("gemini", geminiKey);
        }
        if (gigachatKey != null) {
            aiProviderService.setApiKey("gigachat", gigachatKey);
        }
        return getAiKeys();
    }

    // ---- Question Bank ----

    private static final Set<String> VALID_DIFFICULTIES = Set.of("ALL", "JUNIOR", "MIDDLE", "SENIOR");

    @PostMapping("/competencies/{competencyId}/questions/generate")
    public ResponseEntity<?> generateQuestions(@PathVariable UUID competencyId,
                                               @RequestBody Map<String, Object> body) {
        Object countObj = body.get("count");
        String difficulty = (String) body.getOrDefault("difficulty", "ALL");

        int count;
        try {
            count = countObj instanceof Number ? ((Number) countObj).intValue() : Integer.parseInt(countObj.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "count должен быть числом от 1 до 10"));
        }

        if (count < 1 || count > 10) {
            return ResponseEntity.badRequest().body(Map.of("error", "count должен быть от 1 до 10"));
        }
        if (!VALID_DIFFICULTIES.contains(difficulty)) {
            return ResponseEntity.badRequest().body(Map.of("error", "difficulty должен быть: ALL, JUNIOR, MIDDLE или SENIOR"));
        }

        try {
            List<QuestionBank> questions = questionGeneratorService.generateAndSave(competencyId, count, difficulty);
            return ResponseEntity.ok(questions);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Ошибка генерации вопросов: " + e.getMessage()));
        }
    }

    @GetMapping("/competencies/{competencyId}/questions")
    public ResponseEntity<List<QuestionBank>> listQuestions(@PathVariable UUID competencyId) {
        return ResponseEntity.ok(questionBankRepository.findByCompetencyIdOrderByCreatedAtDesc(competencyId));
    }

    @PutMapping("/questions/{id}")
    public ResponseEntity<?> updateQuestion(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        String questionText = body.get("questionText");
        if (questionText == null || questionText.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "questionText обязателен"));
        }
        return questionBankRepository.findById(id)
                .map(question -> {
                    question.setQuestionText(questionText.trim());
                    return ResponseEntity.ok(questionBankRepository.save(question));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable UUID id) {
        questionBankRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
