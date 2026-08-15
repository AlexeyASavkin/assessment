import { describe, expect, it } from 'vitest'
import { getAiSettings } from '../api/aiSettingsApi'
import { listCompetencies } from '../api/competenciesApi'
import { listEmployees } from '../api/employeesApi'

/**
 * Smoke-тест: убеждаемся, что MSW-сервер перехватывает fetch
 * и API-функции корректно десериализуют мок-ответы.
 */
describe('admin API smoke (MSW wiring)', () => {
  it('listCompetencies возвращает мок-компетенции через MSW', async () => {
    const result = await listCompetencies()
    expect(result).toHaveLength(2)
    expect(result[0]!.name).toBe('Java')
  })

  it('listEmployees возвращает мок-сотрудников', async () => {
    const result = await listEmployees()
    expect(result).toHaveLength(1)
    expect(result[0]!.fullName).toBe('Иванов Иван')
  })

  it('getAiSettings возвращает активного провайдера', async () => {
    const settings = await getAiSettings()
    expect(settings.activeProvider).toBe('gemini')
    expect(settings.availableProviders).toContain('stub')
  })
})
