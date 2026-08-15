import { adminJson, type Competency } from './shared'

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
export async function updateCompetency(
  id: string,
  name: string,
  description: string,
): Promise<Competency> {
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
