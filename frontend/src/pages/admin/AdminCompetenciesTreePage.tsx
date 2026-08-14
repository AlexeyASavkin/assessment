import { AdminPageWrapper } from '../../components/admin/AdminLayout'
import Loader from '../../components/Loader'
import CompetencyTreeNode from './competencies/CompetencyTreeNode'
import TopicDetailPanel from './competencies/TopicDetailPanel'
import TreeModalForm from './competencies/TreeModalForm'
import { useCompetencyTree } from './competencies/useCompetencyTree'
import { useTopicQuestions } from './competencies/useTopicQuestions'

/**
 * Страница управления деревом компетенций.
 * Отображает иерархию компетенций, секций и тем с возможностью создания, редактирования, удаления,
 * генерации вопросов через ИИ и изменения порядка вопросов перетаскиванием.
 */
export default function AdminCompetenciesTreePage() {
  const tree = useCompetencyTree()
  const questions = useTopicQuestions(tree.setError)

  /**
   * Выбирает тему в дереве и загружает её вопросы для отображения в правой панели.
   */
  const handleSelectTopic = (
    topic: Parameters<typeof tree.selectTopic>[0],
    sectionId: string,
    sectionName: string,
    competencyName: string,
  ) => {
    tree.selectTopic(topic, sectionId, sectionName, competencyName)
    questions.selectTopic(topic.id)
  }

  return (
    <AdminPageWrapper>
      {questions.generating && (
        <Loader
          overlay
          text="Генерация вопросов..."
          subtext="ИИ создаёт вопросы для темы. Это может занять примерно минуту."
        />
      )}
      <div className="tree-page">
        <div className="tree-header">
          <h1>Компетенции</h1>
          <button
            className="btn btn-primary"
            onClick={() => tree.openModal({ kind: 'competency', mode: 'create' })}
          >
            + Компетенцию
          </button>
        </div>

        {tree.error && <p className="error-text">{tree.error}</p>}

        <div className="tree-layout">
          <div className="tree-sidebar">
            {tree.isLoading ? (
              <p>Загрузка...</p>
            ) : tree.competencies.length === 0 ? (
              <p className="tree-empty">Компетенций пока нет.</p>
            ) : (
              <ul className="tree-list">
                {tree.competencies.map((c) => (
                  <CompetencyTreeNode
                    key={c.id}
                    competency={c}
                    tree={tree}
                    onSelectTopic={handleSelectTopic}
                  />
                ))}
              </ul>
            )}
          </div>

          <div className="tree-detail-panel">
            <TopicDetailPanel
              selected={tree.selected}
              sectionsByCompetency={tree.sectionsByCompetency}
              topicsBySection={tree.topicsBySection}
              questions={questions}
            />
          </div>
        </div>

        <TreeModalForm tree={tree} />
      </div>
    </AdminPageWrapper>
  )
}
