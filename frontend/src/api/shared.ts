/**
 * Базовый путь для административного API.
 */
export const API_BASE = '/api/admin'

/**
 * Компетенция — область знаний или навыков для оценки.
 */
export interface Competency {
  id: string
  name: string
  description: string
}

/**
 * Раздел внутри компетенции.
 */
export interface Section {
  id: string
  name: string
  description: string
  sortOrder: number
}

/**
 * Тема внутри раздела компетенции.
 */
export interface Topic {
  id: string
  name: string
  description: string
  sortOrder: number
  weight: number
}

/**
 * Сотрудник, проходящий оценку компетенций.
 */
export interface Employee {
  id: string
  fullName: string
  position: string
  department: string
  competency?: Competency
}

/**
 * Одноразовый пригласительный токен для сотрудника.
 */
export interface InviteToken {
  id: string
  token: string
  employeeId: string
  used: boolean
  createdAt?: string
  expiresAt?: string
}

/**
 * Извлекает текст ошибки из HTTP-ответа.
 * @param response - HTTP-ответ
 * @return текст ошибки или сообщение со статусом
 */
export async function parseError(response: Response): Promise<string> {
  try {
    const text = await response.text()
    return text || `Request failed with status ${response.status}`
  } catch {
    return `Request failed with status ${response.status}`
  }
}

/**
 * Читает XSRF-токен из cookie (устанавливается сервером, доступен JS).
 * @return значение токена или пустая строка, если cookie отсутствует
 */
export function getXsrfToken(): string {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/)
  const raw = match?.[1]
  if (!raw) return ''
  try {
    return decodeURIComponent(raw)
  } catch {
    return raw
  }
}

/**
 * Выполняет fetch-запрос к административному API с передачей cookie.
 * Для мутирующих методов (POST/PUT/DELETE/PATCH) добавляет заголовок X-XSRF-TOKEN
 * из cookie — CSRF-защита включена для /api/admin/**.
 * @param path - относительный путь
 * @param init - дополнительные параметры fetch
 * @return HTTP-ответ
 */
export async function adminFetch(path: string, init?: RequestInit): Promise<Response> {
  const method = (init?.method ?? 'GET').toUpperCase()
  const headers = new Headers(init?.headers)
  if (method !== 'GET' && method !== 'HEAD') {
    const token = getXsrfToken()
    if (token) headers.set('X-XSRF-TOKEN', token)
  }
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers,
    credentials: 'include',
    redirect: 'manual',
  })
  return response
}

/**
 * Выполняет JSON-запрос к административному API и парсит ответ.
 * @param path - относительный путь
 * @param init - дополнительные параметры fetch
 * @return распарсенный JSON-ответ
 * @throws Error при неавторизованном доступе или ошибке сервера
 */
export async function adminJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await adminFetch(path, init)
  if (response.type === 'opaqueredirect') {
    throw new Error('Не авторизован')
  }
  if (!response.ok) throw new Error(await parseError(response))
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}
