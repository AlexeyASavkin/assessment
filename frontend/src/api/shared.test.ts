import { HttpResponse, http } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { server } from '../test/server'
import { adminFetch, adminJson, getXsrfToken, parseError } from './shared'

/** Устанавливает XSRF-TOKEN cookie для теста. */
function setXsrfCookie(value: string): void {
  // biome-ignore lint/suspicious/noDocumentCookie: jsdom не поддерживает Cookie Store API
  document.cookie = `XSRF-TOKEN=${value}`
}

/** Удаляет XSRF-TOKEN cookie между тестами. */
function clearXsrfCookie(): void {
  // biome-ignore lint/suspicious/noDocumentCookie: jsdom не поддерживает Cookie Store API
  document.cookie = 'XSRF-TOKEN=; Max-Age=0'
}

describe('parseError', () => {
  it('извлекает текст ошибки из тела ответа', async () => {
    const response = new Response('Ошибка сервера', { status: 500 })
    await expect(parseError(response)).resolves.toBe('Ошибка сервера')
  })

  it('возвращает сообщение со статусом, когда тело пустое', async () => {
    const response = new Response('', { status: 500 })
    await expect(parseError(response)).resolves.toBe('Request failed with status 500')
  })

  it('возвращает сообщение со статусом, когда тело нельзя прочитать', async () => {
    const response = new Response(null, { status: 500 })
    Object.defineProperty(response, 'text', { value: () => Promise.reject(new Error('boom')) })
    await expect(parseError(response)).resolves.toBe('Request failed with status 500')
  })
})

describe('getXsrfToken', () => {
  afterEach(clearXsrfCookie)

  it('возвращает значение токена из cookie', () => {
    setXsrfCookie('abc-123')
    expect(getXsrfToken()).toBe('abc-123')
  })

  it('возвращает пустую строку, когда cookie отсутствует', () => {
    clearXsrfCookie()
    expect(getXsrfToken()).toBe('')
  })
})

describe('adminFetch', () => {
  afterEach(clearXsrfCookie)

  it('вызывает fetch с базовым путём /api/admin и credentials include', async () => {
    server.use(
      http.get('/api/admin/check', ({ request }) => {
        expect(request.credentials).toBe('include')
        return HttpResponse.json({ ok: true })
      }),
    )
    const response = await adminFetch('/check')
    expect(response.ok).toBe(true)
  })

  it('добавляет X-XSRF-TOKEN для POST, когда токен есть в cookie', async () => {
    setXsrfCookie('test-token-123')
    server.use(
      http.post('/api/admin/check', ({ request }) => {
        expect(request.headers.get('X-XSRF-TOKEN')).toBe('test-token-123')
        return HttpResponse.json({ ok: true })
      }),
    )
    const response = await adminFetch('/check', { method: 'POST' })
    expect(response.ok).toBe(true)
  })

  it('не добавляет X-XSRF-TOKEN для GET, даже когда токен есть в cookie', async () => {
    setXsrfCookie('test-token-123')
    server.use(
      http.get('/api/admin/check', ({ request }) => {
        expect(request.headers.get('X-XSRF-TOKEN')).toBeNull()
        return HttpResponse.json({ ok: true })
      }),
    )
    const response = await adminFetch('/check')
    expect(response.ok).toBe(true)
  })

  it('не добавляет X-XSRF-TOKEN для POST без токена в cookie', async () => {
    clearXsrfCookie()
    server.use(
      http.post('/api/admin/check', ({ request }) => {
        expect(request.headers.get('X-XSRF-TOKEN')).toBeNull()
        return HttpResponse.json({ ok: true })
      }),
    )
    const response = await adminFetch('/check', { method: 'POST' })
    expect(response.ok).toBe(true)
  })
})

describe('adminJson', () => {
  it('возвращает распарсенный JSON при успешном ответе', async () => {
    server.use(http.get('/api/admin/data', () => HttpResponse.json({ value: 42 })))
    await expect(adminJson<{ value: number }>('/data')).resolves.toEqual({ value: 42 })
  })

  it('возвращает undefined при статусе 204', async () => {
    server.use(http.get('/api/admin/empty', () => new HttpResponse(null, { status: 204 })))
    await expect(adminJson<undefined>('/empty')).resolves.toBeUndefined()
  })

  it('бросает Error с текстом ошибки при HTTP-ошибке', async () => {
    server.use(
      http.get('/api/admin/broken', () => new HttpResponse('Сервер упал', { status: 500 })),
    )
    await expect(adminJson('/broken')).rejects.toThrow('Сервер упал')
  })

  it('бросает «Не авторизован» при opaqueredirect', async () => {
    const response = new Response(null, { status: 200 })
    Object.defineProperty(response, 'type', { value: 'opaqueredirect' })
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => response),
    )
    try {
      await expect(adminJson('/any')).rejects.toThrow('Не авторизован')
    } finally {
      vi.unstubAllGlobals()
    }
  })
})
