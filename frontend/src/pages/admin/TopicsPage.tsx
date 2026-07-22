import { useState, useEffect, FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  listTopics,
  createTopic,
  updateTopic,
  deleteTopic,
  generateTopicQuestions,
  listTopicQuestions,
  deleteQuestion,
  type Topic,
  type QuestionBankItem,
} from '../../api/admin'
import { AdminPageWrapper } from '../../components/admin/AdminLayout'

interface FormState {
  name: string
  weight: number
}

const emptyForm: FormState = { name: '', weight: 1 }

export default function TopicsPage() {
  const navigate = useNavigate()
  const { sectionId } = useParams<{ sectionId: string }>()
  const [items, setItems] = useState<Topic[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const [generatingFor, setGeneratingFor] = useState<string | null>(null)
  const [generateCount, setGenerateCount] = useState(2)
  const [generating, setGenerating] = useState(false)
  const [success, setSuccess] = useState<string | null>(null)

  const [topicQuestions, setTopicQuestions] = useState<Record<string, QuestionBankItem[]>>({})
  const [loadingQuestionsFor, setLoadingQuestionsFor] = useState<string | null>(null)

  const load = async () => {
    if (!sectionId) return
    setIsLoading(true)
    setError(null)
    try {
      const data = await listTopics(sectionId)
      setItems(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [sectionId])

  useEffect(() => {
    if (success) {
      const timer = setTimeout(() => setSuccess(null), 3000)
      return () => clearTimeout(timer)
    }
  }, [success])

  const resetForm = () => {
    setForm(emptyForm)
    setEditingId(null)
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!sectionId) return
    setSaving(true)
    setError(null)
    try {
      if (editingId) {
        await updateTopic(editingId, form.name, form.weight)
      } else {
        await createTopic(sectionId, form.name, form.weight)
      }
      resetForm()
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка сохранения')
    } finally {
      setSaving(false)
    }
  }

  const handleEdit = (item: Topic) => {
    setEditingId(item.id)
    setForm({ name: item.name, weight: item.weight })
  }

  const handleDelete = async (id: string) => {
    if (!confirm('Удалить тему?')) return
    try {
      await deleteTopic(id)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка удаления')
    }
  }

  const handleGenerate = async (topicId: string) => {
    setGenerating(true)
    setError(null)
    try {
      const questions = await generateTopicQuestions(topicId, generateCount, 'ALL')
      setSuccess(`Сгенерировано ${questions.length} вопросов`)
      await loadQuestions(topicId)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка генерации')
    } finally {
      setGenerating(false)
    }
  }

  const loadQuestions = async (topicId: string) => {
    setLoadingQuestionsFor(topicId)
    try {
      const questions = await listTopicQuestions(topicId)
      setTopicQuestions((prev) => ({ ...prev, [topicId]: questions }))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки вопросов')
    } finally {
      setLoadingQuestionsFor(null)
    }
  }

  const handleDeleteQuestion = async (topicId: string, questionId: string) => {
    if (!confirm('Удалить вопрос?')) return
    try {
      await deleteQuestion(questionId)
      await loadQuestions(topicId)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка удаления')
    }
  }

  return (
    <AdminPageWrapper>
      <button className="btn admin-back" onClick={() => navigate(-1)}>
        ← К секциям
      </button>
      <h1>Темы</h1>
      {error && <p className="error-text">{error}</p>}
      {success && <p className="success-text">{success}</p>}

      <div className="card">
        <h2>{editingId ? 'Редактировать' : 'Создать'} тему</h2>
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
            <label htmlFor="weight">Вес</label>
            <input
              id="weight"
              type="number"
              step="0.1"
              min="0"
              value={form.weight}
              onChange={(e) => setForm({ ...form, weight: parseFloat(e.target.value) })}
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
        <p>Тем пока нет.</p>
      ) : (
        <div className="admin-list">
          {items.map((item) => (
            <div key={item.id} className="card admin-list-item">
              <div className="admin-list-info">
                <h3>{item.name}</h3>
                <span className="badge">Вес: {item.weight}</span>
              </div>
              <div className="admin-list-actions">
                <button className="btn" onClick={() => setGeneratingFor(generatingFor === item.id ? null : item.id)}>
                  Сгенерировать вопросы
                </button>
                <button className="btn" onClick={() => handleEdit(item)}>Изменить</button>
                <button className="btn btn-danger" onClick={() => handleDelete(item.id)}>Удалить</button>
              </div>

              {generatingFor === item.id && (
                <div className="card" style={{ marginTop: '1rem' }}>
                  <h3>Генерация вопросов</h3>
                  <div className="form-field">
                    <label>Количество</label>
                    <input
                      type="number"
                      min={1}
                      max={10}
                      value={generateCount}
                      onChange={(e) => setGenerateCount(parseInt(e.target.value))}
                    />
                  </div>
                   <div className="form-actions">
                    <button
                      className="btn btn-primary"
                      disabled={generating}
                      onClick={() => handleGenerate(item.id)}
                    >
                      {generating ? 'Генерация...' : 'Сгенерировать'}
                    </button>
                  </div>

                  {loadingQuestionsFor === item.id && <p>Загрузка вопросов...</p>}
                  {(() => {
                    const questions = topicQuestions[item.id]
                    if (questions == null) return null
                    if (questions.length === 0) return <p style={{ marginTop: '1rem' }}>Вопросов пока нет.</p>
                    return (
                      <div className="admin-list" style={{ marginTop: '1rem' }}>
                        {questions.map((q) => (
                          <div key={q.id} className="admin-list-item" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <p>{q.questionText}</p>
                            <button className="btn btn-danger" onClick={() => handleDeleteQuestion(item.id, q.id)}>
                              Удалить
                            </button>
                          </div>
                        ))}
                      </div>
                    )
                  })()}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </AdminPageWrapper>
  )
}
