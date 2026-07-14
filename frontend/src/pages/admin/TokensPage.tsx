import { useState, useEffect } from 'react'
import { listTokens, type InviteToken } from '../../api/admin'
import { AdminPageWrapper } from '../../components/admin/AdminLayout'

export default function TokensPage() {
  const [items, setItems] = useState<InviteToken[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await listTokens()
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

  const renderValue = (item: InviteToken, key: keyof InviteToken) => {
    const value = item[key]
    if (value === undefined || value === null) return '—'
    if (typeof value === 'boolean') return value ? 'да' : 'нет'
    return String(value)
  }

  return (
    <AdminPageWrapper>
      <h1>Пригласительные токены</h1>
      {error && <p className="error-text">{error}</p>}

      {isLoading ? (
        <p>Загрузка...</p>
      ) : items.length === 0 ? (
        <p>Токенов пока нет.</p>
      ) : (
        <div className="table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Токен</th>
                <th>Сотрудник</th>
                <th>Использован</th>
                <th>Создан</th>
                <th>Истекает</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id}>
                  <td className="mono">{renderValue(item, 'token')}</td>
                  <td>{renderValue(item, 'employeeId')}</td>
                  <td>{renderValue(item, 'used')}</td>
                  <td>{renderValue(item, 'createdAt')}</td>
                  <td>{renderValue(item, 'expiresAt')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </AdminPageWrapper>
  )
}