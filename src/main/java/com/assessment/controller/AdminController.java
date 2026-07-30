package com.assessment.controller;

import com.assessment.dto.*;
import com.assessment.entity.*;
import com.assessment.dto.mapper.CompetencyMapper;
import com.assessment.dto.mapper.EmployeeMapper;
import com.assessment.dto.mapper.InviteTokenMapper;
import com.assessment.dto.mapper.QuestionBankMapper;
import com.assessment.dto.mapper.SectionMapper;
import com.assessment.dto.mapper.TopicMapper;
import com.assessment.repository.*;
import com.assessment.security.HmacTokenValidator;
import com.assessment.service.AiProviderService;
import com.assessment.service.QuestionGeneratorService;
import com.assessment.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST-контроллер для административных операций.
 * Обрабатывает все запросы по пути {@code /api/admin} и требует аутентификации с ролью ADMIN.
 * Управляет компетенциями, разделами, темами,
 * банком вопросов, сотрудниками и настройками ИИ.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CompetencyRepository competencyRepository;
    private final SectionRepository sectionRepository;
    private final TopicRepository topicRepository;
    private final EmployeeRepository employeeRepository;
    private final AssessmentInviteTokenRepository tokenRepository;
    private final HmacTokenValidator hmacValidator;
    private final AiProviderService aiProviderService;
    private final QuestionGeneratorService questionGeneratorService;
    private final QuestionBankRepository questionBankRepository;
    private final com.assessment.repository.SessionRepository sessionRepository;
    private final ReportService reportService;
    private final CompetencyMapper competencyMapper;
    private final SectionMapper sectionMapper;
    private final TopicMapper topicMapper;
    private final EmployeeMapper employeeMapper;
    private final QuestionBankMapper questionBankMapper;
    private final InviteTokenMapper inviteTokenMapper;

    /**
     * Конструктор с внедрением зависимостей репозиториев, сервисов и мапперов.
     *
     * @param competencyRepository      репозиторий компетенций
     * @param sectionRepository         репозиторий разделов
     * @param topicRepository           репозиторий тем
     * @param employeeRepository        репозиторий сотрудников
     * @param tokenRepository           репозиторий пригласительных токенов
     * @param hmacValidator             сервис валидации HMAC-токенов
     * @param aiProviderService         сервис переключения провайдера ИИ
     * @param questionGeneratorService  сервис генерации вопросов
     * @param questionBankRepository    репозиторий банка вопросов
     * @param sessionRepository         репозиторий сессий оценки
     * @param reportService             сервис формирования отчётов
     * @param competencyMapper          маппер компетенций
     * @param sectionMapper             маппер разделов
     * @param topicMapper               маппер тем
     * @param employeeMapper            маппер сотрудников
     * @param questionBankMapper        маппер банка вопросов
     * @param inviteTokenMapper         маппер пригласительных токенов
     */
    public AdminController(CompetencyRepository competencyRepository,
                           SectionRepository sectionRepository,
                           TopicRepository topicRepository,
                           EmployeeRepository employeeRepository,
                           AssessmentInviteTokenRepository tokenRepository,
                           HmacTokenValidator hmacValidator,
                           AiProviderService aiProviderService,
                           QuestionGeneratorService questionGeneratorService,
                           QuestionBankRepository questionBankRepository,
                           com.assessment.repository.SessionRepository sessionRepository,
                           ReportService reportService,
                           CompetencyMapper competencyMapper,
                           SectionMapper sectionMapper,
                           TopicMapper topicMapper,
                           EmployeeMapper employeeMapper,
                           QuestionBankMapper questionBankMapper,
                           InviteTokenMapper inviteTokenMapper) {
        this.competencyRepository = competencyRepository;
        this.sectionRepository = sectionRepository;
        this.topicRepository = topicRepository;
        this.employeeRepository = employeeRepository;
        this.tokenRepository = tokenRepository;
        this.hmacValidator = hmacValidator;
        this.aiProviderService = aiProviderService;
        this.questionGeneratorService = questionGeneratorService;
        this.questionBankRepository = questionBankRepository;
        this.sessionRepository = sessionRepository;
        this.reportService = reportService;
        this.competencyMapper = competencyMapper;
        this.sectionMapper = sectionMapper;
        this.topicMapper = topicMapper;
        this.employeeMapper = employeeMapper;
        this.questionBankMapper = questionBankMapper;
        this.inviteTokenMapper = inviteTokenMapper;
    }

    // ---- Competencies CRUD ----

    /**
     * Создает новую компетенцию.
     *
     * @param dto данные компетенции из тела запроса
     * @return созданная компетенция с HTTP 201
     */
    @PostMapping("/competencies")
    public ResponseEntity<CompetencyDto> createCompetency(@RequestBody CreateCompetencyRequestDto dto) {
        Competency entity = competencyMapper.toEntity(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(competencyMapper.toDto(competencyRepository.save(entity)));
    }

    /**
     * Возвращает список всех компетенций.
     *
     * @return список компетенций с HTTP 200
     */
    @GetMapping("/competencies")
    public ResponseEntity<List<CompetencyDto>> listCompetencies() {
        return ResponseEntity.ok(competencyRepository.findAll().stream()
                .map(competencyMapper::toDto)
                .collect(Collectors.toList()));
    }

    /**
     * Возвращает компетенцию по идентификатору.
     *
     * @param id идентификатор компетенции
     * @return компетенция с HTTP 200 или HTTP 404, если не найдена
     */
    @GetMapping("/competencies/{id}")
    public ResponseEntity<CompetencyDto> getCompetency(@PathVariable UUID id) {
        return competencyRepository.findById(id)
                .map(e -> ResponseEntity.ok(competencyMapper.toDto(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Обновляет название и описание компетенции.
     *
     * @param id      идентификатор обновляемой компетенции
     * @param dto  новые данные компетенции из тела запроса
     * @return обновленная компетенция с HTTP 200 или HTTP 404
     */
    @PutMapping("/competencies/{id}")
    public ResponseEntity<CompetencyDto> updateCompetency(@PathVariable UUID id,
                                                           @RequestBody UpdateCompetencyRequestDto dto) {
        return competencyRepository.findById(id)
                .map(entity -> {
                    competencyMapper.updateEntity(entity, dto);
                    return ResponseEntity.ok(competencyMapper.toDto(competencyRepository.save(entity)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Удаляет компетенцию по идентификатору.
     *
     * @param id идентификатор удаляемой компетенции
     * @return HTTP 204 при успешном удалении
     */
    @DeleteMapping("/competencies/{id}")
    public ResponseEntity<Void> deleteCompetency(@PathVariable UUID id) {
        competencyRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Sections CRUD ----

    /**
     * Создает раздел внутри указанной компетенции.
     *
     * @param competencyId идентификатор компетенции
     * @param dto          данные раздела из тела запроса
     * @return созданный раздел с HTTP 201 или HTTP 404, если компетенция не найдена
     */
    @PostMapping("/competencies/{competencyId}/sections")
    public ResponseEntity<SectionDto> createSection(@PathVariable UUID competencyId,
                                                     @RequestBody CreateSectionRequestDto dto) {
        return competencyRepository.findById(competencyId)
                .map(competency -> {
                    Section section = sectionMapper.toEntity(competencyId, dto);
                    section.setCompetency(competency);
                    return ResponseEntity.status(HttpStatus.CREATED)
                            .body(sectionMapper.toDto(sectionRepository.save(section)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Возвращает список разделов для указанной компетенции.
     *
     * @param competencyId идентификатор компетенции
     * @return список разделов с HTTP 200
     */
    @GetMapping("/competencies/{competencyId}/sections")
    public ResponseEntity<List<SectionDto>> listSections(@PathVariable UUID competencyId) {
        return ResponseEntity.ok(sectionRepository.findByCompetencyId(competencyId).stream()
                .map(sectionMapper::toDto)
                .collect(Collectors.toList()));
    }

    /**
     * Обновляет название, описание и порядок сортировки раздела.
     *
     * @param id      идентификатор обновляемого раздела
     * @param dto  новые данные раздела из тела запроса
     * @return обновленный раздел с HTTP 200 или HTTP 404
     */
    @PutMapping("/sections/{id}")
    public ResponseEntity<SectionDto> updateSection(@PathVariable UUID id,
                                                     @RequestBody UpdateSectionRequestDto dto) {
        return sectionRepository.findById(id)
                .map(entity -> {
                    sectionMapper.updateEntity(entity, dto);
                    return ResponseEntity.ok(sectionMapper.toDto(sectionRepository.save(entity)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Удаляет раздел по идентификатору.
     *
     * @param id идентификатор удаляемого раздела
     * @return HTTP 204 при успешном удалении
     */
    @DeleteMapping("/sections/{id}")
    public ResponseEntity<Void> deleteSection(@PathVariable UUID id) {
        sectionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Topics CRUD ----

    /**
     * Создает тему внутри указанного раздела.
     *
     * @param sectionId идентификатор раздела
     * @param dto      данные темы из тела запроса
     * @return созданная тема с HTTP 201 или HTTP 404, если раздел не найден
     */
    @PostMapping("/sections/{sectionId}/topics")
    public ResponseEntity<TopicDto> createTopic(@PathVariable UUID sectionId,
                                                 @RequestBody CreateTopicRequestDto dto) {
        return sectionRepository.findById(sectionId)
                .map(section -> {
                    Topic topic = topicMapper.toEntity(sectionId, dto);
                    topic.setSection(section);
                    return ResponseEntity.status(HttpStatus.CREATED)
                            .body(topicMapper.toDto(topicRepository.save(topic)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Возвращает список тем для указанного раздела.
     *
     * @param sectionId идентификатор раздела
     * @return список тем с HTTP 200
     */
    @GetMapping("/sections/{sectionId}/topics")
    public ResponseEntity<List<TopicDto>> listTopics(@PathVariable UUID sectionId) {
        return ResponseEntity.ok(topicRepository.findBySectionId(sectionId).stream()
                .map(topicMapper::toDto)
                .collect(Collectors.toList()));
    }

    /**
     * Обновляет название, описание, вес и порядок сортировки темы.
     *
     * @param id      идентификатор обновляемой темы
     * @param dto  новые данные темы из тела запроса
     * @return обновленная тема с HTTP 200 или HTTP 404
     */
    @PutMapping("/topics/{id}")
    public ResponseEntity<TopicDto> updateTopic(@PathVariable UUID id,
                                                 @RequestBody UpdateTopicRequestDto dto) {
        return topicRepository.findById(id)
                .map(entity -> {
                    topicMapper.updateEntity(entity, dto);
                    return ResponseEntity.ok(topicMapper.toDto(topicRepository.save(entity)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Удаляет тему по идентификатору.
     *
     * @param id идентификатор удаляемой темы
     * @return HTTP 204 при успешном удалении
     */
    @DeleteMapping("/topics/{id}")
    public ResponseEntity<Void> deleteTopic(@PathVariable UUID id) {
        topicRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Topic Question Bank ----

    /**
     * Генерирует вопросы для указанной темы с помощью ИИ и сохраняет их в банк вопросов.
     *
     *
     * @param topicId идентификатор темы
     * @param dto    тело запроса с полями {@code count} (количество, 1–10) и {@code difficulty} (ALL, JUNIOR, MIDDLE, SENIOR)
     * @return список сгенерированных вопросов с HTTP 200,
     *         HTTP 404 если тема не найдена,
     *         HTTP 400 при неверных параметрах,
     *         HTTP 503 при ошибке генерации
     */
    @PostMapping("/topics/{topicId}/questions/generate")
    public ResponseEntity<?> generateTopicQuestions(@PathVariable UUID topicId,
                                                     @RequestBody GenerateTopicQuestionsRequestDto dto) {
        if (dto.getCount() < 1 || dto.getCount() > 10) {
            return ResponseEntity.badRequest().body(Map.of("error", "count должен быть от 1 до 10"));
        }
        try {
            List<QuestionBank> questions = questionGeneratorService.generateAndSaveForTopic(
                    topicId, dto.getCount(), dto.getDifficulty() != null ? dto.getDifficulty().getValue() : "ALL");
            return ResponseEntity.ok(questions.stream().map(questionBankMapper::toDto).collect(Collectors.toList()));
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

    /**
     * Возвращает список вопросов банка для указанной темы, отсортированных по порядку.
     *
     * @param topicId идентификатор темы
     * @return список вопросов с HTTP 200
     */
    @GetMapping("/topics/{topicId}/questions")
    public ResponseEntity<List<QuestionBankItemDto>> listTopicQuestions(@PathVariable UUID topicId) {
        return ResponseEntity.ok(questionBankRepository.findByTopicIdOrderBySortOrderAsc(topicId).stream()
                .map(questionBankMapper::toDto)
                .collect(Collectors.toList()));
    }

    /**
     * Переупорядочивает вопросы банка для указанной темы.
     *
     * @param topicId   идентификатор темы
     * @param orderedIds список идентификаторов вопросов в новом порядке
     * @return HTTP 204 при успешном обновлении
     */
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

    // ---- Employees CRUD ----

    /**
     * Создает нового сотрудника. При указании компетенции разрешает ее по идентификатору.
     *
     * @param dto данные сотрудника из тела запроса
     * @return созданный сотрудник с HTTP 201
     */
    @PostMapping("/employees")
    public ResponseEntity<EmployeeDto> createEmployee(@RequestBody CreateEmployeeRequestDto dto) {
        Employee employee = employeeMapper.toEntity(dto);
        if (dto.getCompetencyId() != null) {
            Competency comp = competencyRepository.findById(dto.getCompetencyId())
                    .orElseThrow(() -> new NoSuchElementException("Компетенция не найдена: " + dto.getCompetencyId()));
            employee.setCompetency(comp);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeMapper.toDto(employeeRepository.save(employee)));
    }

    /**
     * Возвращает список всех сотрудников, отсортированных по дате создания.
     *
     * @return список сотрудников с HTTP 200
     */
    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeDto>> listEmployees() {
        return ResponseEntity.ok(employeeRepository.findAllByOrderByCreatedAtDesc().stream()
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
        return employeeRepository.findById(id)
                .map(e -> ResponseEntity.ok(employeeMapper.toDto(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Обновляет данные сотрудника, включая ФИО, должность, отдел и компетенцию.
     *
     * @param id      идентификатор обновляемого сотрудника
     * @param dto новые данные сотрудника из тела запроса
     * @return обновленный сотрудник с HTTP 200 или HTTP 404
     */
    @PutMapping("/employees/{id}")
    public ResponseEntity<EmployeeDto> updateEmployee(@PathVariable UUID id,
                                                       @RequestBody UpdateEmployeeRequestDto dto) {
        return employeeRepository.findById(id)
                .map(employee -> {
                    employeeMapper.updateEntity(employee, dto);
                    if (dto.getCompetencyId() != null) {
                        Competency comp = competencyRepository.findById(dto.getCompetencyId())
                                .orElseThrow(() -> new NoSuchElementException("Компетенция не найдена: " + dto.getCompetencyId()));
                        employee.setCompetency(comp);
                    } else {
                        employee.setCompetency(null);
                    }
                    return ResponseEntity.ok(employeeMapper.toDto(employeeRepository.save(employee)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Удаляет сотрудника вместе с его пригласительными токенами и сессиями.
     * Сессии каскадно удаляют попытки ответов через JPA CascadeType.ALL.
     *
     * @param id идентификатор удаляемого сотрудника
     * @return HTTP 204 при успешном удалении или HTTP 404
     */
    @Transactional
    @DeleteMapping("/employees/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable UUID id) {
        return employeeRepository.findById(id)
                .map(employee -> {
                    tokenRepository.deleteByEmployeeId(employee.getId());
                    new ArrayList<>(sessionRepository.findByEmployeeId(employee.getId()))
                            .forEach(session -> sessionRepository.deleteById(session.getId()));
                    employeeRepository.delete(employee);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ---- Invite Tokens ----

    /**
     * Генерирует одноразовую пригласительную ссылку для сотрудника.
     * Удаляет предыдущие токены этого сотрудника, чтобы избежать нарушения уникального ограничения.
     *
     * @param employeeId идентификатор сотрудника
     * @return пригласительный URL с HTTP 200 или HTTP 404, если сотрудник не найден
     */
    @Transactional
    @PostMapping("/employees/{employeeId}/invite")
    public ResponseEntity<String> generateInviteLink(@PathVariable UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .map(employee -> {
                    tokenRepository.deleteByEmployeeId(employeeId);
                    String token = hmacValidator.generateToken(employeeId.toString());
                    String hash = hmacValidator.generateToken(token);
                    AssessmentInviteToken inviteToken = AssessmentInviteToken.builder()
                            .tokenHash(hash)
                            .employee(employee)
                            .expiresAt(Instant.now().plusSeconds(72 * 3600))
                            .build();
                    tokenRepository.save(inviteToken);
                    return ResponseEntity.ok("/api/employee/invite/" + token);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Возвращает список всех выданных пригласительных токенов.
     *
     * @return список токенов с HTTP 200
     */
    @GetMapping("/tokens")
    public ResponseEntity<List<AssessmentInviteTokenDto>> listTokens() {
        return ResponseEntity.ok(tokenRepository.findAll().stream()
                .map(inviteTokenMapper::toDto)
                .collect(Collectors.toList()));
    }

    // ---- Applications ----

    /**
     * Возвращает список всех заявок на оценку.
     * Заявка связывает пригласительный токен, сотрудника, сессию и агрегированные метрики результата.
     *
     * @return список заявок с HTTP 200
     */
    @GetMapping("/applications")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ApplicationSummaryDto>> listApplications() {
        List<AssessmentInviteToken> tokens = tokenRepository.findAll();
        List<ApplicationSummaryDto> summaries = new ArrayList<>();
        for (AssessmentInviteToken token : tokens) {
            Employee employee = token.getEmployee();
            com.assessment.entity.Session session = token.getSession();
            UUID sessionId = session != null ? session.getId() : null;
            String sessionStatus = session != null ? session.getStatus() : null;
            Instant completedAt = session != null && "COMPLETED".equals(session.getStatus())
                    ? session.getUpdatedAt() : null;
            String competencyName = employee != null && employee.getCompetency() != null
                    ? employee.getCompetency().getName() : null;
            BigDecimal averageScore = null;
            boolean passed = false;
            if (sessionId != null && "COMPLETED".equals(sessionStatus)) {
                ReportService.SessionSummary summary = reportService.computeSummary(sessionId);
                averageScore = summary.averageScore();
                passed = summary.passed();
            }
            summaries.add(new ApplicationSummaryDto()
                    .tokenId(token.getId())
                    .employeeId(employee != null ? employee.getId() : null)
                    .employeeName(employee != null ? employee.getFullName() : null)
                    .competencyName(competencyName)
                    .sessionStatus(sessionStatus != null ? ApplicationSummaryDto.SessionStatusEnum.fromValue(sessionStatus) : null)
                    .sessionId(sessionId)
                    .averageScore(averageScore != null ? averageScore.floatValue() : null)
                    .passed(passed)
                    .createdAt(token.getCreatedAt() != null ? token.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null)
                    .completedAt(completedAt != null ? completedAt.atOffset(java.time.ZoneOffset.UTC) : null));
        }
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/applications/{sessionId}/report")
    public ResponseEntity<Map<String, Object>> getApplicationReport(@PathVariable UUID sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reportService.generateAdminReport(sessionId));
    }

    // ---- AI Settings ----

    /**
     * Возвращает текущие настройки провайдера ИИ и список доступных провайдеров.
     *
     * @return карта с полями {@code activeProvider} и {@code availableProviders} с HTTP 200
     */
    @GetMapping("/settings/ai")
    public ResponseEntity<GetAiSettings200ResponseDto> getAiSettings() {
        String activeProvider = aiProviderService.getActiveProvider();
        return ResponseEntity.ok(new GetAiSettings200ResponseDto()
                .activeProvider(GetAiSettings200ResponseDto.ActiveProviderEnum.fromValue(activeProvider))
                .availableProviders(List.of("gemini", "gigachat", "openrouter", "opencode")));
    }

    /**
     * Обновляет активного провайдера ИИ.
     *
     * @param dto тело запроса с полем {@code activeProvider}
     * @return обновленные настройки с HTTP 200 или HTTP 400 при отсутствии провайдера
     */
    @PutMapping("/settings/ai")
    public ResponseEntity<?> updateAiSettings(@RequestBody UpdateAiSettingsRequestDto dto) {
        String provider = dto.getActiveProvider().getValue();
        if (provider == null || provider.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        aiProviderService.setActiveProvider(provider);
        String activeProvider = aiProviderService.getActiveProvider();
        return ResponseEntity.ok(new UpdateAiSettings200ResponseDto()
                .activeProvider(activeProvider)
                .availableProviders(List.of("gemini", "gigachat", "openrouter", "opencode")));
    }

    // ---- AI Prompts ----

    /**
     * Возвращает все промты ИИ (промт оценки, генерации вопроса, уточняющего вопроса).
     *
     * @return карта с промтами
     */
    @GetMapping("/settings/ai/prompts")
    public ResponseEntity<AiPromptsDto> getAiPrompts() {
        Map<String, String> prompts = aiProviderService.getAllPrompts();
        return ResponseEntity.ok(new AiPromptsDto()
                .promptScoring(prompts.get("prompt_scoring"))
                .promptQuestion(prompts.get("prompt_question"))
                .promptFollowup(prompts.get("prompt_followup"))
                .promptRescore(prompts.get("prompt_rescore")));
    }

    /**
     * Обновляет промты ИИ. Принимает карту ключ→значение.
     * Ключи: prompt_scoring, prompt_question.
     *
     * @param dto карта с промтами
     * @return обновленные промты
     */
    @PutMapping("/settings/ai/prompts")
    public ResponseEntity<AiPromptsDto> updateAiPrompts(@RequestBody AiPromptsDto dto) {
        if (dto.getPromptScoring() != null) aiProviderService.setPrompt("prompt_scoring", dto.getPromptScoring());
        if (dto.getPromptQuestion() != null) aiProviderService.setPrompt("prompt_question", dto.getPromptQuestion());
        if (dto.getPromptFollowup() != null) aiProviderService.setPrompt("prompt_followup", dto.getPromptFollowup());
        if (dto.getPromptRescore() != null) aiProviderService.setPrompt("prompt_rescore", dto.getPromptRescore());
        return getAiPrompts();
    }

    // ---- Question Bank (for competencies) ----

    /**
     * Генерирует вопросы для всех тем указанной компетенции с помощью ИИ.
     *
     * @param competencyId идентификатор компетенции
     * @param dto         тело запроса с полями {@code count} (количество, 1–10) и {@code difficulty} (ALL, JUNIOR, MIDDLE, SENIOR)
     * @return список сгенерированных вопросов с HTTP 200,
     *         HTTP 404 если компетенция не найдена,
     *         HTTP 400 при неверных параметрах,
     *         HTTP 503 при ошибке генерации
     */
    @PostMapping("/competencies/{competencyId}/questions/generate")
    public ResponseEntity<?> generateQuestions(@PathVariable UUID competencyId,
                                                @RequestBody GenerateQuestionsRequestDto dto) {
        if (dto.getCount() < 1 || dto.getCount() > 10) {
            return ResponseEntity.badRequest().body(Map.of("error", "count должен быть от 1 до 10"));
        }
        try {
            List<QuestionBank> questions = questionGeneratorService.generateAndSave(
                    competencyId, dto.getCount(), dto.getDifficulty() != null ? dto.getDifficulty().getValue() : "ALL");
            return ResponseEntity.ok(questions.stream().map(questionBankMapper::toDto).collect(Collectors.toList()));
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

    /**
     * Возвращает список вопросов банка для указанной компетенции, отсортированных по дате создания.
     *
     * @param competencyId идентификатор компетенции
     * @return список вопросов с HTTP 200
     */
    @GetMapping("/competencies/{competencyId}/questions")
    public ResponseEntity<List<QuestionBankItemDto>> listQuestions(@PathVariable UUID competencyId) {
        return ResponseEntity.ok(questionBankRepository.findByCompetencyIdOrderByCreatedAtDesc(competencyId).stream()
                .map(questionBankMapper::toDto)
                .collect(Collectors.toList()));
    }

    /**
     * Обновляет текст вопроса в банке вопросов.
     *
     * @param id   идентификатор вопроса
     * @param dto тело запроса с полем {@code questionText}
     * @return обновленный вопрос с HTTP 200, HTTP 404 если не найден, или HTTP 400 при пустом тексте
     */
    @PutMapping("/questions/{id}")
    public ResponseEntity<?> updateQuestion(@PathVariable UUID id, @RequestBody UpdateQuestionRequestDto dto) {
        if (dto.getQuestionText() == null || dto.getQuestionText().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "questionText обязателен"));
        }
        return questionBankRepository.findById(id)
                .map(question -> {
                    question.setQuestionText(dto.getQuestionText().trim());
                    return ResponseEntity.ok(questionBankMapper.toDto(questionBankRepository.save(question)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Удаляет вопрос из банка вопросов по идентификатору.
     *
     * @param id идентификатор удаляемого вопроса
     * @return HTTP 204 при успешном удалении
     */
    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable UUID id) {
        questionBankRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
