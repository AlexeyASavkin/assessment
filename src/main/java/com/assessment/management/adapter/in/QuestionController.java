package com.assessment.management.adapter.in;

import com.assessment.dto.GenerateQuestionsRequestDto;
import com.assessment.dto.GenerateTopicQuestionsRequestDto;
import com.assessment.dto.QuestionBankItemDto;
import com.assessment.dto.UpdateQuestionRequestDto;
import com.assessment.dto.mapper.QuestionBankMapper;
import com.assessment.management.application.QuestionBankManagementUseCase;
import jakarta.validation.Valid;
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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Тонкий HTTP-адаптер банка вопросов (пути {@code /api/admin/.../questions...} и {@code /api/admin/questions/{id}}).
 * Генерация и чтение вопросов на уровне компетенций и тем, переупорядочивание, обновление и удаление.
 * Требует роль ADMIN, делегирует бизнес-логику {@link QuestionBankManagementUseCase}, маппит DTO через {@link QuestionBankMapper}.
 */
@RestController
@RequestMapping("/api/admin")
public class QuestionController {

    private final QuestionBankManagementUseCase questionBankManagement;
    private final QuestionBankMapper questionBankMapper;

    public QuestionController(QuestionBankManagementUseCase questionBankManagement, QuestionBankMapper questionBankMapper) {
        this.questionBankManagement = questionBankManagement;
        this.questionBankMapper = questionBankMapper;
    }

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
    public ResponseEntity<List<QuestionBankItemDto>> generateQuestions(@PathVariable UUID competencyId,
                                                                       @Valid @RequestBody GenerateQuestionsRequestDto dto) {
        List<QuestionBankItemDto> questions = questionBankManagement
                .generateForCompetency(competencyId, dto.getCount(),
                        dto.getDifficulty() != null ? dto.getDifficulty().getValue() : "ALL")
                .stream().map(questionBankMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(questions);
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
    public ResponseEntity<List<QuestionBankItemDto>> generateTopicQuestions(@PathVariable UUID topicId,
                                                                            @Valid @RequestBody GenerateTopicQuestionsRequestDto dto) {
        List<QuestionBankItemDto> questions = questionBankManagement
                .generateForTopic(topicId, dto.getCount(),
                        dto.getDifficulty() != null ? dto.getDifficulty().getValue() : "ALL")
                .stream().map(questionBankMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(questions);
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
                                                      @Valid @RequestBody List<UUID> orderedIds) {
        questionBankManagement.reorder(topicId, orderedIds);
        return ResponseEntity.noContent().build();
    }

    /**
     * Обновляет текст вопроса в банке вопросов.
     *
     * @param id  идентификатор вопроса
     * @param dto тело запроса с полем {@code questionText}
     * @return обновленный вопрос с HTTP 200, HTTP 404 если не найден, или HTTP 400 при пустом тексте
     */
    @PutMapping("/questions/{id}")
    public ResponseEntity<?> updateQuestion(@PathVariable UUID id, @Valid @RequestBody UpdateQuestionRequestDto dto) {
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