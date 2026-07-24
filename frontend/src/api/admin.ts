/**
 * Базовый путь для административного API.
 */
const API_BASE = '/api/admin'

/**
 * Возможные уровни компетенций сотрудника.
 */
export type LevelValue = 'JUNIOR' | 'MIDDLE' | 'SENIOR'

/**
 * Компетенция — область знаний или навыков для оценки.
 */
export interface Competency {
  id: string
  name: string
  description: string
}

/**
 * Раздел внутри компетенции.
 */
export interface Section {
  id: string
  name: string
  description: string
  sortOrder: number
}

/**
 * Тема внутри раздела компетенции.
 */
export interface Topic {
  id: string
  name: string
  description: string
  sortOrder: number
  weight: number
}

/**
 * Критерий оценки внутри компетенции.
 */
export interface Criterion {
  id: string
  name: string
  description: string
  weight: number
}

/**
 * Уровень требований для критерия оценки.
 */
export interface CriteriaLevel {
  id: string
  level: LevelValue
  requirements: string
}

/**
 * Сотрудник, проходящий оценку компетенций.
 */
export interface Employee {
  id: string
  fullName: string
  position: string
  department: string
  competency?: Competency
}

/**
 * Одноразовый пригласительный токен для сотрудника.
 */
export interface InviteToken {
  id: string
  token: string
  employeeId: string
  used: boolean
  createdAt?: string
  expiresAt?: string
}

/**
 * Извлекает текст ошибки из HTTP-ответа.
 * @param response - HTTP-ответ
 * @return текст ошибки или сообщение со статусом
 */
async function parseError(response: Response): Promise<string> {
  try {
    const text = await response.text()
    return text || `Request failed with status ${response.status}`
  } catch {
    return `Request failed with status ${response.status}`
  }
}

/**
 * Выполняет fetch-запрос к административному API с передачей cookie.
 * @param path - относительный путь
 * @param init - дополнительные параметры fetch
 * @return HTTP-ответ
 */
async function adminFetch(path: string, init?: RequestInit): Promise<Response> {
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    credentials: 'include',
    redirect: 'manual',
  })
  return response
}

/**
 * Выполняет JSON-запрос к административному API и парсит ответ.
 * @param path - относительный путь
 * @param init - дополнительные параметры fetch
 * @return распарсенный JSON-ответ
 * @throws Error при неавторизованном доступе или ошибке сервера
 */
async function adminJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await adminFetch(path, init)
  if (response.type === 'opaqueredirect') {
    throw new Error('Не авторизован')
  }
  if (!response.ok) throw new Error(await parseError(response))
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

// ---- Auth ----

/**
 * Аутентифицирует администратора по логину и паролю.
 * @param username - имя пользователя
 * @param password - пароль
 * @throws Error при неверных учетных данных или ошибке сервера
 */
export async function adminLogin(username: string, password: string): Promise<void> {
  const body = new URLSearchParams()
  body.append('username', username)
  body.append('password', password)
  const response = await fetch(`${API_BASE}/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    credentials: 'include',
    body: body.toString(),
  })
  if (response.ok) {
    return
  }
  if (response.status === 401 || response.status === 403) {
    throw new Error('Неверный логин или пароль')
  }
  throw new Error('Ошибка входа: ' + response.status)
}

/**
 * Проверяет, авторизован ли текущий администратор.
 * @return true если сессия активна, иначе false
 */
export async function checkAuth(): Promise<boolean> {
  try {
    const response = await adminFetch('/competencies', { method: 'GET' })
    return response.ok && response.type !== 'opaqueredirect'
  } catch {
    return false
  }
}

// ---- Competencies ----

/**
 * Возвращает список всех компетенций.
 * @return массив компетенций
 */
export async function listCompetencies(): Promise<Competency[]> {
  return adminJson<Competency[]>('/competencies')
}

/**
 * Получает компетенцию по идентификатору.
 * @param id - идентификатор компетенции
 * @return объект компетенции
 */
export async function getCompetency(id: string): Promise<Competency> {
  return adminJson<Competency>(`/competencies/${id}`)
}

/**
 * Создает новую компетенцию.
 * @param name - название компетенции
 * @param description - описание компетенции
 * @return созданная компетенция
 */
export async function createCompetency(name: string, description: string): Promise<Competency> {
  return adminJson<Competency>('/competencies', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, description }),
  })
}

/**
 * Обновляет существующую компетенцию.
 * @param id - идентификатор компетенции
 * @param name - новое название
 * @param description - новое описание
 * @return обновленная компетенция
 */
export async function updateCompetency(id: string, name: string, description: string): Promise<Competency> {
  return adminJson<Competency>(`/competencies/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, description }),
  })
}

/**
 * Удаляет компетенцию по идентификатору.
 * @param id - идентификатор компетенции
 */
export async function deleteCompetency(id: string): Promise<void> {
  await adminJson<void>(`/competencies/${id}`, { method: 'DELETE' })
}

// ---- Criteria ----

/**
 * Возвращает список критериев для указанной компетенции.
 * @param competencyId - идентификатор компетенции
 * @return массив критериев
 */
