import { HttpResponse, http, type JsonBodyType } from 'msw'
import type { Competency, Employee, InviteToken, Section, Topic } from '../api/shared'

// ---- In-memory data store -------------------------------------------------

let competencySeq = 1
const competencies: Competency[] = [
  { id: 'comp-1', name: 'Java', description: 'Оценка знаний Java' },
  { id: 'comp-2', name: 'SQL', description: 'Оценка знаний SQL' },
]

let sectionSeq = 1
const sections: Section[] = [
  { id: 'sec-1', name: 'Java Core', description: 'Базовые концепции Java', sortOrder: 1 },
  { id: 'sec-2', name: 'Java EE', description: 'Корпоративные технологии', sortOrder: 2 },
]

let topicSeq = 1
const topics: Topic[] = [
  { id: 'top-1', name: 'Stream API', description: 'Потоки', sortOrder: 1, weight: 5 },
  { id: 'top-2', name: 'Коллекции', description: 'Collections', sortOrder: 2, weight: 4 },
]

let employeeSeq = 1
const employees: Employee[] = [
  {
    id: 'emp-1',
    fullName: 'Иванов Иван',
    position: 'Разработчик',
    department: 'Разработка',
    competency: competencies[0],
  },
]

const tokens: InviteToken[] = [
  {
    id: 'tok-1',
    token: 'tok-seed-1',
    employeeId: 'emp-1',
    used: false,
    createdAt: '2026-08-01T10:00:00',
    expiresAt: '2026-08-04T10:00:00',
  },
]

let aiActiveProvider = 'gemini'
const aiAvailableProviders = ['gemini', 'gigachat', 'openrouter', 'opencode', 'stub']
const aiPrompts = {
  prompt_scoring: 'Оцени ответ',
  prompt_question: 'Составь вопрос',
  prompt_followup: 'Уточни',
  prompt_rescore: 'Переоцени',
  prompt_followup_system: 'Система уточнения',
  prompt_rescore_system: 'Система переоценки',
}

// ---- Helpers --------------------------------------------------------------

function json(data: unknown, status = 200) {
  return HttpResponse.json(data as JsonBodyType, { status })
}

function param(params: URLSearchParams, key: string): string {
  return params.get(key) ?? ''
}

// ---- Handlers -------------------------------------------------------------

