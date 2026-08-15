import { adminJson } from './shared'

/**
 * Элемент банка вопросов для оценки компетенций.
 */
export interface QuestionBankItem {
  id: string
  competencyId: string
  topicId?: string
  questionText: string
  difficulty: string
  sortOrder: number
  createdAt?: string
  updatedAt?: string
}

/**
 * Генерирует вопросы для компетенции с помощью ИИ.
 * @param competencyId - идентификатор компетенции
 * @param count - количество вопросов
 * @param difficulty - уровень сложности
 * @return массив сгенерированных вопросов
 */
export async function generateQuestions(
  competencyId: string,
  count: number,
  difficulty: string,
): Promise<QuestionBankItem[]> {
  return adminJson<QuestionBankItem[]>(`/competencies/${competencyId}/questions/generate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ count, difficulty }),
  })
}

/**
 * Возвращает список вопросов для компетенции.
 * @param competencyId - идентификатор компетенции
 * @return массив вопросов
 */
export async function listQuestions(competencyId: string): Promise<QuestionBankItem[]> {
  return adminJson<QuestionBankItem[]>(`/competencies/${competencyId}/questions`)
}

/**
 * Генерирует вопросы для темы с помощью ИИ.
 * @param topicId - идентификатор темы
 * @param count - количество вопросов
 * @param difficulty - уровень сложности
 * @return массив сгенерированных вопросов
 */
export async function generateTopicQuestions(
  topicId: string,
  count: number,
  difficulty: string,
): Promise<QuestionBankItem[]> {
  return adminJson<QuestionBankItem[]>(`/topics/${topicId}/questions/generate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ count, difficulty }),
  })
}

/**
 * Возвращает список вопросов для темы.
 * @param topicId - идентификатор темы
 * @return массив вопросов
 */
export async function listTopicQuestions(topicId: string): Promise<QuestionBankItem[]> {
  return adminJson<QuestionBankItem[]>(`/topics/${topicId}/questions`)
}

/**
 * Обновляет текст существующего вопроса.
 * @param id - идентификатор вопроса
 * @param questionText - новый текст вопроса
 * @return обновленный вопрос
 */
export async function updateQuestion(id: string, questionText: string): Promise<QuestionBankItem> {
  return adminJson<QuestionBankItem>(`/questions/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ questionText }),
  })
}

/**
 * Удаляет вопрос по идентификатору.
 * @param id - идентификатор вопроса
 */
export async function deleteQuestion(id: string): Promise<void> {
  await adminJson<void>(`/questions/${id}`, { method: 'DELETE' })
}

/**
 * Изменяет порядок вопросов в теме.
 * @param topicId - идентификатор темы
 * @param orderedIds - массив идентификаторов вопросов в новом порядке
 */
export async function reorderTopicQuestions(topicId: string, orderedIds: string[]): Promise<void> {
  await adminJson<void>(`/topics/${topicId}/questions/reorder`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(orderedIds),
  })
}
