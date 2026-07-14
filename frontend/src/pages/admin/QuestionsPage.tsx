import { useState, useEffect, FormEvent } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  listQuestions,
  generateQuestions,
  updateQuestion,
  deleteQuestion,
  getCompetency,
  type QuestionBankItem,
  type Competency,
} from '../../api/admin'
import { AdminPageWrapper } from '../../components/admin/AdminLayout'

interface GenFormState {
  count: string
  difficulty: string
}

const emptyForm: GenFormState = { count: '3', difficulty: 'ALL' }

export default function QuestionsPage() {
  const { competencyId } = useParams<{ competencyId: string }>()
  const navigate = useNavigate()
  const [items, setItems] = useState<QuestionBankItem[]>([])
  const [competency, setCompetency] = useState<Competency | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [form, setForm] = useState<GenFormState>(emptyForm)
  const [generating, setGenerating] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editText, setEditText] = useState('')
  const [saving, setSaving] = useState(false)

  const load = async () => {
    if (!competencyId) return
    setIsLoading(true)
    setError(null)
    try {
      const [data, comp] = await Promise.all([
        listQuestions(competencyId),
        getCompetency(competencyId),
      ])
      setItems(data)
      setCompetency(comp)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [competencyId])

  useEffect(() => {
    if (!success) return
    const timer = setTimeout(() => setSuccess(null), 3000)
    return () => clearTimeout(timer)
  }, [success])

  const handleGenerate = async (e: FormEvent) => {
    e.preventDefault()
    if (!competencyId) return
    setGenerating(true)
    setError(null)
    setSuccess(null)
    try {
      const count = Number(form.count)
      const generated = await generateQuestions(competencyId, count, form.difficulty)
      await load()
      setSuccess(`Сгенерировано ${generated.length} вопросов`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка генерации')
    } finally {
      setGenerating(false)
    }
  }

  const startEdit = (item: QuestionBankItem) => {
    setEditingId(item.id)
    setEditText(item.questionText)
  }

  const cancelEdit = () => {
    setEditingId(null)
    setEditText('')
  }

  const handleSaveEdit = async (id: string) => {
    setSaving(true)
    setError(null)
    try {
      await updateQuestion(id, editText)
      cancelEdit()
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка сохранения')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id: string) => {
    if (!confirm('Удалить вопрос?')) return
    try {
      await deleteQuestion(id)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка удаления')
    }
  }

  const headerTitle = competency ? `Вопросы — ${competency.name}` : 'Вопросы'

  return (
    <AdminPageWrapper>
      <button className="btn admin-back" onClick={() => navigate('/admin/competencies')}>← К компетенциям</button>
      <h1>{headerTitle}</h1>
      {error && <p className="error-text">{error}</p>}
      {success && <p className="success-text">{success}</p>}

      <div className="card">
        <h2>Генерация вопросов</h2>
        <form onSubmit={handleGenerate} className="admin-form">
          <div className="form-field">
            <label htmlFor="count">Количество</label>
            <input
              id="count"
              type="number"
              min="1"
              max="10"
              value={form.count}
              onChange={(e) => setForm({ ...form, count: e.target.value })}
              required
            />
          </div>
          <div className="form-field">
            <label htmlFor="difficulty">Сложность</label>
            <select
              id="difficulty"
              value={form.difficulty}
              onChange={(e) => setForm({ ...form, difficulty: e.target.value })}
            >
              <option value="ALL">ALL</option>
              <option value="JUNIOR">JUNIOR</option>
              <option value="MIDDLE">MIDDLE</option>
              <option value="SENIOR">SENIOR</option>
            </select>
          </div>
          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={generating}>
              {generating ? 'Генерация...' : 'Сгенерировать'}
            </button>
          </div>
        </form>
      </div>

      {isLoading ? (
        <p>Загрузка...</p>
      ) : items.length === 0 ? (
        <p>Вопросов пока нет. Нажмите «Сгенерировать», чтобы создать.</p>
      ) : (
        <>
          <p>{items.length} вопросов</p>
          <div className="admin-list">
            {items.map((item) => (
              <div key={item.id} className="card admin-list-item">
                <div className="admin-list-info">
                  {editingId === item.id ? (
                    <div className="form-field">
                      <textarea
                        value={editText}
                        onChange={(e) => setEditText(e.target.value)}
                        style={{ minHeight: '80px' }}
                      />
                    </div>
                  ) : (
                    <h3>{item.questionText} <span className="badge">{item.difficulty}</span></h3>
                  )}
                </div>
                <div className="admin-list-actions">
                  {editingId === item.id ? (
                    <>
                      <button className="btn btn-primary" onClick={() => handleSaveEdit(item.id)} disabled={saving}>
                        {saving ? 'Сохранение...' : 'Сохранить'}
                      </button>
                      <button className="btn" onClick={cancelEdit}>Отмена</button>
                    </>
                  ) : (
                    <>
                      <button className="btn" onClick={() => startEdit(item)}>Редактировать</button>
                      <button className="btn btn-danger" onClick={() => handleDelete(item.id)}>Удалить</button>
                    </>
                  )}
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </AdminPageWrapper>
  )
}