import { useState, useEffect, FormEvent } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  listCriteria,
  createCriterion,
  updateCriterion,
  deleteCriterion,
  type Criterion,
} from '../../api/admin'
import { AdminPageWrapper } from '../../components/admin/AdminLayout'

interface FormState {
  name: string
  description: string
  weight: string
}

const emptyForm: FormState = { name: '', description: '', weight: '1' }

export default function CriteriaPage() {
  const { competencyId } = useParams<{ competencyId: string }>()
  const navigate = useNavigate()
  const [items, setItems] = useState<Criterion[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const load = async () => {
    if (!competencyId) return
    setIsLoading(true)
    setError(null)
    try {
      const data = await listCriteria(competencyId)
      setItems(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [competencyId])

  const resetForm = () => {
    setForm(emptyForm)
    setEditingId(null)
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!competencyId) return
    setSaving(true)
    setError(null)
    try {
      const weight = Number(form.weight)
      if (editingId) {
        await updateCriterion(editingId, form.name, form.description, weight)
      } else {
        await createCriterion(competencyId, form.name, form.description, weight)
      }
      resetForm()
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка сохранения')
    } finally {
      setSaving(false)
    }
  }

  const handleEdit = (item: Criterion) => {
    setEditingId(item.id)
    setForm({ name: item.name, description: item.description, weight: String(item.weight) })
  }

  const handleDelete = async (id: string) => {
    if (!confirm('Удалить критерий?')) return
    try {
      await deleteCriterion(id)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка удаления')
    }
  }

  return (
    <AdminPageWrapper>
      <button className="btn admin-back" onClick={() => navigate('/admin/competencies')}>← К компетенциям</button>
      <h1>Критерии</h1>
      {error && <p className="error-text">{error}</p>}

      <div className="card">
        <h2>{editingId ? 'Редактировать' : 'Создать'} критерий</h2>
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
          <div className="form-field">
            <label htmlFor="weight">Вес</label>
            <input
              id="weight"
              type="number"
              step="0.1"
              min="0"
              value={form.weight}
              onChange={(e) => setForm({ ...form, weight: e.target.value })}
              required
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
        <p>Критериев пока нет.</p>
      ) : (
        <div className="admin-list">
          {items.map((item) => (
            <div key={item.id} className="card admin-list-item">
              <div className="admin-list-info">
                <h3>{item.name} <span className="badge">вес: {item.weight}</span></h3>
                {item.description && <p>{item.description}</p>}
              </div>
              <div className="admin-list-actions">
                <button className="btn btn-primary" onClick={() => navigate(`/admin/criteria/${item.id}/levels`)}>
                  Уровни
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