import { useState } from 'react'
import { createSection, type Section, updateSection } from '../../../api/admin'

/**
 * Пропсы формы секции.
 */
interface SectionFormProps {
  editSection?: Section
  parentId?: string
  onCancel: () => void
  onSaved: (competencyId: string) => Promise<void>
}

/**
 * Форма создания и редактирования секции внутри компетенции.
 */
export default function SectionForm({
  editSection,
  parentId,
  onCancel,
  onSaved,
}: SectionFormProps) {
  const [name, setName] = useState(editSection?.name ?? '')
  const [sortOrder, setSortOrder] = useState(editSection?.sortOrder ?? 0)
  const [saving, setSaving] = useState(false)
  const [err, setErr] = useState<string | null>(null)

  /**
   * Сохраняет секцию: создаёт новую или обновляет существующую.
   */
  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setErr(null)
    try {
      if (editSection) {
        await updateSection(editSection.id, name, sortOrder)
        await onSaved(parentId ?? '')
      } else {
        if (!parentId) {
          setErr('Не указана компетенция')
          return
        }
        await createSection(parentId, name, sortOrder)
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
      <h3>{editSection ? 'Редактировать секцию' : 'Новая секция'}</h3>
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
