import { HttpResponse, http } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { server } from '../test/server'
import { adminFetch, adminJson, parseError } from './shared'

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

describe('adminFetch', () => {
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
