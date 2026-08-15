import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminCompetenciesTreePage from './AdminCompetenciesTreePage'

/**
 * Lock-тесты поведения дерева компетенций.
 * Фиксируют пользовательское поведение ДО рефакторинга (split на компоненты),
 * чтобы рефакторинг не изменил видимого результата.
 */
describe('AdminCompetenciesTreePage (lock)', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('загружает и отображает список компетенций', async () => {
    render(<AdminCompetenciesTreePage />)
    expect(await screen.findByText('📁 Java')).toBeInTheDocument()
    expect(screen.getByText('📁 SQL')).toBeInTheDocument()
  })

  it('раскрытие компетенции загружает и показывает её секции', async () => {
    render(<AdminCompetenciesTreePage />)
    await screen.findByText('📁 Java')

    fireEvent.click(screen.getAllByRole('button', { name: 'Развернуть' })[0]!)

    expect(await screen.findByText('📂 Java Core')).toBeInTheDocument()
    expect(screen.getByText('📂 Java EE')).toBeInTheDocument()
  })

  it('раскрытие секции показывает темы, выбор темы открывает панель деталей', async () => {
    render(<AdminCompetenciesTreePage />)
    await screen.findByText('📁 Java')

    fireEvent.click(screen.getAllByRole('button', { name: 'Развернуть' })[0]!)
    const sectionRow = (await screen.findByText('📂 Java Core')).closest('.tree-row') as HTMLElement
    fireEvent.click(within(sectionRow).getByRole('button', { name: 'Развернуть' }))
    await screen.findByText('📄 Stream API')

    fireEvent.click(screen.getByText('📄 Stream API'))

    expect(screen.getByText('Java › Java Core')).toBeInTheDocument()
    expect(await screen.findByText('Вопросов пока нет.')).toBeInTheDocument()
  })

  it('создаёт компетенцию через модальное окно и обновляет список', async () => {
    render(<AdminCompetenciesTreePage />)
    await screen.findByText('📁 Java')

    fireEvent.click(screen.getByText('+ Компетенцию'))
    const textboxes = screen.getAllByRole('textbox')
    fireEvent.change(textboxes[0]!, { target: { value: 'Go' } })
    fireEvent.change(textboxes[1]!, { target: { value: 'Оценка знаний Go' } })
    fireEvent.click(screen.getByRole('button', { name: 'Сохранить' }))

    expect(await screen.findByText('📁 Go')).toBeInTheDocument()
  })

  it('удаляет компетенцию после подтверждения', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    render(<AdminCompetenciesTreePage />)
    await screen.findByText('📁 Java')

    fireEvent.click(screen.getAllByTitle('Удалить')[0]!)

    await waitFor(() => expect(screen.queryByText('📁 Java')).not.toBeInTheDocument())
    expect(screen.getByText('📁 SQL')).toBeInTheDocument()
    expect(window.confirm).toHaveBeenCalled()
  })
})
