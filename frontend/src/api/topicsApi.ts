import { adminJson, type Topic } from './shared'

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
export async function createTopic(
  sectionId: string,
  name: string,
  weight: number,
  sortOrder: number,
): Promise<Topic> {
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
export async function updateTopic(
  id: string,
  name: string,
  weight: number,
  sortOrder: number,
): Promise<Topic> {
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
