import { adminJson } from './shared'

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
 * Промты ИИ (ключи совпадают с бэкенд-константами AiProviderService).
 */
export interface AiPrompts {
  prompt_scoring: string
  prompt_question: string
  prompt_followup: string
  prompt_rescore: string
  prompt_followup_system: string
  prompt_rescore_system: string
}

/**
 * Получает текущие промты ИИ.
 */
export async function getAiPrompts(): Promise<AiPrompts> {
  return adminJson<AiPrompts>('/settings/ai/prompts')
}

/**
 * Обновляет промты ИИ.
 */
export async function updateAiPrompts(prompts: AiPrompts): Promise<AiPrompts> {
  return adminJson<AiPrompts>('/settings/ai/prompts', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(prompts),
  })
}
