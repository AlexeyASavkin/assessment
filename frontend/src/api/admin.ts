const API_BASE = '/api/admin'

export type LevelValue = 'JUNIOR' | 'MIDDLE' | 'SENIOR'

export interface Competency {
  id: string
  name: string
  description: string
}

export interface Section {
  id: string
  name: string
  description: string
  sortOrder: number
}

export interface Topic {
  id: string
  name: string
  description: string
  sortOrder: number
  weight: number
}

export interface Criterion {
  id: string
  name: string
  description: string
  weight: number
}

export interface CriteriaLevel {
  id: string
  level: LevelValue
  requirements: string
}

export interface Employee {
  id: string
  fullName: string
  position: string
  department: string
  competency?: Competency
}

export interface InviteToken {
  id: string
  token: string
  employeeId: string
  used: boolean
  createdAt?: string
  expiresAt?: string
}

async function parseError(response: Response): Promise<string> {
  try {
    const text = await response.text()
    return text || `Request failed with status ${response.status}`
  } catch {
    return `Request failed with status ${response.status}`
  }
}

async function adminFetch(path: string, init?: RequestInit): Promise<Response> {
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    credentials: 'include',
    redirect: 'manual',
  })
  return response
}

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

export async function checkAuth(): Promise<boolean> {
  try {
    const response = await adminFetch('/competencies', { method: 'GET' })
    return response.ok && response.type !== 'opaqueredirect'
  } catch {
    return false
  }
}

// ---- Competencies ----

export async function listCompetencies(): Promise<Competency[]> {
  return adminJson<Competency[]>('/competencies')
}

export async function getCompetency(id: string): Promise<Competency> {
  return adminJson<Competency>(`/competencies/${id}`)
}

export async function createCompetency(name: string, description: string): Promise<Competency> {
  return adminJson<Competency>('/competencies', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, description }),
  })
}

export async function updateCompetency(id: string, name: string, description: string): Promise<Competency> {
  return adminJson<Competency>(`/competencies/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, description }),
  })
}

export async function deleteCompetency(id: string): Promise<void> {
  await adminJson<void>(`/competencies/${id}`, { method: 'DELETE' })
}

// ---- Criteria ----

export async function listCriteria(competencyId: string): Promise<Criterion[]> {
  return adminJson<Criterion[]>(`/competencies/${competencyId}/criteria`)
}

export async function createCriterion(competencyId: string, name: string, description: string, weight: number): Promise<Criterion> {
  return adminJson<Criterion>(`/competencies/${competencyId}/criteria`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, description, weight }),
  })
}

export async function updateCriterion(id: string, name: string, description: string, weight: number): Promise<Criterion> {
  return adminJson<Criterion>(`/criteria/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, description, weight }),
  })
}

export async function deleteCriterion(id: string): Promise<void> {
  await adminJson<void>(`/criteria/${id}`, { method: 'DELETE' })
}

// ---- Sections ----

export async function listSections(competencyId: string): Promise<Section[]> {
  return adminJson<Section[]>(`/competencies/${competencyId}/sections`)
}

export async function createSection(competencyId: string, name: string, sortOrder: number): Promise<Section> {
  return adminJson<Section>(`/competencies/${competencyId}/sections`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, sortOrder }),
  })
}

export async function updateSection(id: string, name: string, sortOrder: number): Promise<Section> {
  return adminJson<Section>(`/sections/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, sortOrder }),
  })
}

export async function deleteSection(id: string): Promise<void> {
  await adminJson<void>(`/sections/${id}`, { method: 'DELETE' })
}

// ---- Topics ----

export async function listTopics(sectionId: string): Promise<Topic[]> {
  return adminJson<Topic[]>(`/sections/${sectionId}/topics`)
}

export async function createTopic(sectionId: string, name: string, weight: number, sortOrder: number): Promise<Topic> {
  return adminJson<Topic>(`/sections/${sectionId}/topics`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, weight, sortOrder }),
  })
}

export async function updateTopic(id: string, name: string, weight: number, sortOrder: number): Promise<Topic> {
  return adminJson<Topic>(`/topics/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, weight, sortOrder }),
  })
}

export async function deleteTopic(id: string): Promise<void> {
  await adminJson<void>(`/topics/${id}`, { method: 'DELETE' })
}

// ---- Legacy Criteria/Levels (保留向后兼容) ----

