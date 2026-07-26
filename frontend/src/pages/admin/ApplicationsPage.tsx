import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { listApplications, type ApplicationSummary } from '../../api/admin'
import { AdminPageWrapper } from '../../components/admin/AdminLayout'

/**
 * Страница «Результаты» — единый список заявок на оценку.
 * Связывает пригласительный токен, сессию и результат (средний балл, пройдена ли оценка).
 * Клик по завершённой заявке открывает детальный отчёт.
 */
export default function ApplicationsPage() {
  const [items, setItems] = useState<ApplicationSummary[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  /**
   * Загружает список заявок с сервера.
   */
  const load = async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await listApplications()
      // Сортировка: новые сверху, завершённые раньше активных
      data.sort((a, b) => {
        const aTime = a.createdAt ? Date.parse(a.createdAt) : 0
        const bTime = b.createdAt ? Date.parse(b.createdAt) : 0
        return bTime - aTime
      })
      setItems(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  /**
   * Открывает детальный отчёт по завершённой заявке.
   * @param sessionId - идентификатор сессии
   */
  const openReport = (sessionId: string | null) => {
    if (!sessionId) return
    navigate(`/admin/applications/${sessionId}/report`)
  }

  /**
   * Возвращает отображаемое имя статуса сессии.
   * @param status - статус сессии
   */
  const renderStatus = (status: string | null): string => {
    if (!status) return 'Не начата'
    switch (status) {
      case 'COMPLETED': return 'Завершена'
      case 'ACTIVE': return 'Активна'
      default: return status
    }
  }

  /**
   * Форматирует ISO-дату в локальный формат.
   * @param iso - ISO-строка даты
   */
  const formatDate = (iso: string | null): string => {
    if (!iso) return '—'
    const d = new Date(iso)
    return d.toLocaleString('ru-RU', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  return (
    <AdminPageWrapper>
      <h1>Результаты</h1>
      {error && <p className="error-text">{error}</p>}

      {isLoading ? (
        <p>Загрузка...</p>
      ) : items.length === 0 ? (
        <p>Результатов пока нет.</p>
      ) : (
        <div className="table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Сотрудник</th>
                <th>Компетенция</th>
                <th>Статус</th>
                <th>Средний балл</th>
                <th>Результат</th>
                <th>Создана</th>
                <th>Завершена</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => {
                const hasReport = item.sessionStatus === 'COMPLETED' && item.sessionId
                return (
                  <tr key={item.tokenId}>
                    <td>{item.employeeName ?? '—'}</td>
                    <td>{item.competencyName ?? '—'}</td>
                    <td>{renderStatus(item.sessionStatus)}</td>
                    <td>
                      {item.averageScore != null ? item.averageScore.toFixed(2) : '—'}
                    </td>
                    <td>
                      {item.sessionStatus === 'COMPLETED' ? (
                        <span style={{
                          color: item.passed ? '#28a745' : '#dc3545',
                          fontWeight: 600,
                        }}>
                          {item.passed ? 'Пройден' : 'Не пройден'}
                        </span>
                      ) : '—'}
                    </td>
                    <td>{formatDate(item.createdAt)}</td>
                    <td>{formatDate(item.completedAt)}</td>
                    <td>
                      {hasReport ? (
                        <button
                          className="btn btn-primary"
                          onClick={() => openReport(item.sessionId)}
                        >
                          Отчёт
                        </button>
                      ) : (
                        <span style={{ color: '#6c757d' }}>—</span>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      <button className="btn btn-secondary" onClick={load} style={{ marginTop: '1rem' }}>
        Обновить
      </button>
    </AdminPageWrapper>
  )
}