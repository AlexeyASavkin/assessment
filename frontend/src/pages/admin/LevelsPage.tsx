import { useState, useEffect, FormEvent } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  listLevels,
  createLevel,
  updateLevel,
  deleteLevel,
  type CriteriaLevel,
  type LevelValue,
} from '../../api/admin'
import { AdminPageWrapper } from '../../components/admin/AdminLayout'

const LEVELS: LevelValue[] = ['JUNIOR', 'MIDDLE', 'SENIOR']

/**
 * Состояние формы создания и редактирования уровня требований.
 */
interface FormState {
  level: LevelValue
  requirements: string
}

const emptyForm: FormState = { level: 'JUNIOR', requirements: '' }

/**
 * Страница управления уровнями требований критерия.
 * Позволяет добавлять, редактировать и удалять уровни JUNIOR, MIDDLE и SENIOR.
 */
export default function LevelsPage() {
  const { criteriaId } = useParams<{ criteriaId: string }>()
  const navigate = useNavigate()
  const [items, setItems] = useState<CriteriaLevel[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

/**
   * Загружает список уровней требований для текущего критерия.
   */
  const load = async () => {
    if (!criteriaId) return
    setIsLoading(true)
    setError(null)
    try {
      const data = await listLevels(criteriaId)
      setItems(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [criteriaId])

/**
   * Сбрасывает форму в начальное состояние и отменяет режим редактирования.
   */
  const resetForm = () => {
    setForm(emptyForm)
    setEditingId(null)
  }

/**
   * Создаёт новый уровень требований или обновляет существующий, затем перезагружает список.
   */
  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!criteriaId) return
    setSaving(true)
    setError(null)
    try {
      if (editingId) {
        await updateLevel(editingId, form.level, form.requirements)
      } else {
        await createLevel(criteriaId, form.level, form.requirements)
      }
      resetForm()
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка сохранения')
    } finally {
      setSaving(false)
    }
  }

/**
   * Переключает форму в режим редактирования выбранного уровня.
   */
  const handleEdit = (item: CriteriaLevel) => {
    setEditingId(item.id)
    setForm({ level: item.level, requirements: item.requirements })
  }

/**
   * Удаляет уровень требований после подтверждения и обновляет список.
   */
  const handleDelete = async (id: string) => {
    if (!confirm('Удалить уровень?')) return
    try {
      await deleteLevel(id)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка удаления')
    }
  }

  return (
    <AdminPageWrapper>
      <button className="btn admin-back" onClick={() => navigate(-1)}>← Назад</button>
      <h1>Уровни требований</h1>
      {error && <p className="error-text">{error}</p>}

      <div className="card">
        <h2>{editingId ? 'Редактировать' : 'Создать'} уровень</h2>
        <form onSubmit={handleSubmit} className="admin-form">
          <div className="form-field">
            <label htmlFor="level">Уровень</label>
            <select
              id="level"
              value={form.level}
              onChange={(e) => setForm({ ...form, level: e.target.value as LevelValue })}
            >
              {LEVELS.map((l) => (
                <option key={l} value={l}>{l}</option>
              ))}
            </select>
          </div>
          <div className="form-field">
            <label htmlFor="requirements">Требования</label>
            <textarea
              id="requirements"
              value={form.requirements}
              onChange={(e) => setForm({ ...form, requirements: e.target.value })}
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
        <p>Уровней пока нет.</p>
      ) : (
        <div className="admin-list">
          {items.map((item) => (
            <div key={item.id} className="card admin-list-item">
              <div className="admin-list-info">
                <h3><span className="badge badge-level">{item.level}</span></h3>
                <p>{item.requirements}</p>
              </div>
              <div className="admin-list-actions">
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