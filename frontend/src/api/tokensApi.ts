import { adminFetch, adminJson, type InviteToken, parseError } from './shared'

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