export const handlers = [
  // ---- Auth ----
  http.post('/api/admin/login', ({ request }) => {
    void request
    return json({ message: 'ok' })
  }),

  // ---- Competencies ----
  http.get('/api/admin/competencies', () => json(competencies)),
  http.get('/api/admin/competencies/:id', ({ params }) => {
    const found = competencies.find((c) => c.id === params.id)
    if (!found) return json({ message: 'Не найдено' }, 404)
    return json(found)
  }),
  http.post('/api/admin/competencies', async ({ request }) => {
    const body = (await request.json()) as { name: string; description: string }
    const created: Competency = {
      id: `comp-${++competencySeq}`,
      name: body.name,
      description: body.description,
    }
    competencies.push(created)
    return json(created, 201)
  }),
  http.put('/api/admin/competencies/:id', async ({ request, params }) => {
    const found = competencies.find((c) => c.id === params.id)
    if (!found) return json({ message: 'Не найдено' }, 404)
    const body = (await request.json()) as { name: string; description: string }
    found.name = body.name
    found.description = body.description
    return json(found)
  }),
  http.delete('/api/admin/competencies/:id', ({ params }) => {
    const idx = competencies.findIndex((c) => c.id === params.id)
    if (idx === -1) return json({ message: 'Не найдено' }, 404)
    competencies.splice(idx, 1)
    return json(null, 204)
  }),

  // ---- Sections ----
  http.get('/api/admin/competencies/:competencyId/sections', () => json(sections)),
  http.post('/api/admin/competencies/:competencyId/sections', async ({ request }) => {
    const body = (await request.json()) as { name: string; sortOrder: number }
    const created: Section = {
      id: `sec-${++sectionSeq}`,
      name: body.name,
      description: '',
      sortOrder: body.sortOrder,
    }
    sections.push(created)
    return json(created, 201)
  }),
  http.put('/api/admin/sections/:id', async ({ request, params }) => {
    const found = sections.find((s) => s.id === params.id)
    if (!found) return json({ message: 'Не найдено' }, 404)
    const body = (await request.json()) as { name: string; sortOrder: number }
    found.name = body.name
    found.sortOrder = body.sortOrder
    return json(found)
  }),
  http.delete('/api/admin/sections/:id', ({ params }) => {
    const idx = sections.findIndex((s) => s.id === params.id)
    if (idx === -1) return json({ message: 'Не найдено' }, 404)
    sections.splice(idx, 1)
    return json(null, 204)
  }),

  // ---- Topics ----
  http.get('/api/admin/sections/:sectionId/topics', () => json(topics)),
  http.post('/api/admin/sections/:sectionId/topics', async ({ request }) => {
    const body = (await request.json()) as { name: string; weight: number; sortOrder: number }
    const created: Topic = {
      id: `top-${++topicSeq}`,
      name: body.name,
      description: '',
      weight: body.weight,
      sortOrder: body.sortOrder,
    }
    topics.push(created)
    return json(created, 201)
  }),
  http.put('/api/admin/topics/:id', async ({ request, params }) => {
    const found = topics.find((t) => t.id === params.id)
    if (!found) return json({ message: 'Не найдено' }, 404)
    const body = (await request.json()) as { name: string; weight: number; sortOrder: number }
    found.name = body.name
    found.weight = body.weight
    found.sortOrder = body.sortOrder
    return json(found)
  }),
  http.delete('/api/admin/topics/:id', ({ params }) => {
    const idx = topics.findIndex((t) => t.id === params.id)
    if (idx === -1) return json({ message: 'Не найдено' }, 404)
    topics.splice(idx, 1)
    return json(null, 204)
  }),

  // ---- Employees ----
  http.get('/api/admin/employees', () => json(employees)),
  http.get('/api/admin/employees/:id', ({ params }) => {
    const found = employees.find((e) => e.id === params.id)
    if (!found) return json({ message: 'Не найдено' }, 404)
    return json(found)
  }),
  http.post('/api/admin/employees', async ({ request }) => {
    const body = (await request.json()) as {
      fullName: string
      position: string
      department: string
      competencyId: string | null
    }
    const competency = competencies.find((c) => c.id === body.competencyId)
    const created: Employee = {
      id: `emp-${++employeeSeq}`,
      fullName: body.fullName,
      position: body.position,
      department: body.department,
      competency,
    }
    employees.push(created)
    return json(created, 201)
  }),
  http.put('/api/admin/employees/:id', async ({ request, params }) => {
    const found = employees.find((e) => e.id === params.id)
    if (!found) return json({ message: 'Не найдено' }, 404)
    const body = (await request.json()) as {
      fullName: string
      position: string
      department: string
      competencyId: string | null
    }
    found.fullName = body.fullName
    found.position = body.position
    found.department = body.department
    found.competency = competencies.find((c) => c.id === body.competencyId)
    return json(found)
  }),
  http.delete('/api/admin/employees/:id', ({ params }) => {
    const idx = employees.findIndex((e) => e.id === params.id)
    if (idx === -1) return json({ message: 'Не найдено' }, 404)
    employees.splice(idx, 1)
    return json(null, 204)
  }),

  // ---- Invite tokens ----
  http.post('/api/admin/employees/:employeeId/invite', ({ params }) => {
    void params
    return new HttpResponse('/api/employee/invite/tok-new-1', { status: 200 })
  }),
  http.get('/api/admin/tokens', () => json(tokens)),

  // ---- AI Settings ----
  http.get('/api/admin/settings/ai', () =>
    json({ activeProvider: aiActiveProvider, availableProviders: aiAvailableProviders }),
  ),
  http.put('/api/admin/settings/ai', async ({ request }) => {
    const body = (await request.json()) as { activeProvider: string }
    if (aiAvailableProviders.includes(body.activeProvider)) {
      aiActiveProvider = body.activeProvider
    }
    return json({ activeProvider: aiActiveProvider, availableProviders: aiAvailableProviders })
  }),
  http.get('/api/admin/settings/ai/prompts', () => json(aiPrompts)),
  http.put('/api/admin/settings/ai/prompts', async ({ request }) => {
    const body = (await request.json()) as typeof aiPrompts
    Object.assign(aiPrompts, body)
    return json(aiPrompts)
  }),

  // ---- Question Bank ----
  http.get('/api/admin/competencies/:competencyId/questions', () => json([])),
  http.post('/api/admin/competencies/:competencyId/questions/generate', async ({ request }) => {
    const body = (await request.json()) as { count: number; difficulty: string }
    return json(
      Array.from({ length: body.count }, (_, i) => ({
        id: `q-${i + 1}`,
        competencyId: 'comp-1',
        questionText: `Вопрос ${i + 1}`,
        difficulty: body.difficulty,
        sortOrder: i + 1,
      })),
    )
  }),
  http.get('/api/admin/topics/:topicId/questions', () => json([])),
  http.post('/api/admin/topics/:topicId/questions/generate', async ({ request }) => {
    const body = (await request.json()) as { count: number; difficulty: string }
    return json(
      Array.from({ length: body.count }, (_, i) => ({
        id: `qt-${i + 1}`,
        competencyId: 'comp-1',
        topicId: 'top-1',
        questionText: `Вопрос темы ${i + 1}`,
        difficulty: body.difficulty,
        sortOrder: i + 1,
      })),
    )
  }),
  http.put('/api/admin/questions/:id', async ({ request, params }) => {
    const body = (await request.json()) as { questionText: string }
    return json({
      id: params.id,
      competencyId: 'comp-1',
      questionText: body.questionText,
      difficulty: 'easy',
      sortOrder: 1,
    })
  }),
  http.delete('/api/admin/questions/:id', () => json(null, 204)),
  http.put('/api/admin/topics/:topicId/questions/reorder', async ({ request }) => {
    await request.json()
    return json(null, 204)
  }),

  // ---- Applications ----
  http.get('/api/admin/applications', () => json([])),
  http.get('/api/admin/applications/:sessionId/report', () =>
    json({
      sessionId: 'sess-1',
      employeeId: 'emp-1',
      employeeName: 'Иванов Иван',
      competencyName: 'Java',
      sessionStatus: 'COMPLETED',
      createdAt: '2026-08-01T10:00:00',
      updatedAt: '2026-08-01T11:00:00',
      competencies: [],
      passed: true,
      overallRecommendation: 'Хорошо',
      attempts: [],
    }),
  ),
]

