import { API_BASE, adminFetch, getXsrfToken } from './shared'

/**
 * Аутентифицирует администратора по логину и паролю.
 * Первый POST без XSRF-токена отклоняется CSRF-фильтром (302 + Set-Cookie XSRF-TOKEN),
 * поэтому запрос повторяется один раз с токеном из cookie (deferred-token bootstrap).
 * @param username - имя пользователя
 * @param password - пароль
 * @throws Error при неверных учетных данных или ошибке сервера
 */
export async function adminLogin(username: string, password: string): Promise<void> {
  const body = new URLSearchParams()
  body.append('username', username)
  body.append('password', password)

  const doLogin = (token: string): Promise<Response> => {
    const headers: Record<string, string> = { 'Content-Type': 'application/x-www-form-urlencoded' }
    if (token) headers['X-XSRF-TOKEN'] = token
    return fetch(`${API_BASE}/login`, {
      method: 'POST',
      headers,
      credentials: 'include',
      redirect: 'manual',
      body: body.toString(),
    })
  }

  let response = await doLogin(getXsrfToken())
  // CSRF-фильтр отклоняет первый POST без токена: 302 + Set-Cookie XSRF-TOKEN.
  if (response.type === 'opaqueredirect' || response.status === 302) {
    const retryToken = getXsrfToken()
    if (retryToken) response = await doLogin(retryToken)
  }
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