export async function listLevels(criteriaId: string): Promise<CriteriaLevel[]> {
  return adminJson<CriteriaLevel[]>(`/criteria/${criteriaId}/levels`)
}

export async function createLevel(criteriaId: string, level: LevelValue, requirements: string): Promise<CriteriaLevel> {
  return adminJson<CriteriaLevel>(`/criteria/${criteriaId}/levels`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ level, requirements }),
  })
}

export async function updateLevel(id: string, level: LevelValue, requirements: string): Promise<CriteriaLevel> {
  return adminJson<CriteriaLevel>(`/criteria/levels/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ level, requirements }),
  })
}

export async function deleteLevel(id: string): Promise<void> {
  await adminJson<void>(`/criteria/levels/${id}`, { method: 'DELETE' })
}

// ---- Employees ----

export async function listEmployees(): Promise<Employee[]> {
  return adminJson<Employee[]>('/employees')
}

export async function getEmployee(id: string): Promise<Employee> {
  return adminJson<Employee>(`/employees/${id}`)
}

export async function createEmployee(fullName: string, position: string, department: string, competencyId: string | null): Promise<Employee> {
  const competency = competencyId ? { id: competencyId } : null
  return adminJson<Employee>('/employees', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fullName, position, department, competency }),
  })
}

export async function updateEmployee(id: string, fullName: string, position: string, department: string, competencyId: string | null): Promise<Employee> {
  const competency = competencyId ? { id: competencyId } : null
  return adminJson<Employee>(`/employees/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fullName, position, department, competency }),
  })
}

export async function deleteEmployee(id: string): Promise<void> {
  await adminJson<void>(`/employees/${id}`, { method: 'DELETE' })
}

// ---- Invite tokens ----

export async function generateInvite(employeeId: string): Promise<string> {
  const response = await adminFetch(`/employees/${employeeId}/invite`, { method: 'POST' })
  if (response.type === 'opaqueredirect') throw new Error('Не авторизован')
  if (!response.ok) throw new Error(await parseError(response))
  // Endpoint returns a plain string like "/api/employee/invite/{token}"
  return (await response.text()).trim().replace(/^"|"$/g, '')
}

export async function listTokens(): Promise<InviteToken[]> {
  return adminJson<InviteToken[]>('/tokens')
}

// ---- AI Settings ----

export interface AiSettings {
  activeProvider: string
  availableProviders: string[]
}

export async function getAiSettings(): Promise<AiSettings> {
  return adminJson<AiSettings>('/settings/ai')
}

export async function updateAiSettings(activeProvider: string): Promise<AiSettings> {
  return adminJson<AiSettings>('/settings/ai', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ activeProvider }),
  })
}

export interface AiKeys {
  geminiApiKey: string
  gigachatApiKey: string
}

export async function getAiKeys(): Promise<AiKeys> {
  return adminJson<AiKeys>('/settings/ai/keys')
}

export async function updateAiKeys(keys: AiKeys): Promise<AiKeys> {
  return adminJson<AiKeys>('/settings/ai/keys', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(keys),
  })
}

// ---- Question Bank ----

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

export async function generateQuestions(competencyId: string, count: number, difficulty: string): Promise<QuestionBankItem[]> {
  return adminJson<QuestionBankItem[]>(`/competencies/${competencyId}/questions/generate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ count, difficulty }),
  })
}

export async function listQuestions(competencyId: string): Promise<QuestionBankItem[]> {
  return adminJson<QuestionBankItem[]>(`/competencies/${competencyId}/questions`)
}

export async function generateTopicQuestions(topicId: string, count: number, difficulty: string): Promise<QuestionBankItem[]> {
  return adminJson<QuestionBankItem[]>(`/topics/${topicId}/questions/generate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ count, difficulty }),
  })
}

export async function listTopicQuestions(topicId: string): Promise<QuestionBankItem[]> {
  return adminJson<QuestionBankItem[]>(`/topics/${topicId}/questions`)
}

export async function updateQuestion(id: string, questionText: string): Promise<QuestionBankItem> {
  return adminJson<QuestionBankItem>(`/questions/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ questionText }),
  })
}

export async function deleteQuestion(id: string): Promise<void> {
  await adminJson<void>(`/questions/${id}`, { method: 'DELETE' })
}

export async function reorderTopicQuestions(topicId: string, orderedIds: string[]): Promise<void> {
  await adminJson<void>(`/topics/${topicId}/questions/reorder`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(orderedIds),
  })
}