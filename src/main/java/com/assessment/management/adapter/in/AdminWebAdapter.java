package com.assessment.management.adapter.in;

import com.assessment.dto.AiPromptsDto;
import com.assessment.dto.ApplicationSummaryDto;
import com.assessment.dto.AssessmentInviteTokenDto;
import com.assessment.dto.CompetencyDto;
import com.assessment.dto.CreateCompetencyRequestDto;
import com.assessment.dto.CreateEmployeeRequestDto;
import com.assessment.dto.CreateSectionRequestDto;
import com.assessment.dto.CreateTopicRequestDto;
import com.assessment.dto.EmployeeDto;
import com.assessment.dto.GenerateQuestionsRequestDto;
import com.assessment.dto.GenerateTopicQuestionsRequestDto;
import com.assessment.dto.GetAiSettings200ResponseDto;
import com.assessment.dto.QuestionBankItemDto;
import com.assessment.dto.SectionDto;
import com.assessment.dto.TopicDto;
import com.assessment.dto.UpdateAiSettings200ResponseDto;
import com.assessment.dto.UpdateAiSettingsRequestDto;
import com.assessment.dto.UpdateCompetencyRequestDto;
import com.assessment.dto.UpdateEmployeeRequestDto;
import com.assessment.dto.UpdateQuestionRequestDto;
import com.assessment.dto.UpdateSectionRequestDto;
import com.assessment.dto.UpdateTopicRequestDto;
import com.assessment.dto.mapper.CompetencyMapper;
import com.assessment.dto.mapper.EmployeeMapper;
import com.assessment.dto.mapper.InviteTokenMapper;
import com.assessment.dto.mapper.QuestionBankMapper;
import com.assessment.dto.mapper.SectionMapper;
import com.assessment.dto.mapper.TopicMapper;
import com.assessment.management.application.AiSettingsUseCase;
import com.assessment.management.application.ApplicationManagementUseCase;
import com.assessment.management.application.ApplicationSummary;
import com.assessment.management.application.CompetencyCrudUseCase;
import com.assessment.management.application.EmployeeCrudUseCase;
import com.assessment.management.application.QuestionBankManagementUseCase;
import com.assessment.management.application.SectionCrudUseCase;
import com.assessment.management.application.TokenManagementUseCase;
import com.assessment.management.application.TopicCrudUseCase;
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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Входящий HTTP-адаптер (driving adapter) административных операций.
 *
 * <p>Тонкий контроллер: обрабатывает все запросы по пути {@code /api/admin},
 * требует аутентификации с ролью ADMIN, преобразует DTO ↔ сущности через
 * MapStruct-мапперы и делегирует бизнес-логику use case'ам management-контекста.
 * Не инжектит репозитории и не работает с JPA-сущностями напрямую.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminWebAdapter {

    private final CompetencyCrudUseCase competencyCrud;
    private final SectionCrudUseCase sectionCrud;
    private final TopicCrudUseCase topicCrud;
    private final EmployeeCrudUseCase employeeCrud;
    private final TokenManagementUseCase tokenManagement;
    private final QuestionBankManagementUseCase questionBankManagement;
    private final AiSettingsUseCase aiSettings;
    private final ApplicationManagementUseCase applicationManagement;
    private final CompetencyMapper competencyMapper;
    private final SectionMapper sectionMapper;
    private final TopicMapper topicMapper;
    private final EmployeeMapper employeeMapper;
    private final QuestionBankMapper questionBankMapper;
    private final InviteTokenMapper inviteTokenMapper;

    /**
     * Конструктор с внедрением use case'ов management-контекста и MapStruct-мапперов.
     *
     * @param competencyCrud         use case CRUD компетенций
     * @param sectionCrud            use case CRUD разделов
     * @param topicCrud              use case CRUD тем
     * @param employeeCrud           use case CRUD сотрудников
     * @param tokenManagement        use case управления пригласительными токенами
     * @param questionBankManagement use case управления банком вопросов
     * @param aiSettings             use case настроек ИИ
     * @param applicationManagement  use case заявок на оценку и отчётов
     * @param competencyMapper       маппер компетенций
     * @param sectionMapper          маппер разделов
     * @param topicMapper            маппер тем
     * @param employeeMapper         маппер сотрудников
     * @param questionBankMapper     маппер банка вопросов
     * @param inviteTokenMapper      маппер пригласительных токенов
     */
    public AdminWebAdapter(CompetencyCrudUseCase competencyCrud,
                           SectionCrudUseCase sectionCrud,
                           TopicCrudUseCase topicCrud,
                           EmployeeCrudUseCase employeeCrud,
                           TokenManagementUseCase tokenManagement,
                           QuestionBankManagementUseCase questionBankManagement,
                           AiSettingsUseCase aiSettings,
                           ApplicationManagementUseCase applicationManagement,
                           CompetencyMapper competencyMapper,
                           SectionMapper sectionMapper,
                           TopicMapper topicMapper,
                           EmployeeMapper employeeMapper,
                           QuestionBankMapper questionBankMapper,
                           InviteTokenMapper inviteTokenMapper) {
        this.competencyCrud = competencyCrud;
        this.sectionCrud = sectionCrud;
        this.topicCrud = topicCrud;
        this.employeeCrud = employeeCrud;
        this.tokenManagement = tokenManagement;
        this.questionBankManagement = questionBankManagement;
        this.aiSettings = aiSettings;
        this.applicationManagement = applicationManagement;
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
        var entity = competencyMapper.toEntity(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(competencyMapper.toDto(competencyCrud.createCompetency(entity)));
    }

    /**
     * Возвращает список всех компетенций.
     *
     * @return список компетенций с HTTP 200
     */
    @GetMapping("/competencies")
    public ResponseEntity<List<CompetencyDto>> listCompetencies() {
        return ResponseEntity.ok(competencyCrud.listCompetencies().stream()
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
        return competencyCrud.getCompetency(id)
                .map(competencyMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Обновляет название и описание компетенции.
     *
     * @param id  идентификатор обновляемой компетенции
     * @param dto новые данные компетенции из тела запроса
     * @return обновленная компетенция с HTTP 200 или HTTP 404
     */
    @PutMapping("/competencies/{id}")
    public ResponseEntity<CompetencyDto> updateCompetency(@PathVariable UUID id,
                                                           @RequestBody UpdateCompetencyRequestDto dto) {
        return competencyCrud.updateCompetency(id, e -> {
                    competencyMapper.updateEntity(e, dto);
                    return e;
                })
                .map(competencyMapper::toDto)
                .map(ResponseEntity::ok)
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
        competencyCrud.deleteCompetency(id);
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
        var section = sectionMapper.toEntity(competencyId, dto);
        return sectionCrud.createSection(competencyId, section)
                .map(sectionMapper::toDto)
                .map(e -> ResponseEntity.status(HttpStatus.CREATED).body(e))
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
        return ResponseEntity.ok(sectionCrud.listSections(competencyId).stream()
                .map(sectionMapper::toDto)
                .collect(Collectors.toList()));
    }

    /**
     * Обновляет название, описание и порядок сортировки раздела.
     *
     * @param id  идентификатор обновляемого раздела
     * @param dto новые данные раздела из тела запроса
     * @return обновленный раздел с HTTP 200 или HTTP 404
     */
    @PutMapping("/sections/{id}")
    public ResponseEntity<SectionDto> updateSection(@PathVariable UUID id,
                                                     @RequestBody UpdateSectionRequestDto dto) {
        return sectionCrud.updateSection(id, e -> {
                    sectionMapper.updateEntity(e, dto);
                    return e;
                })
                .map(sectionMapper::toDto)
                .map(ResponseEntity::ok)
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
        sectionCrud.deleteSection(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Topics CRUD ----

    /**
     * Создает тему внутри указанного раздела.
     *
     * @param sectionId идентификатор раздела
     * @param dto       данные темы из тела запроса
     * @return созданная тема с HTTP 201 или HTTP 404, если раздел не найден
     */
    @PostMapping("/sections/{sectionId}/topics")
    public ResponseEntity<TopicDto> createTopic(@PathVariable UUID sectionId,
                                                 @RequestBody CreateTopicRequestDto dto) {
        var topic = topicMapper.toEntity(sectionId, dto);
        return topicCrud.createTopic(sectionId, topic)
                .map(topicMapper::toDto)
                .map(e -> ResponseEntity.status(HttpStatus.CREATED).body(e))
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
        return ResponseEntity.ok(topicCrud.listTopics(sectionId).stream()
                .map(topicMapper::toDto)
                .collect(Collectors.toList()));
    }

    /**
     * Обновляет название, описание, вес и порядок сортировки темы.
     *
     * @param id  идентификатор обновляемой темы
     * @param dto новые данные темы из тела запроса
     * @return обновленная тема с HTTP 200 или HTTP 404
     */
    @PutMapping("/topics/{id}")
    public ResponseEntity<TopicDto> updateTopic(@PathVariable UUID id,
                                                 @RequestBody UpdateTopicRequestDto dto) {
        return topicCrud.updateTopic(id, e -> {
                    topicMapper.updateEntity(e, dto);
                    return e;
                })
                .map(topicMapper::toDto)
                .map(ResponseEntity::ok)
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
        topicCrud.deleteTopic(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Topic Question Bank ----

    /**
     * Генерирует вопросы для указанной темы с помощью ИИ и сохраняет их в банк вопросов.
     *
     * @param topicId идентификатор темы
     * @param dto     тело запроса с полями {@code count} (количество, 1–10)
     *                и {@code difficulty} (ALL, JUNIOR, MIDDLE, SENIOR)
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
            List<QuestionBankItemDto> questions = questionBankManagement
                    .generateForTopic(topicId, dto.getCount(),
                            dto.getDifficulty() != null ? dto.getDifficulty().getValue() : "ALL")
                    .stream().map(questionBankMapper::toDto)
                    .collect(Collectors.toList());
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

    /**
     * Возвращает список вопросов банка для указанной темы, отсортированных по порядку.
     *
     * @param topicId идентификатор темы
     * @return список вопросов с HTTP 200
     */
    @GetMapping("/topics/{topicId}/questions")
    public ResponseEntity<List<QuestionBankItemDto>> listTopicQuestions(@PathVariable UUID topicId) {
        return ResponseEntity.ok(questionBankManagement.listByTopic(topicId).stream()
                .map(questionBankMapper::toDto)
                .collect(Collectors.toList()));
    }

    /**
     * Переупорядочивает вопросы банка для указанной темы.
     *
     * @param topicId    идентификатор темы
     * @param orderedIds список идентификаторов вопросов в новом порядке
     * @return HTTP 204 при успешном обновлении
     */
    @PutMapping("/topics/{topicId}/questions/reorder")
    public ResponseEntity<Void> reorderTopicQuestions(@PathVariable UUID topicId,
                                                       @RequestBody List<UUID> orderedIds) {
        questionBankManagement.reorder(topicId, orderedIds);
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
                                                       @RequestBody UpdateEmployeeRequestDto dto) {
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

    // ---- Invite Tokens ----

    /**
     * Генерирует одноразовую пригласительную ссылку для сотрудника.
     *
     * @param employeeId идентификатор сотрудника
     * @return пригласительный URL с HTTP 200 или HTTP 404, если сотрудник не найден
     */
    @PostMapping("/employees/{employeeId}/invite")
    public ResponseEntity<String> generateInviteLink(@PathVariable UUID employeeId) {
        return tokenManagement.generateInviteLink(employeeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Возвращает список всех выданных пригласительных токенов.
     *
     * @return список токенов с HTTP 200
     */
    @GetMapping("/tokens")
    public ResponseEntity<List<AssessmentInviteTokenDto>> listTokens() {
        return ResponseEntity.ok(tokenManagement.listTokens().stream()
                .map(inviteTokenMapper::toDto)
                .collect(Collectors.toList()));
    }

    // ---- Applications ----

    /**
     * Возвращает список всех заявок на оценку.
     *
     * @return список заявок с HTTP 200
     */
    @GetMapping("/applications")
    public ResponseEntity<List<ApplicationSummaryDto>> listApplications() {
        return ResponseEntity.ok(applicationManagement.listApplications().stream()
                .map(this::toApplicationSummaryDto)
                .collect(Collectors.toList()));
    }

    /**
     * Возвращает развёрнутый отчёт по сессии для админ-панели.
     *
     * @param sessionId идентификатор сессии
     * @return отчёт с HTTP 200 или HTTP 404, если сессия не найдена
     */
    @GetMapping("/applications/{sessionId}/report")
    public ResponseEntity<Map<String, Object>> getApplicationReport(@PathVariable UUID sessionId) {
        return applicationManagement.getAdminReport(sessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Преобразует сводку заявки из доменной модели в API-DTO.
     *
     * @param s сводка заявки
     * @return DTO заявки
     */
    private ApplicationSummaryDto toApplicationSummaryDto(ApplicationSummary s) {
        return new ApplicationSummaryDto()
                .tokenId(s.tokenId())
                .employeeId(s.employeeId())
                .employeeName(s.employeeName())
                .competencyName(s.competencyName())
                .sessionStatus(s.sessionStatus() != null
                        ? ApplicationSummaryDto.SessionStatusEnum.fromValue(s.sessionStatus()) : null)
                .sessionId(s.sessionId())
                .averageScore(s.averageScore() != null ? s.averageScore().floatValue() : null)
                .passed(s.passed())
                .createdAt(s.createdAt() != null ? s.createdAt().atOffset(java.time.ZoneOffset.UTC) : null)
                .completedAt(s.completedAt() != null ? s.completedAt().atOffset(java.time.ZoneOffset.UTC) : null);
    }

    // ---- AI Settings ----

    /**
     * Возвращает текущие настройки провайдера ИИ и список доступных провайдеров.
     *
     * @return карта с полями {@code activeProvider} и {@code availableProviders} с HTTP 200
     */
    @GetMapping("/settings/ai")
    public ResponseEntity<GetAiSettings200ResponseDto> getAiSettings() {
        String activeProvider = aiSettings.getActiveProvider();
        return ResponseEntity.ok(new GetAiSettings200ResponseDto()
                .activeProvider(GetAiSettings200ResponseDto.ActiveProviderEnum.fromValue(activeProvider))
                .availableProviders(aiSettings.getAvailableProviders()));
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
        aiSettings.setActiveProvider(provider);
        String activeProvider = aiSettings.getActiveProvider();
        return ResponseEntity.ok(new UpdateAiSettings200ResponseDto()
                .activeProvider(activeProvider)
                .availableProviders(aiSettings.getAvailableProviders()));
    }

    // ---- AI Prompts ----

    /**
     * Возвращает все промты ИИ (промт оценки, генерации вопроса, уточняющего вопроса).
     *
     * @return карта с промтами
     */
    @GetMapping("/settings/ai/prompts")
    public ResponseEntity<AiPromptsDto> getAiPrompts() {
        Map<String, String> prompts = aiSettings.getAllPrompts();
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
        if (dto.getPromptScoring() != null) aiSettings.setPrompt("prompt_scoring", dto.getPromptScoring());
        if (dto.getPromptQuestion() != null) aiSettings.setPrompt("prompt_question", dto.getPromptQuestion());
        if (dto.getPromptFollowup() != null) aiSettings.setPrompt("prompt_followup", dto.getPromptFollowup());
        if (dto.getPromptRescore() != null) aiSettings.setPrompt("prompt_rescore", dto.getPromptRescore());
        return getAiPrompts();
    }

    // ---- Question Bank (for competencies) ----

    /**
     * Генерирует вопросы для всех тем указанной компетенции с помощью ИИ.
     *
     * @param competencyId идентификатор компетенции
     * @param dto          тело запроса с полями {@code count} (количество, 1–10)
     *                     и {@code difficulty} (ALL, JUNIOR, MIDDLE, SENIOR)
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
            List<QuestionBankItemDto> questions = questionBankManagement
                    .generateForCompetency(competencyId, dto.getCount(),
                            dto.getDifficulty() != null ? dto.getDifficulty().getValue() : "ALL")
                    .stream().map(questionBankMapper::toDto)
                    .collect(Collectors.toList());
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

    /**
     * Возвращает список вопросов банка для указанной компетенции, отсортированных по дате создания.
     *
     * @param competencyId идентификатор компетенции
     * @return список вопросов с HTTP 200
     */
    @GetMapping("/competencies/{competencyId}/questions")
    public ResponseEntity<List<QuestionBankItemDto>> listQuestions(@PathVariable UUID competencyId) {
        return ResponseEntity.ok(questionBankManagement.listByCompetency(competencyId).stream()
                .map(questionBankMapper::toDto)
                .collect(Collectors.toList()));
    }

    /**
     * Обновляет текст вопроса в банке вопросов.
     *
     * @param id  идентификатор вопроса
     * @param dto тело запроса с полем {@code questionText}
     * @return обновленный вопрос с HTTP 200, HTTP 404 если не найден, или HTTP 400 при пустом тексте
     */
    @PutMapping("/questions/{id}")
    public ResponseEntity<?> updateQuestion(@PathVariable UUID id, @RequestBody UpdateQuestionRequestDto dto) {
        if (dto.getQuestionText() == null || dto.getQuestionText().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "questionText обязателен"));
        }
        return questionBankManagement.updateQuestion(id, dto.getQuestionText())
                .map(questionBankMapper::toDto)
                .map(ResponseEntity::ok)
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
        questionBankManagement.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }
}