// ---- Reset between tests ----
export function resetAdminStore(): void {
  competencySeq = 1
  competencies.splice(
    0,
    competencies.length,
    { id: 'comp-1', name: 'Java', description: 'Оценка знаний Java' },
    { id: 'comp-2', name: 'SQL', description: 'Оценка знаний SQL' },
  )
  sectionSeq = 1
  sections.splice(
    0,
    sections.length,
    { id: 'sec-1', name: 'Java Core', description: 'Базовые концепции Java', sortOrder: 1 },
    { id: 'sec-2', name: 'Java EE', description: 'Корпоративные технологии', sortOrder: 2 },
  )
  topicSeq = 1
  topics.splice(
    0,
    topics.length,
    { id: 'top-1', name: 'Stream API', description: 'Потоки', sortOrder: 1, weight: 5 },
    { id: 'top-2', name: 'Коллекции', description: 'Collections', sortOrder: 2, weight: 4 },
  )
  employeeSeq = 1
  employees.splice(0, employees.length, {
    id: 'emp-1',
    fullName: 'Иванов Иван',
    position: 'Разработчик',
    department: 'Разработка',
    competency: competencies[0],
  })
  tokens.splice(0, tokens.length, {
    id: 'tok-1',
    token: 'tok-seed-1',
    employeeId: 'emp-1',
    used: false,
    createdAt: '2026-08-01T10:00:00',
    expiresAt: '2026-08-04T10:00:00',
  })
  aiActiveProvider = 'gemini'
}

// Re-export param helper for potential consumers.
export { param }
