import { useState, useEffect, FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  listCompetencies,
  createCompetency,
  updateCompetency,
  deleteCompetency,
  type Competency,
} from '../../api/admin'
import { AdminPageWrapper } from '../../components/admin/AdminLayout'

interface FormState {
  name: string
  description: string
}

const emptyForm: FormState = { name: '', description: '' }

export default function CompetenciesPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState<Competency[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const load = async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await listCompetencies()
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
        await updateCompetency(editingId, form.name, form.description)
      } else {
        await createCompetency(form.name, form.description)
      }
      resetForm()
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка сохранения')
    } finally {
      setSaving(false)
    }
  }

  const handleEdit = (item: Competency) => {
    setEditingId(item.id)
    setForm({ name: item.name, description: item.description })
  }

  const handleDelete = async (id: string) => {
    if (!confirm('Удалить компетенцию?')) return
    try {
      await deleteCompetency(id)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка удаления')
    }
  }

  return (
    <AdminPageWrapper>
      <h1>Компетенции</h1>
      {error && <p className="error-text">{error}</p>}

      <div className="card">
        <h2>{editingId ? 'Редактировать' : 'Создать'} компетенцию</h2>
        <form onSubmit={handleSubmit} className="admin-form">
          <div className="form-field">
            <label htmlFor="name">Название</label>
            <input
              id="name"
              type="text"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              required
            />
          </div>
          <div className="form-field">
            <label htmlFor="description">Описание</label>
            <textarea
              id="description"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
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

      {isLoading ? (
        <p>Загрузка...</p>
      ) : items.length === 0 ? (
        <p>Компетенций пока нет.</p>
      ) : (
        <div className="admin-list">
          {items.map((item) => (
            <div key={item.id} className="card admin-list-item">
              <div className="admin-list-info">
                <h3>{item.name}</h3>
                {item.description && <p>{item.description}</p>}
              </div>
              <div className="admin-list-actions">
                <button className="btn btn-primary" onClick={() => navigate(`/admin/competencies/${item.id}/criteria`)}>
                  Критерии
                </button>
                <button className="btn" onClick={() => handleEdit(item)}>Изменить</button>
                <button className="btn btn-danger" onClick={() => handleDelete(item.id)}>Удалить</button>
              </div>
            </div>
          ))}
        </div>
      )}
    </AdminPageWrapper>
  )
}