export async function listCriteria(competencyId: string): Promise<Criterion[]> {
  return adminJson<Criterion[]>(`/competencies/${competencyId}/criteria`)
}

/**
 * Создает новый критерий внутри компетенции.
 * @param competencyId - идентификатор компетенции
 * @param name - название критерия
 * @param description - описание критерия
 * @param weight - вес критерия
 * @return созданный критерий
 */
export async function createCriterion(competencyId: string, name: string, description: string, weight: number): Promise<Criterion> {
  return adminJson<Criterion>(`/competencies/${competencyId}/criteria`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, description, weight }),
  })
}

/**
 * Обновляет существующий критерий.
 * @param id - идентификатор критерия
 * @param name - новое название
 * @param description - новое описание
 * @param weight - новый вес
 * @return обновленный критерий
 */
export async function updateCriterion(id: string, name: string, description: string, weight: number): Promise<Criterion> {
  return adminJson<Criterion>(`/criteria/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, description, weight }),
  })
}

/**
 * Удаляет критерий по идентификатору.
 * @param id - идентификатор критерия
 */
export async function deleteCriterion(id: string): Promise<void> {
  await adminJson<void>(`/criteria/${id}`, { method: 'DELETE' })
}

// ---- Sections ----

/**
 * Возвращает список разделов для указанной компетенции.
 * @param competencyId - идентификатор компетенции
 * @return массив разделов
 */
export async function listSections(competencyId: string): Promise<Section[]> {
  return adminJson<Section[]>(`/competencies/${competencyId}/sections`)
}

/**
 * Создает новый раздел внутри компетенции.
 * @param competencyId - идентификатор компетенции
 * @param name - название раздела
 * @param sortOrder - порядок сортировки
 * @return созданный раздел
 */
export async function createSection(competencyId: string, name: string, sortOrder: number): Promise<Section> {
  return adminJson<Section>(`/competencies/${competencyId}/sections`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, sortOrder }),
  })
}

/**
 * Обновляет существующий раздел.
 * @param id - идентификатор раздела
 * @param name - новое название
 * @param sortOrder - новый порядок сортировки
 * @return обновленный раздел
 */
export async function updateSection(id: string, name: string, sortOrder: number): Promise<Section> {
  return adminJson<Section>(`/sections/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, sortOrder }),
  })
}

/**
 * Удаляет раздел по идентификатору.
 * @param id - идентификатор раздела
 */
export async function deleteSection(id: string): Promise<void> {
  await adminJson<void>(`/sections/${id}`, { method: 'DELETE' })
}

// ---- Topics ----

/**
 * Возвращает список тем для указанного раздела.
 * @param sectionId - идентификатор раздела
 * @return массив тем
 */
export async function listTopics(sectionId: string): Promise<Topic[]> {
  return adminJson<Topic[]>(`/sections/${sectionId}/topics`)
}

/**
 * Создает новую тему внутри раздела.
 * @param sectionId - идентификатор раздела
 * @param name - название темы
 * @param weight - вес темы
 * @param sortOrder - порядок сортировки
 * @return созданная тема
 */
export async function createTopic(sectionId: string, name: string, weight: number, sortOrder: number): Promise<Topic> {
  return adminJson<Topic>(`/sections/${sectionId}/topics`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, weight, sortOrder }),
  })
}

/**
 * Обновляет существующую тему.
 * @param id - идентификатор темы
 * @param name - новое название
 * @param weight - новый вес
 * @param sortOrder - новый порядок сортировки
 * @return обновленная тема
 */
export async function updateTopic(id: string, name: string, weight: number, sortOrder: number): Promise<Topic> {
  return adminJson<Topic>(`/topics/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, weight, sortOrder }),
  })
}

/**
 * Удаляет тему по идентификатору.
 * @param id - идентификатор темы
 */
export async function deleteTopic(id: string): Promise<void> {
  await adminJson<void>(`/topics/${id}`, { method: 'DELETE' })
}

// ---- Legacy Criteria/Levels (保留向后兼容) ----

/**
 * Возвращает список уровней требований для критерия.
 * @param criteriaId - идентификатор критерия
 * @return массив уровней требований
 */
export async function listLevels(criteriaId: string): Promise<CriteriaLevel[]> {
  return adminJson<CriteriaLevel[]>(`/criteria/${criteriaId}/levels`)
}

/**
 * Создает новый уровень требований для критерия.
 * @param criteriaId - идентификатор критерия
 * @param level - уровень (JUNIOR, MIDDLE, SENIOR)
 * @param requirements - описание требований
 * @return созданный уровень требований
 */
export async function createLevel(criteriaId: string, level: LevelValue, requirements: string): Promise<CriteriaLevel> {
  return adminJson<CriteriaLevel>(`/criteria/${criteriaId}/levels`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ level, requirements }),
  })
}

/**
 * Обновляет существующий уровень требований.
 * @param id - идентификатор уровня
 * @param level - новый уровень
 * @param requirements - новое описание требований
 * @return обновленный уровень требований
 */
export async function updateLevel(id: string, level: LevelValue, requirements: string): Promise<CriteriaLevel> {
  return adminJson<CriteriaLevel>(`/criteria/levels/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ level, requirements }),
  })
}

