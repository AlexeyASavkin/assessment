/**
 * API-клиент для операций сотрудника.
 * Все запросы отправляются с учетными данными (cookie) для аутентификации по сессии.
 */
const API_BASE = '/api'

/**
 * Получает текущий вопрос для активной сессии оценки.
 * @param sessionId - идентификатор сессии
 * @return объект с данными вопроса
 */
export async function getCurrentQuestion(sessionId: string) {
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
export async function submitAnswer(sessionId: string, questionAttemptId: string, finalTranscript: string) {
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
