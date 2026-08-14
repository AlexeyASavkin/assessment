import { API_BASE, adminFetch } from './shared'

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
