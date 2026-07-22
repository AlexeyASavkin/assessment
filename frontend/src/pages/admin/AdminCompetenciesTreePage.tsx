import { useState, useEffect, useCallback, useRef } from 'react'
import {
  listCompetencies,
  listSections,
  listTopics,
  listTopicQuestions,
  createCompetency,
  updateCompetency,
  deleteCompetency,
  createSection,
  updateSection,
  deleteSection,
  createTopic,
  updateTopic,
  deleteTopic,
  generateTopicQuestions,
  deleteQuestion,
  reorderTopicQuestions,
  type Competency,
  type Section,
  type Topic,
  type QuestionBankItem,
} from '../../api/admin'
import { AdminPageWrapper } from '../../components/admin/AdminLayout'

type SelectedNode =
  | { type: 'competency'; competency: Competency }
  | { type: 'section'; section: Section; competencyId: string; competencyName: string }
  | { type: 'topic'; topic: Topic; sectionId: string; sectionName: string; competencyName: string }

interface ModalState {
  kind: 'competency' | 'section' | 'topic' | null
  mode: 'create' | 'edit'
  editId?: string
  parentId?: string
}

const emptyModal: ModalState = { kind: null, mode: 'create' }

export default function AdminCompetenciesTreePage() {
  const [competencies, setCompetencies] = useState<Competency[]>([])
  const [expanded, setExpanded] = useState<Set<string>>(new Set())
  const [selected, setSelected] = useState<SelectedNode | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [generating, setGenerating] = useState(false)
  const [generateCount, setGenerateCount] = useState(2)
  const [topicQuestions, setTopicQuestions] = useState<QuestionBankItem[]>([])
  const [modal, setModal] = useState<ModalState>(emptyModal)
  const [dragOverIndex, setDragOverIndex] = useState<number | null>(null)
  const dragIndexRef = useRef<number | null>(null)

  const loadCompetencies = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await listCompetencies()
      setCompetencies(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки')
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    loadCompetencies()
  }, [loadCompetencies])

  const toggleExpand = (id: string) => {
    setExpanded((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  // ---- Section loading ----
  const [sectionsByCompetency, setSectionsByCompetency] = useState<Record<string, Section[]>>({})
  const loadSections = useCallback(async (competencyId: string) => {
    try {
      const sections = await listSections(competencyId)
      setSectionsByCompetency((prev) => ({ ...prev, [competencyId]: sections }))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки секций')
    }
  }, [])

  // ---- Topic loading ----
  const [topicsBySection, setTopicsBySection] = useState<Record<string, Topic[]>>({})
  const loadTopics = useCallback(async (sectionId: string) => {
    try {
      const topics = await listTopics(sectionId)
      setTopicsBySection((prev) => ({ ...prev, [sectionId]: topics }))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки тем')
    }
  }, [])

  const handleExpandCompetency = (competency: Competency) => {
    toggleExpand(competency.id)
    if (!sectionsByCompetency[competency.id]) {
      loadSections(competency.id)
    }
  }

  const handleExpandSection = (section: Section, competencyId: string, competencyName: string) => {
    toggleExpand(section.id)
    if (!topicsBySection[section.id]) {
      loadTopics(section.id)
    }
    setSelected({ type: 'section', section, competencyId, competencyName })
  }

  const refreshSections = async (competencyId: string) => {
    await loadSections(competencyId)
  }

  const refreshTopics = async (sectionId: string) => {
    await loadTopics(sectionId)
  }

  const refreshCompetencies = async () => {
    await loadCompetencies()
  }

  // ---- Question handling ----
  const loadQuestions = async (topicId: string) => {
    try {
      const questions = await listTopicQuestions(topicId)
      setTopicQuestions(questions)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки вопросов')
    }
  }

  const handleSelectTopic = (topic: Topic, sectionId: string, sectionName: string, competencyName: string) => {
    setSelected({ type: 'topic', topic, sectionId, sectionName, competencyName })
    setTopicQuestions([])
    loadQuestions(topic.id)
  }

  const handleGenerate = async (topicId: string) => {
    setGenerating(true)
    setError(null)
    try {
      await generateTopicQuestions(topicId, generateCount, 'ALL')
      await loadQuestions(topicId)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка генерации')
    } finally {
      setGenerating(false)
    }
  }

  const handleDeleteQuestion = async (questionId: string, topicId: string) => {
    if (!confirm('Удалить вопрос?')) return
    try {
      await deleteQuestion(questionId)
      await loadQuestions(topicId)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка удаления')
    }
  }

  // ---- Delete handlers ----
  const handleDeleteCompetency = async (id: string) => {
    if (!confirm('Удалить компетенцию вместе со всеми секциями и темами?')) return
    try {
      await deleteCompetency(id)
      if (selected?.type === 'competency' && selected.competency.id === id) setSelected(null)
      await refreshCompetencies()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка удаления')
    }
  }

  const handleDeleteSection = async (id: string, competencyId: string) => {
    if (!confirm('Удалить секцию вместе со всеми темами?')) return
    try {
      await deleteSection(id)
      if (selected?.type === 'section' && selected.section.id === id) setSelected(null)
      await refreshSections(competencyId)
      setSectionsByCompetency((prev) => {
        const next = { ...prev }
        delete next[competencyId]
        return next
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка удаления')
    }
  }

  const handleDeleteTopic = async (id: string, sectionId: string) => {
    if (!confirm('Удалить тему вместе со всеми вопросами?')) return
    try {
      await deleteTopic(id)
      if (selected?.type === 'topic' && selected.topic.id === id) setSelected(null)
      await refreshTopics(sectionId)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка удаления')
    }
  }

  // ---- Drag-and-drop for questions ----
  const handleDragStart = (e: React.DragEvent, index: number) => {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', String(index))
    dragIndexRef.current = index
  }

  const handleDragOver = (e: React.DragEvent, index: number) => {
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
    setDragOverIndex(index)
  }

  const handleDragLeave = () => {
    setDragOverIndex(null)
  }

  const handleDrop = async (e: React.DragEvent, dropIndex: number) => {
    e.preventDefault()
    setDragOverIndex(null)
    const fromIndex = dragIndexRef.current
    dragIndexRef.current = null
    if (fromIndex === null || fromIndex === dropIndex) return

    const topicId = selected?.type === 'topic' ? selected.topic.id : null
    if (!topicId) return

    const reordered = [...topicQuestions]
    const [moved] = reordered.splice(fromIndex, 1)
    if (!moved) return
    reordered.splice(dropIndex, 0, moved)
    setTopicQuestions(reordered)

    const orderedIds = reordered.map((q) => q.id)
    try {
      await reorderTopicQuestions(topicId, orderedIds)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка изменения порядка')
      loadQuestions(topicId)
    }
  }

  // ---- Right panel ----
  const renderDetail = () => {
    if (!selected) {
      return (
        <div className="tree-detail-empty">
          <p>Выберите элемент слева для просмотра деталей</p>
        </div>
      )
    }

    if (selected.type === 'competency') {
      const c = selected.competency
      return (
        <div className="tree-detail">
          <div className="tree-detail-header">
            <h2>{c.name}</h2>
          </div>
          {c.description && <p className="tree-detail-desc">{c.description}</p>}
          <div className="tree-detail-stats">
            <span className="badge">Секций: {sectionsByCompetency[c.id]?.length ?? '—'}</span>
          </div>
        </div>
      )
    }

    if (selected.type === 'section') {
      const s = selected.section
      const topics = topicsBySection[s.id] ?? []
      return (
        <div className="tree-detail">
          <div className="tree-detail-header">
            <h2>{s.name}</h2>
          </div>
          <div className="tree-detail-stats">
            <span className="badge">Порядок: {s.sortOrder}</span>
            <span className="badge">Тем: {topics.length}</span>
          </div>

          <h3 className="tree-detail-subtitle">Темы</h3>
          {topics.length === 0 ? (
            <p className="tree-detail-empty">Тем пока нет.</p>
          ) : (
            <div className="admin-list">
              {topics.map((t) => (
                <div key={t.id} className="card admin-list-item">
                  <div className="admin-list-info">
                    <h3>{t.name}</h3>
                    <span className="badge">Вес: {t.weight}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )
    }

    if (selected.type === 'topic') {
      const t = selected.topic
      const breadcrumb = `${selected.competencyName} › ${selected.sectionName}`
      return (
        <div className="tree-detail">
          <div className="tree-detail-header">
            <div>
              <p className="tree-detail-breadcrumb">{breadcrumb}</p>
              <h2>{t.name}</h2>
            </div>
          </div>
          <div className="tree-detail-stats">
            <span className="badge">Порядок: {t.sortOrder}</span>
            <span className="badge">Вес: {t.weight}</span>
            <span className="badge">Вопросов: {topicQuestions.length}</span>
          </div>

          <h3 className="tree-detail-subtitle">Генерация вопросов</h3>
          <div className="tree-gen-row">
            <label>Количество</label>
            <input
              type="number"
              min={1}
              max={10}
              value={generateCount}
              onChange={(e) => setGenerateCount(parseInt(e.target.value) || 1)}
              className="tree-gen-input"
            />
            <button className="btn btn-primary" disabled={generating} onClick={() => handleGenerate(t.id)}>
              {generating ? 'Генерация...' : 'Сгенерировать'}
            </button>
          </div>

          <h3 className="tree-detail-subtitle">Вопросы</h3>
          {topicQuestions.length === 0 ? (
            <p className="tree-detail-empty">Вопросов пока нет.</p>
          ) : (
            <div className="admin-list">
              {topicQuestions.map((q, index) => (
                <div
                  key={q.id}
                  className={`card admin-list-item draggable-question ${dragOverIndex === index ? 'drag-over' : ''}`}
                  draggable
                  onDragStart={(e) => handleDragStart(e, index)}
                  onDragOver={(e) => handleDragOver(e, index)}
                  onDragLeave={handleDragLeave}
                  onDrop={(e) => handleDrop(e, index)}
                >
                  <div className="drag-handle" title="Перетащить">⠿</div>
                  <div className="admin-list-info">
                    <p>{q.questionText}</p>
                  </div>
                  <button className="btn btn-danger" onClick={() => handleDeleteQuestion(q.id, t.id)}>
                    Удалить
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      )
    }

    return null
  }

  // ---- Modal form ----
  const renderModal = () => {
    if (!modal.kind) return null

    return (
      <ModalForm
        modal={modal}
        onClose={() => setModal(emptyModal)}
        onCompetencySaved={async () => {
          await refreshCompetencies()
          setModal(emptyModal)
        }}
        onSectionSaved={async (competencyId) => {
          await refreshSections(competencyId)
          setModal(emptyModal)
        }}
        onTopicSaved={async (sectionId) => {
          await refreshTopics(sectionId)
          setModal(emptyModal)
        }}
        competency={modal.kind === 'competency' && modal.mode === 'edit' ? competencies.find((c) => c.id === modal.editId) : undefined}
        section={modal.kind === 'section' && modal.mode === 'edit'
          ? Object.values(sectionsByCompetency).flat().find((s) => s.id === modal.editId)
          : undefined}
        topic={modal.kind === 'topic' && modal.mode === 'edit'
          ? Object.values(topicsBySection).flat().find((t) => t.id === modal.editId)
          : undefined}
      />
    )
  }

  return (
    <AdminPageWrapper>
      <div className="tree-page">
        <div className="tree-header">
          <h1>Компетенции</h1>
          <button className="btn btn-primary" onClick={() => setModal({ kind: 'competency', mode: 'create' })}>
            + Компетенцию
          </button>
        </div>

        {error && <p className="error-text">{error}</p>}

        <div className="tree-layout">
          <div className="tree-sidebar">
            {isLoading ? (
              <p>Загрузка...</p>
            ) : competencies.length === 0 ? (
              <p className="tree-empty">Компетенций пока нет.</p>
            ) : (
              <ul className="tree-list">
                {competencies.map((c) => {
                  const isOpen = expanded.has(c.id)
                  const sections = sectionsByCompetency[c.id] ?? []
                  const isSelected = selected?.type === 'competency' && selected.competency.id === c.id
                  return (
                    <li key={c.id} className="tree-node">
                      <div className={`tree-row ${isSelected ? 'tree-row-selected' : ''}`}>
                        <button
                          className="tree-toggle"
                          onClick={() => handleExpandCompetency(c)}
                          aria-label={isOpen ? 'Свернуть' : 'Развернуть'}
                        >
                          {isOpen ? '▼' : '▶'}
                        </button>
                        <button
                          className={`tree-label ${isSelected ? 'tree-label-selected' : ''}`}
                          onClick={() => setSelected({ type: 'competency', competency: c })}
                          onDoubleClick={(e) => { e.stopPropagation(); handleExpandCompetency(c) }}
                        >
                          📁 {c.name}
                        </button>
                        <div className="tree-actions">
                          <button className="tree-action-btn" title="Добавить секцию" onClick={() => setModal({ kind: 'section', mode: 'create', parentId: c.id })}>+</button>
                          <button className="tree-action-btn" title="Редактировать" onClick={() => setModal({ kind: 'competency', mode: 'edit', editId: c.id })}>✎</button>
                          <button className="tree-action-btn tree-action-danger" title="Удалить" onClick={() => handleDeleteCompetency(c.id)}>×</button>
                        </div>
                      </div>
                      {isOpen && (
                        <ul className="tree-children">
                          {sections.length === 0 ? (
                            <li className="tree-leaf-muted">Секций нет</li>
                          ) : (
                            sections.map((s) => {
                              const sOpen = expanded.has(s.id)
                              const topics = topicsBySection[s.id] ?? []
                              const sSelected = selected?.type === 'section' && selected.section.id === s.id
                              return (
                                <li key={s.id} className="tree-node">
                                  <div className={`tree-row ${sSelected ? 'tree-row-selected' : ''}`}>
                                    <button
                                      className="tree-toggle"
                                      onClick={() => handleExpandSection(s, c.id, c.name)}
                                      aria-label={sOpen ? 'Свернуть' : 'Развернуть'}
                                    >
                                      {sOpen ? '▼' : '▶'}
                                    </button>
                                    <button
                                      className={`tree-label ${sSelected ? 'tree-label-selected' : ''}`}
                                      onClick={() => { handleExpandSection(s, c.id, c.name); setSelected({ type: 'section', section: s, competencyId: c.id, competencyName: c.name }) }}
                                      onDoubleClick={(e) => { e.stopPropagation(); handleExpandSection(s, c.id, c.name) }}
                                    >
                                      📂 {s.name}
                                    </button>
                                    <div className="tree-actions">
                                      <button className="tree-action-btn" title="Добавить тему" onClick={() => setModal({ kind: 'topic', mode: 'create', parentId: s.id })}>+</button>
                                      <button className="tree-action-btn" title="Редактировать" onClick={() => setModal({ kind: 'section', mode: 'edit', editId: s.id, parentId: c.id })}>✎</button>
                                      <button className="tree-action-btn tree-action-danger" title="Удалить" onClick={() => handleDeleteSection(s.id, c.id)}>×</button>
                                    </div>
                                  </div>
                                  {sOpen && (
                                    <ul className="tree-children">
                                      {topics.length === 0 ? (
                                        <li className="tree-leaf-muted">Тем нет</li>
                                      ) : (
                                        topics.map((t) => {
                                          const tSelected = selected?.type === 'topic' && selected.topic.id === t.id
                                          return (
                                            <li key={t.id} className="tree-node">
                                              <div className={`tree-row ${tSelected ? 'tree-row-selected' : ''}`}>
                                                <span className="tree-toggle-placeholder" />
                                                <button
                                                  className={`tree-label ${tSelected ? 'tree-label-selected' : ''}`}
                                                  onClick={() => handleSelectTopic(t, s.id, s.name, c.name)}
                                                >
                                                  📄 {t.name}
                                                </button>
                                                <div className="tree-actions">
                                                  <button className="tree-action-btn" title="Редактировать" onClick={() => setModal({ kind: 'topic', mode: 'edit', editId: t.id, parentId: s.id })}>✎</button>
                                                  <button className="tree-action-btn tree-action-danger" title="Удалить" onClick={() => handleDeleteTopic(t.id, s.id)}>×</button>
                                                </div>
                                              </div>
                                            </li>
                                          )
                                        })
                                      )}
                                    </ul>
                                  )}
                                </li>
                              )
                            })
                          )}
                        </ul>
                      )}
                    </li>
                  )
                })}
              </ul>
            )}
          </div>

          <div className="tree-detail-panel">
            {renderDetail()}
          </div>
        </div>

        {renderModal()}
      </div>
    </AdminPageWrapper>
  )
}

// ---- Modal form component ----

interface ModalFormProps {
  modal: ModalState
  onClose: () => void
  onCompetencySaved: () => Promise<void>
  onSectionSaved: (competencyId: string) => Promise<void>
  onTopicSaved: (sectionId: string) => Promise<void>
  competency?: Competency
  section?: Section
  topic?: Topic
}

function ModalForm({ modal, onClose, onCompetencySaved, onSectionSaved, onTopicSaved, competency, section, topic }: ModalFormProps) {
  if (!modal.kind) return null

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        {modal.kind === 'competency' && (
          <CompetencyForm
            editCompetency={modal.mode === 'edit' ? competency : undefined}
            onCancel={onClose}
            onSaved={onCompetencySaved}
          />
        )}
        {modal.kind === 'section' && (
          <SectionForm
            editSection={modal.mode === 'edit' ? section : undefined}
            parentId={modal.parentId}
            onCancel={onClose}
            onSaved={onSectionSaved}
          />
        )}
        {modal.kind === 'topic' && (
          <TopicForm
            editTopic={modal.mode === 'edit' ? topic : undefined}
            parentId={modal.parentId}
            onCancel={onClose}
            onSaved={onTopicSaved}
          />
        )}
      </div>
    </div>
  )
}

// ---- Competency form ----

function CompetencyForm({ editCompetency, onCancel, onSaved }: {
  editCompetency?: Competency
  onCancel: () => void
  onSaved: () => Promise<void>
}) {
  const [name, setName] = useState(editCompetency?.name ?? '')
  const [description, setDescription] = useState(editCompetency?.description ?? '')
  const [saving, setSaving] = useState(false)
  const [err, setErr] = useState<string | null>(null)

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
        <button type="button" className="btn" onClick={onCancel}>Отмена</button>
      </div>
    </form>
  )
}

// ---- Section form ----

function SectionForm({ editSection, parentId, onCancel, onSaved }: {
  editSection?: Section
  parentId?: string
  onCancel: () => void
  onSaved: (competencyId: string) => Promise<void>
}) {
  const [name, setName] = useState(editSection?.name ?? '')
  const [sortOrder, setSortOrder] = useState(editSection?.sortOrder ?? 0)
  const [saving, setSaving] = useState(false)
  const [err, setErr] = useState<string | null>(null)

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setErr(null)
    try {
      if (editSection) {
        await updateSection(editSection.id, name, sortOrder)
        await onSaved(parentId ?? '')
      } else {
        if (!parentId) { setErr('Не указана компетенция'); return }
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
        <input type="number" value={sortOrder} onChange={(e) => setSortOrder(parseInt(e.target.value) || 0)} />
      </div>
      <div className="form-actions">
        <button type="submit" className="btn btn-primary" disabled={saving}>
          {saving ? 'Сохранение...' : 'Сохранить'}
        </button>
        <button type="button" className="btn" onClick={onCancel}>Отмена</button>
      </div>
    </form>
  )
}

// ---- Topic form ----

function TopicForm({ editTopic, parentId, onCancel, onSaved }: {
  editTopic?: Topic
  parentId?: string
  onCancel: () => void
  onSaved: (sectionId: string) => Promise<void>
}) {
  const [name, setName] = useState(editTopic?.name ?? '')
  const [weight, setWeight] = useState(editTopic?.weight ?? 1)
  const [sortOrder, setSortOrder] = useState(editTopic?.sortOrder ?? 0)
  const [saving, setSaving] = useState(false)
  const [err, setErr] = useState<string | null>(null)

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setErr(null)
    try {
      if (editTopic) {
        await updateTopic(editTopic.id, name, weight, sortOrder)
        await onSaved(parentId ?? '')
      } else {
        if (!parentId) { setErr('Не указана секция'); return }
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
        <input type="number" value={sortOrder} onChange={(e) => setSortOrder(parseInt(e.target.value) || 0)} />
      </div>
      <div className="form-field">
        <label>Вес</label>
        <input type="number" step="0.1" min="0" value={weight} onChange={(e) => setWeight(parseFloat(e.target.value) || 1)} />
      </div>
      <div className="form-actions">
        <button type="submit" className="btn btn-primary" disabled={saving}>
          {saving ? 'Сохранение...' : 'Сохранить'}
        </button>
        <button type="button" className="btn" onClick={onCancel}>Отмена</button>
      </div>
    </form>
  )
}