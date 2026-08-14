import { useState } from 'react'
import { createTopic, type Topic, updateTopic } from '../../../api/admin'

/**
 * Пропсы формы темы.
 */
interface TopicFormProps {
  editTopic?: Topic
  parentId?: string
  onCancel: () => void
  onSaved: (sectionId: string) => Promise<void>
}

/**
 * Форма создания и редактирования темы внутри секции.
 */
export default function TopicForm({ editTopic, parentId, onCancel, onSaved }: TopicFormProps) {
  const [name, setName] = useState(editTopic?.name ?? '')
  const [weight, setWeight] = useState(editTopic?.weight ?? 1)
  const [sortOrder, setSortOrder] = useState(editTopic?.sortOrder ?? 0)
  const [saving, setSaving] = useState(false)
  const [err, setErr] = useState<string | null>(null)

  /**
   * Сохраняет тему: создаёт новую или обновляет существующую.
   */
  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setErr(null)
    try {
      if (editTopic) {
        await updateTopic(editTopic.id, name, weight, sortOrder)
        await onSaved(parentId ?? '')
      } else {
        if (!parentId) {
          setErr('Не указана секция')
          return
        }
        await createTopic(parentId, name, weight, sortOrder)
        await onSaved(parentId)
      }
    } catch (e) {
      setErr(e instanceof Error ? e.message : 'Ошибка сохранения')
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={submit} className="admin-form">
      <h3>{editTopic ? 'Редактировать тему' : 'Новая тема'}</h3>
      {err && <p className="error-text">{err}</p>}
      <div className="form-field">
        <label>Название</label>
        <input type="text" value={name} onChange={(e) => setName(e.target.value)} required />
      </div>
      <div className="form-field">
        <label>Порядок</label>
        <input
          type="number"
          value={sortOrder}
          onChange={(e) => setSortOrder(parseInt(e.target.value, 10) || 0)}
        />
      </div>
      <div className="form-field">
        <label>Вес</label>
        <input
          type="number"
          step="0.1"
          min="0"
          value={weight}
          onChange={(e) => setWeight(parseFloat(e.target.value) || 1)}
        />
      </div>
      <div className="form-actions">
        <button type="submit" className="btn btn-primary" disabled={saving}>
          {saving ? 'Сохранение...' : 'Сохранить'}
        </button>
        <button type="button" className="btn" onClick={onCancel}>
          Отмена
        </button>
      </div>
    </form>
  )
}