/**
 * Удаляет уровень требований по идентификатору.
 * @param id - идентификатор уровня
 */
export async function deleteLevel(id: string): Promise<void> {
  await adminJson<void>(`/criteria/levels/${id}`, { method: 'DELETE' })
}

// ---- Employees ----

/**
 * Возвращает список всех сотрудников.
 * @return массив сотрудников
 */
export async function listEmployees(): Promise<Employee[]> {
  return adminJson<Employee[]>('/employees')
}

/**
 * Получает сотрудника по идентификатору.
 * @param id - идентификатор сотрудника
 * @return объект сотрудника
 */
export async function getEmployee(id: string): Promise<Employee> {
  return adminJson<Employee>(`/employees/${id}`)
}

/**
 * Создает нового сотрудника.
 * @param fullName - полное имя
 * @param position - должность
 * @param department - отдел
 * @param competencyId - идентификатор компетенции (может быть null)
 * @return созданный сотрудник
 */
export async function createEmployee(fullName: string, position: string, department: string, competencyId: string | null): Promise<Employee> {
  const competency = competencyId ? { id: competencyId } : null
  return adminJson<Employee>('/employees', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fullName, position, department, competency }),
  })
}

/**
 * Обновляет данные сотрудника.
 * @param id - идентификатор сотрудника
 * @param fullName - полное имя
 * @param position - должность
 * @param department - отдел
 * @param competencyId - идентификатор компетенции (может быть null)
 * @return обновленный сотрудник
 */
export async function updateEmployee(id: string, fullName: string, position: string, department: string, competencyId: string | null): Promise<Employee> {
  const competency = competencyId ? { id: competencyId } : null
  return adminJson<Employee>(`/employees/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fullName, position, department, competency }),
  })
}

/**
 * Удаляет сотрудника по идентификатору.
 * @param id - идентификатор сотрудника
 */
export async function deleteEmployee(id: string): Promise<void> {
  await adminJson<void>(`/employees/${id}`, { method: 'DELETE' })
}

// ---- Invite tokens ----

/**
 * Генерирует пригласительную ссылку для сотрудника.
 * @param employeeId - идентификатор сотрудника
 * @return строка с пригласительной ссылкой
 * @throws Error при неавторизованном доступе или ошибке сервера
 */
export async function generateInvite(employeeId: string): Promise<string> {
  const response = await adminFetch(`/employees/${employeeId}/invite`, { method: 'POST' })
  if (response.type === 'opaqueredirect') throw new Error('Не авторизован')
  if (!response.ok) throw new Error(await parseError(response))
  // Endpoint returns a plain string like "/api/employee/invite/{token}"
  return (await response.text()).trim().replace(/^"|"$/g, '')
}

/**
 * Возвращает список всех пригласительных токенов.
 * @return массив токенов
 */
export async function listTokens(): Promise<InviteToken[]> {
  return adminJson<InviteToken[]>('/tokens')
}

// ---- AI Settings ----

/**
 * Настройки провайдера искусственного интеллекта.
 */
export interface AiSettings {
  activeProvider: string
  availableProviders: string[]
}

/**
 * Получает текущие настройки ИИ.
 * @return объект с активным провайдером и списком доступных
 */
export async function getAiSettings(): Promise<AiSettings> {
  return adminJson<AiSettings>('/settings/ai')
}

/**
 * Обновляет активного провайдера ИИ.
 * @param activeProvider - идентификатор провайдера (например, 'gemini' или 'gigachat')
 * @return обновленные настройки
 */
export async function updateAiSettings(activeProvider: string): Promise<AiSettings> {
  return adminJson<AiSettings>('/settings/ai', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ activeProvider }),
  })
}

/**
 * API-ключи для провайдеров искусственного интеллекта.
 */
export interface AiKeys {
  geminiApiKey: string
  gigachatApiKey: string
}

/**
 * Получает сохраненные API-ключи для ИИ-провайдеров.
 * @return объект с ключами Gemini и GigaChat
 */
export async function getAiKeys(): Promise<AiKeys> {
  return adminJson<AiKeys>('/settings/ai/keys')
}

/**
 * Обновляет API-ключи для ИИ-провайдеров.
 * @param keys - объект с новыми ключами
 * @return обновленные ключи
 */
export async function updateAiKeys(keys: AiKeys): Promise<AiKeys> {
  return adminJson<AiKeys>('/settings/ai/keys', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(keys),
  })
}

// ---- Question Bank ----

/**
 * Элемент банка вопросов для оценки компетенций.
 */
export interface QuestionBankItem {
  id: string
  competencyId: string
  criteriaId?: string
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
export async function generateQuestions(competencyId: string, count: number, difficulty: string): Promise<QuestionBankItem[]> {
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
export async function generateTopicQuestions(topicId: string, count: number, difficulty: string): Promise<QuestionBankItem[]> {
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