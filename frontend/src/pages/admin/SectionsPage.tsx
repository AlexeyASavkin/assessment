import { useState, useEffect, FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  listSections,
  createSection,
  updateSection,
  deleteSection,
  getCompetency,
  type Section,
} from '../../api/admin'
import { AdminPageWrapper } from '../../components/admin/AdminLayout'

interface FormState {
  name: string
  sortOrder: number
}

const emptyForm: FormState = { name: '', sortOrder: 0 }

export default function SectionsPage() {
  const navigate = useNavigate()
  const { competencyId } = useParams<{ competencyId: string }>()
  const [competencyName, setCompetencyName] = useState('')
  const [items, setItems] = useState<Section[]>([])
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
      const [sections, competency] = await Promise.all([
        listSections(competencyId),
        getCompetency(competencyId),
      ])
      setItems(sections)
      setCompetencyName(competency.name)
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
      if (editingId) {
        await updateSection(editingId, form.name, form.sortOrder)
      } else {
        await createSection(competencyId, form.name, form.sortOrder)
      }
      resetForm()
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка сохранения')
    } finally {
      setSaving(false)
    }
  }

  const handleEdit = (item: Section) => {
    setEditingId(item.id)
    setForm({ name: item.name, sortOrder: item.sortOrder })
  }

  const handleDelete = async (id: string) => {
    if (!confirm('Удалить секцию?')) return
    try {
      await deleteSection(id)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка удаления')
    }
  }

  return (
    <AdminPageWrapper>
      <button className="btn admin-back" onClick={() => navigate('/admin/competencies')}>
        ← К компетенциям
      </button>
      <h1>Секции — {competencyName}</h1>
      {error && <p className="error-text">{error}</p>}

      <div className="card">
        <h2>{editingId ? 'Редактировать' : 'Создать'} секцию</h2>
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
            <label htmlFor="sortOrder">Порядок</label>
            <input
              id="sortOrder"
              type="number"
              value={form.sortOrder}
              onChange={(e) => setForm({ ...form, sortOrder: parseInt(e.target.value) || 0 })}
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
        <p>Секций пока нет.</p>
      ) : (
        <div className="admin-list">
          {items.map((item) => (
            <div key={item.id} className="card admin-list-item">
              <div className="admin-list-info">
                <h3>{item.name}</h3>
                <span className="badge">Порядок: {item.sortOrder}</span>
              </div>
              <div className="admin-list-actions">
                <button className="btn btn-primary" onClick={() => navigate(`/admin/sections/${item.id}/topics`)}>
                  Темы
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
