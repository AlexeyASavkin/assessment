import { useState } from 'react'
import { type Competency, createCompetency, updateCompetency } from '../../../api/admin'

/**
 * Пропсы формы компетенции.
 */
interface CompetencyFormProps {
  editCompetency?: Competency
  onCancel: () => void
  onSaved: () => Promise<void>
}

/**
 * Форма создания и редактирования компетенции.
 */
export default function CompetencyForm({ editCompetency, onCancel, onSaved }: CompetencyFormProps) {
  const [name, setName] = useState(editCompetency?.name ?? '')
  const [description, setDescription] = useState(editCompetency?.description ?? '')
  const [saving, setSaving] = useState(false)
  const [err, setErr] = useState<string | null>(null)

  /**
   * Сохраняет компетенцию: создаёт новую или обновляет существующую.
   */
  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setErr(null)
    try {
      if (editCompetency) {
        await updateCompetency(editCompetency.id, name, description)
      } else {
        await createCompetency(name, description)
      }
      await onSaved()
    } catch (e) {
      setErr(e instanceof Error ? e.message : 'Ошибка сохранения')
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={submit} className="admin-form">
      <h3>{editCompetency ? 'Редактировать компетенцию' : 'Новая компетенция'}</h3>
      {err && <p className="error-text">{err}</p>}
      <div className="form-field">
        <label>Название</label>
        <input type="text" value={name} onChange={(e) => setName(e.target.value)} required />
      </div>
      <div className="form-field">
        <label>Описание</label>
        <textarea value={description} onChange={(e) => setDescription(e.target.value)} />
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
