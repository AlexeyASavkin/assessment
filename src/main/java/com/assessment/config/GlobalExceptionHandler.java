package com.assessment.config;

import com.assessment.common.BadRequestException;
import com.assessment.common.ConflictException;
import com.assessment.common.ForbiddenException;
import com.assessment.common.LlmUnavailableException;
import com.assessment.common.NotFoundException;
import com.openai.errors.RateLimitException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Глобальный обработчик исключений для единообразного HTTP-маппинга.
 * <p>
 * Заменяет ad-hoc try/catch в контроллерах единым набором правил:
 * <ul>
 *   <li>{@link NotFoundException} → 404</li>
 *   <li>{@link ForbiddenException} → 403</li>
 *   <li>{@link ConflictException} → 409</li>
 *   <li>{@link BadRequestException} → 400</li>
 *   <li>{@link LlmUnavailableException} → 503</li>
 *   <li>Legacy {@link NoSuchElementException} → 404</li>
 *   <li>Legacy {@link IllegalArgumentException} → 400</li>
 *   <li>Legacy {@link IllegalStateException} → 503 (сервис временно недоступен)</li>
 *   <li>{@link RequestNotPermitted} (Resilience4j) → 429 + Retry-After</li>
 *   <li>{@link RateLimitException} (429 от LLM-провайдера) → 429 + Retry-After</li>
 *   <li>{@link MethodArgumentNotValidException} (Bean Validation) → 400 с полями</li>
 *   <li>{@link DataIntegrityViolationException} → 409</li>
 * </ul>
 * Внутренняя ошибка (непредвиденный {@link RuntimeException}) → 500 с обезличенным сообщением,
 * полный стектрейс пишется в лог.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Формат ошибки: {@code {"error": "сообщение"}}.
     */
    private static ResponseEntity<Object> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFound(NotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Object> handleForbidden(ForbiddenException e) {
        return error(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflict(ConflictException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequest(BadRequestException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(LlmUnavailableException.class)
    public ResponseEntity<Object> handleLlmUnavailable(LlmUnavailableException e) {
        log.warn("LLM недоступен: {}", e.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE, "Сервис оценки временно недоступен. Попробуйте позже.");
    }

    /**
     * Legacy-маппинг: use case'ы, которые ещё не переведены на {@link NotFoundException}.
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Object> handleLegacyNotFound(NoSuchElementException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * IllegalStateException трактуем как «сервис временно недоступен»:
     * неверная конфигурация провайдера, ChatModel не настроен и т.п.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalState(IllegalStateException e) {
        log.warn("Illegal state: {}", e.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    /**
     * Rate limiter (Resilience4j) — HTTP 429 с Retry-After.
     */
    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<Object> handleRateLimit(RequestNotPermitted e) {
        log.warn("Rate limit exceeded: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", "60")
                .body(new ErrorResponse("Превышен лимит запросов к LLM. Попробуйте через минуту."));
    }

    /**
     * 429 от самого LLM-провайдера (OpenAI SDK) — HTTP 429 с Retry-After,
     * как и при собственном rate limit'е (Resilience4j).
     */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Object> handleProviderRateLimit(RateLimitException e) {
        log.warn("Rate limit от LLM-провайдера: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", "60")
                .body(new ErrorResponse("Превышен лимит запросов к LLM. Попробуйте через минуту."));
    }

    /**
     * Bean Validation: {@code @Valid} не прошёл на @RequestBody.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException e) {
        String details = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Некорректный запрос: " + details));
    }

    /**
     * Bean Validation на параметрах (path/query/live).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraint(ConstraintViolationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Некорректный запрос: " + e.getMessage()));
    }

    /**
     * Не совпал тип path-параметра (например, UUID malformed) —> 400 вместо 500.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Некорректный параметр: " + e.getName()));
    }

    /**
     * DB constraint violation (UNIQUE, FK) — трактуем как конфликт состояния.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}", e.getMostSpecificCause().getMessage());
        return error(HttpStatus.CONFLICT, "Нарушение ограничения целостности данных.");
    }

    /**
     * Catch-all для непредвиденных ошибок: логируем полный стектрейс, клиенту отдаём
     * обезличенное сообщение без деталей реализации.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleUnexpected(RuntimeException e) {
        // Не логируем как warn NotFoundException/Forbidden — они уже перехвачены выше;
        // сюда попадают действительно непредвиденные ошибки.
        log.error("Непредвиденная ошибка", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера.");
    }

    /**
     * Унифицированное тело ответа с ошибкой.
     */
    public record ErrorResponse(String error) {}
}