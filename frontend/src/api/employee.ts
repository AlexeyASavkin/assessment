/**
 * API-клиент для операций сотрудника.
 * Все запросы отправляются с учетными данными (cookie) для аутентификации по сессии.
 */
const API_BASE = '/api'

/**
 * Ответ {@code GET /sessions/{id}/questions}: текущий вопрос сессии или признак завершения.
 *
 * Завершённая сессия: {@code completed=true}, остальные поля отсутствуют.
 * Уточняющий вопрос: {@code isFollowUp=true}, ссылка на родителя в {@code followupParentId}.
 */
export interface QuestionResponse {
  /** Идентификатор попытки ответа (передаётся в submitAnswer). */
  questionId: string
  /** Текст вопроса для отображения сотруднику. */
  questionText: string
  /** Идентификатор темы (для UI-индикаторов прогресса). */
  topicId: string | null
  /** True, если это уточняющий вопрос (depth>0). */
  isFollowUp: boolean
  /** Идентификатор основной попытки, к которой относится уточнение (null для основных). */
  followupParentId: string | null
  /** True, если сессия завершена — дальше отчёт. */
  completed?: boolean
  /** Сообщение об ошибке (legacy). */
  error?: string
}

/**
 * Ответ {@code POST /sessions/{id}/answers}: следующий вопрос или признак завершения.
 */
export interface AnswerResponse {
  /** Идентификатор следующей попытки (null если completed). */
  nextQuestionId: string | null
  /** Текст следующего вопроса (для отображения без доп. fetch'а). */
  nextQuestionText?: string | null
  /** Тема следующего вопроса. */
  topicId?: string | null
  /** True, если следующий вопрос — уточняющий. */
  isFollowUp: boolean
  /** Идентификатор родителя для уточняющего (null для основных). */
  followupParentId?: string | null
  /** True, если сессия завершена. */
  completed: boolean
}

/**
 * Получает текущий вопрос для активной сессии оценки.
 * @param sessionId - идентификатор сессии
 * @return объект с данными вопроса
 */
export async function getCurrentQuestion(sessionId: string): Promise<QuestionResponse> {
  const response = await fetch(`${API_BASE}/employee/sessions/${sessionId}/questions`, {
    credentials: 'include',
  })
  if (!response.ok) throw new Error('Failed to get question')
  return response.json()
}

/**
 * Отправляет ответ сотрудника на текущий вопрос.
 * @param sessionId - идентификатор сессии
 * @param questionAttemptId - идентификатор попытки ответа на вопрос
 * @param finalTranscript - отредактированный финальный текст ответа
 * @return объект с информацией о следующем вопросе или завершении сессии
 */
export async function submitAnswer(
  sessionId: string,
  questionAttemptId: string,
  finalTranscript: string,
): Promise<AnswerResponse> {
  const response = await fetch(`${API_BASE}/employee/sessions/${sessionId}/answers`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ questionAttemptId, finalTranscript }),
  })
  if (!response.ok) throw new Error('Failed to submit answer')
  return response.json()
}

/**
 * Получает итоговый отчет по завершенной сессии оценки.
 * @param sessionId - идентификатор сессии
 * @return объект с отчетом, включающим оценки по критериям и итоговый уровень
 */
export async function getReport(sessionId: string) {
  const response = await fetch(`${API_BASE}/employee/sessions/${sessionId}/report`, {
    credentials: 'include',
  })
  if (!response.ok) throw new Error('Failed to get report')
  return response.json()
}
