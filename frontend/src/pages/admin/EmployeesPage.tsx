import { useState, useEffect, FormEvent } from 'react'
import {
  listEmployees,
  createEmployee,
  updateEmployee,
  generateInvite,
  type Employee,
} from '../../api/admin'
import { AdminPageWrapper } from '../../components/admin/AdminLayout'

interface FormState {
  fullName: string
  position: string
  department: string
}

const emptyForm: FormState = { fullName: '', position: '', department: '' }

export default function EmployeesPage() {
  const [items, setItems] = useState<Employee[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [inviteUrl, setInviteUrl] = useState<string | null>(null)
  const [inviteFor, setInviteFor] = useState<string | null>(null)
  const [generating, setGenerating] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)

  const load = async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await listEmployees()
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

  const resetForm = () => {
    setForm(emptyForm)
    setEditingId(null)
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      if (editingId) {
        await updateEmployee(editingId, form.fullName, form.position, form.department)
      } else {
        await createEmployee(form.fullName, form.position, form.department)
      }
      resetForm()
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка сохранения')
    } finally {
      setSaving(false)
    }
  }

  const handleEdit = (item: Employee) => {
    setEditingId(item.id)
    setForm({ fullName: item.fullName, position: item.position, department: item.department })
  }

  const handleGenerateInvite = async (employee: Employee) => {
    setGenerating(employee.id)
    setError(null)
    setInviteUrl(null)
    setInviteFor(employee.id)
    setCopied(false)
    try {
      const path = await generateInvite(employee.id)
      const fullUrl = `${window.location.origin}${path}`
      setInviteUrl(fullUrl)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка генерации ссылки')
    } finally {
      setGenerating(null)
    }
  }

  const handleCopy = async () => {
    if (!inviteUrl) return
    try {
      await navigator.clipboard.writeText(inviteUrl)
      setCopied(true)
    } catch {
      // Fallback: select the input
      const input = document.getElementById('invite-url') as HTMLInputElement | null
      if (input) {
        input.select()
      }
    }
  }

  return (
    <AdminPageWrapper>
      <h1>Сотрудники</h1>
      {error && <p className="error-text">{error}</p>}

      <div className="card">
        <h2>{editingId ? 'Редактировать' : 'Создать'} сотрудника</h2>
        <form onSubmit={handleSubmit} className="admin-form">
          <div className="form-field">
            <label htmlFor="fullName">ФИО</label>
            <input
              id="fullName"
              type="text"
              value={form.fullName}
              onChange={(e) => setForm({ ...form, fullName: e.target.value })}
              required
            />
          </div>
          <div className="form-field">
            <label htmlFor="position">Должность</label>
            <input
              id="position"
              type="text"
              value={form.position}
              onChange={(e) => setForm({ ...form, position: e.target.value })}
            />
          </div>
          <div className="form-field">
            <label htmlFor="department">Отдел</label>
            <input
              id="department"
              type="text"
              value={form.department}
              onChange={(e) => setForm({ ...form, department: e.target.value })}
            />
          </div>
          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Сохранение...' : 'Сохранить'}
            </button>
            {editingId && (
              <button type="button" className="btn" onClick={resetForm}>Отмена</button>
            )}
          </div>
        </form>
      </div>

      {inviteUrl && (
        <div className="card invite-card">
          <h2>Пригласительная ссылка</h2>
          <p>Ссылка для сотрудника {inviteFor}:</p>
          <div className="invite-url-row">
            <input id="invite-url" type="text" readOnly value={inviteUrl} className="invite-url-input" />
            <button className="btn btn-success" onClick={handleCopy}>
              {copied ? 'Скопировано!' : 'Копировать'}
            </button>
          </div>
        </div>
      )}

      {isLoading ? (
        <p>Загрузка...</p>
      ) : items.length === 0 ? (
        <p>Сотрудников пока нет.</p>
      ) : (
        <div className="admin-list">
          {items.map((item) => (
            <div key={item.id} className="card admin-list-item">
              <div className="admin-list-info">
                <h3>{item.fullName}</h3>
                {item.position && <p>Должность: {item.position}</p>}
                {item.department && <p>Отдел: {item.department}</p>}
              </div>
              <div className="admin-list-actions">
                <button
                  className="btn btn-success"
                  onClick={() => handleGenerateInvite(item)}
                  disabled={generating === item.id}
                >
                  {generating === item.id ? 'Генерация...' : 'Пригласительная ссылка'}
                </button>
                <button className="btn" onClick={() => handleEdit(item)}>Изменить</button>
              </div>
            </div>
          ))}
        </div>
      )}
    </AdminPageWrapper>
  )
}