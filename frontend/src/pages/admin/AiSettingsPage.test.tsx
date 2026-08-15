import { fireEvent, render, screen } from '@testing-library/react'
import { HttpResponse, http } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../test/server'
import AiSettingsPage from './AiSettingsPage'

/**
 * RTL-тесты страницы настроек ИИ: загрузка настроек/промтов,
 * переключение провайдера, сохранение промтов, обработка ошибок.
 */
describe('AiSettingsPage', () => {
  it('загружает и отображает активного провайдера и промты', async () => {
    render(<AiSettingsPage />)

    const geminiRadio = await screen.findByLabelText(/Google Gemini/)
    expect(geminiRadio).toBeChecked()
    expect(screen.getByLabelText(/Сбер GigaChat/)).not.toBeChecked()

    const scoringTextarea = screen.getByPlaceholderText('Промт оценки ответа')
    expect(scoringTextarea).toHaveValue('Оцени ответ')
  })

  it('переключает активного провайдера и сохраняет', async () => {
    render(<AiSettingsPage />)

    const stubRadio = await screen.findByLabelText(/Stub/)
    fireEvent.click(stubRadio)
    fireEvent.click(screen.getByRole('button', { name: 'Сохранить провайдер' }))

    expect(await screen.findByText('Провайдер сохранён')).toBeInTheDocument()
    expect(screen.getByLabelText(/Stub/)).toBeChecked()
  })

  it('редактирует и сохраняет промты', async () => {
    render(<AiSettingsPage />)

    const scoringTextarea = await screen.findByPlaceholderText('Промт оценки ответа')
    fireEvent.change(scoringTextarea, { target: { value: 'Новый промт оценки' } })
    fireEvent.click(screen.getByRole('button', { name: 'Сохранить промты' }))

    expect(await screen.findByText('Промты сохранены')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('Промт оценки ответа')).toHaveValue('Новый промт оценки')
  })

  it('показывает ошибку при сбое загрузки настроек', async () => {
    server.use(
      http.get(
        '/api/admin/settings/ai',
        () => new HttpResponse('Сервер недоступен', { status: 500 }),
      ),
    )
    render(<AiSettingsPage />)

    expect(await screen.findByText('Сервер недоступен')).toBeInTheDocument()
    // При ошибке загрузки настроек секция провайдера не рендерится
    expect(screen.queryByRole('button', { name: 'Сохранить провайдер' })).not.toBeInTheDocument()
  })
})
