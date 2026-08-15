import type { Section, Topic } from '../../../api/admin'
import QuestionListWithDnd from './QuestionListWithDnd'
import type { SelectedNode } from './types'
import type { TopicQuestionsApi } from './useTopicQuestions'

/**
 * Пропсы панели деталей выбранного элемента дерева.
 */
interface TopicDetailPanelProps {
  selected: SelectedNode | null
  sectionsByCompetency: Record<string, Section[]>
  topicsBySection: Record<string, Topic[]>
  questions: TopicQuestionsApi
}

/**
 * Отображает детальную информацию о выбранном элементе дерева в правой панели:
 * компетенцию, секцию или тему с генерацией и списком вопросов.
 */
export default function TopicDetailPanel({
  selected,
  sectionsByCompetency,
  topicsBySection,
  questions,
}: TopicDetailPanelProps) {
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
        <span className="badge">Вопросов: {questions.topicQuestions.length}</span>
      </div>

      <h3 className="tree-detail-subtitle">Генерация вопросов</h3>
      <div className="tree-gen-row">
        <label>Количество</label>
        <input
          type="number"
          min={1}
          max={10}
          value={questions.generateCount}
          onChange={(e) => questions.setGenerateCount(parseInt(e.target.value, 10) || 1)}
          className="tree-gen-input"
        />
        <button
          className="btn btn-primary"
          disabled={questions.generating}
          onClick={() => questions.generate(t.id)}
        >
          {questions.generating ? 'Генерация...' : 'Сгенерировать'}
        </button>
      </div>

      <h3 className="tree-detail-subtitle">Вопросы</h3>
      <QuestionListWithDnd topicId={t.id} questions={questions} />
    </div>
  )
}
