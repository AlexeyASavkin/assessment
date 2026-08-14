import { adminJson, type Section } from './shared'

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
export async function createSection(
  competencyId: string,
  name: string,
  sortOrder: number,
): Promise<Section> {
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
