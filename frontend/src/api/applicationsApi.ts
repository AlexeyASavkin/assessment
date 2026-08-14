import { adminJson } from './shared'

/**
 * Краткая сводка по заявке на оценку.
 * Связывает пригласительный токен, сессию и агрегированные метрики результата.
 */
export interface ApplicationSummary {
  /** Идентификатор пригласительного токена. */
  tokenId: string
  /** Идентификатор сотрудника. */
  employeeId: string
  /** ФИО сотрудника. */
  employeeName: string
  /** Наименование компетенции. */
  competencyName: string | null
  /** Статус сессии (ACTIVE, COMPLETED или null, если сессия ещё не создана). */
  sessionStatus: string | null
  /** Идентификатор сессии (null, если сессия ещё не создана). */
  sessionId: string | null
  /** Средний балл по валидным попыткам (null, если оценок нет). */
  averageScore: number | null
  /** Результат прохождения (средний балл >= 3.0). */
  passed: boolean
  /** Дата создания пригласительного токена. */
  createdAt: string | null
  /** Дата завершения сессии (null, если не завершена). */
  completedAt: string | null
}

/**
 * Детальная попытка ответа в отчёте заявки.
 */
export interface AttemptDetail {
  /** Идентификатор попытки. */
  attemptId: string
  /** Текст вопроса. */
  questionText: string
  /** Итоговый отредактированный текст ответа. */
  finalTranscript: string | null
  /** Оценка по шкале 0–5 (текущая, после переоценки с учётом уточнения, если было). */
  score: number | null
  /** Базовая оценка до переоценки с учётом уточняющего ответа. null, если уточнения не было. */
  baseScore: number | null
  /** Уровень уверенности LLM (high, medium, low). */
  confidence: string | null
  /** Флаг валидности оценки. */
  validJudge: boolean | null
  /** Feedback от LLM по ответу. */
  feedback: string | null
  /** Глубина уточняющего вопроса (0 — основной). */
  followupDepth: number
  /** Идентификатор родительской попытки (для уточняющих). */
  followupParentId: string | null
  /** Идентификатор темы. */
  topicId: string | null
  /** Название темы. */
  topicName: string | null
  /** Название раздела. */
  sectionName: string | null
  /** Название компетенции. */
  competencyName: string | null
  /** Дата создания попытки. */
  createdAt: string | null
}

/**
 * Агрегированный отчёт по теме в заявке.
 */
export interface CompetencyReportItem {
  /** Идентификатор темы. */
  topicId: string
  /** Название темы. */
  topicName: string
  /** Название раздела. */
  sectionName: string
  /** Название компетенции. */
  competencyName: string
  /** Средний балл по теме. */
  averageScore: number
  /** Результат прохождения темы (средний балл >= 3.0). */
  passed: boolean
  /** Баллы за уточняющие вопросы. */
  followUpScores: number[]
  /** Feedback от LLM по теме. */
  feedbacks: string[]
}

/**
 * Полный отчёт по заявке для администратора.
 */
export interface ApplicationReport {
  /** Идентификатор сессии. */
  sessionId: string
  /** Идентификатор сотрудника. */
  employeeId: string
  /** ФИО сотрудника. */
  employeeName: string
  /** Наименование компетенции. */
  competencyName: string | null
  /** Статус сессии. */
  sessionStatus: string
  /** Дата создания сессии. */
  createdAt: string | null
  /** Дата обновления сессии. */
  updatedAt: string | null
  /** Агрегированные отчёты по темам. */
  competencies: CompetencyReportItem[]
  /** Итоговый результат прохождения. */
  passed: boolean
  /** Общая рекомендация. */
  overallRecommendation: string
  /** Все попытки ответов с детальной информацией. */
  attempts: AttemptDetail[]
}

/**
 * Возвращает список всех заявок на оценку.
 * @return массив заявок
 */
export async function listApplications(): Promise<ApplicationSummary[]> {
  return adminJson<ApplicationSummary[]>(`/applications`)
}

/**
 * Возвращает детальный отчёт по заявке.
 * @param sessionId - идентификатор сессии
 * @return отчёт с попытками, оценками и рекомендациями
 */
export async function getApplicationReport(sessionId: string): Promise<ApplicationReport> {
  return adminJson<ApplicationReport>(`/applications/${sessionId}/report`)
}
