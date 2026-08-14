import type { Competency, Topic } from '../../../api/admin'
import type { CompetencyTreeApi } from './useCompetencyTree'

/**
 * Пропсы узла дерева компетенций.
 */
interface CompetencyTreeNodeProps {
  competency: Competency
  tree: CompetencyTreeApi
  onSelectTopic: (
    topic: Topic,
    sectionId: string,
    sectionName: string,
    competencyName: string,
  ) => void
}

/**
 * Узел дерева компетенций: компетенция с вложенными секциями и темами.
 * Отвечает за раскрытие узлов, выделение и действия (добавление, редактирование, удаление).
 */
export default function CompetencyTreeNode({
  competency,
  tree,
  onSelectTopic,
}: CompetencyTreeNodeProps) {
  const isOpen = tree.expanded.has(competency.id)
  const sections = tree.sectionsByCompetency[competency.id] ?? []
  const isSelected =
    tree.selected?.type === 'competency' && tree.selected.competency.id === competency.id

  return (
    <li className="tree-node">
      <div className={`tree-row ${isSelected ? 'tree-row-selected' : ''}`}>
        <button
          className="tree-toggle"
          onClick={() => tree.handleExpandCompetency(competency)}
          aria-label={isOpen ? 'Свернуть' : 'Развернуть'}
        >
          {isOpen ? '▼' : '▶'}
        </button>
        <button
          className={`tree-label ${isSelected ? 'tree-label-selected' : ''}`}
          onClick={() => tree.selectCompetency(competency)}
          onDoubleClick={(e) => {
            e.stopPropagation()
            tree.handleExpandCompetency(competency)
          }}
        >
          📁 {competency.name}
        </button>
        <div className="tree-actions">
          <button
            className="tree-action-btn"
            title="Добавить секцию"
            onClick={() =>
              tree.openModal({ kind: 'section', mode: 'create', parentId: competency.id })
            }
          >
            +
          </button>
          <button
            className="tree-action-btn"
            title="Редактировать"
            onClick={() =>
              tree.openModal({ kind: 'competency', mode: 'edit', editId: competency.id })
            }
          >
            ✎
          </button>
          <button
            className="tree-action-btn tree-action-danger"
            title="Удалить"
            onClick={() => tree.deleteCompetency(competency.id)}
          >
            ×
          </button>
        </div>
      </div>
      {isOpen && (
        <ul className="tree-children">
          {sections.length === 0 ? (
            <li className="tree-leaf-muted">Секций нет</li>
          ) : (
            sections.map((s) => {
              const sOpen = tree.expanded.has(s.id)
              const topics = tree.topicsBySection[s.id] ?? []
              const sSelected =
                tree.selected?.type === 'section' && tree.selected.section.id === s.id
              return (
                <li key={s.id} className="tree-node">
                  <div className={`tree-row ${sSelected ? 'tree-row-selected' : ''}`}>
                    <button
                      className="tree-toggle"
                      onClick={() => tree.handleExpandSection(s, competency.id, competency.name)}
                      aria-label={sOpen ? 'Свернуть' : 'Развернуть'}
                    >
                      {sOpen ? '▼' : '▶'}
                    </button>
                    <button
                      className={`tree-label ${sSelected ? 'tree-label-selected' : ''}`}
                      onClick={() => tree.handleExpandSection(s, competency.id, competency.name)}
                      onDoubleClick={(e) => {
                        e.stopPropagation()
                        tree.handleExpandSection(s, competency.id, competency.name)
                      }}
                    >
                      📂 {s.name}
                    </button>
                    <div className="tree-actions">
                      <button
                        className="tree-action-btn"
                        title="Добавить тему"
                        onClick={() =>
                          tree.openModal({ kind: 'topic', mode: 'create', parentId: s.id })
                        }
                      >
                        +
                      </button>
                      <button
                        className="tree-action-btn"
                        title="Редактировать"
                        onClick={() =>
                          tree.openModal({
                            kind: 'section',
                            mode: 'edit',
                            editId: s.id,
                            parentId: competency.id,
                          })
                        }
                      >
                        ✎
                      </button>
                      <button
                        className="tree-action-btn tree-action-danger"
                        title="Удалить"
                        onClick={() => tree.deleteSection(s.id, competency.id)}
                      >
                        ×
                      </button>
                    </div>
                  </div>
                  {sOpen && (
                    <ul className="tree-children">
                      {topics.length === 0 ? (
                        <li className="tree-leaf-muted">Тем нет</li>
                      ) : (
                        topics.map((t) => {
                          const tSelected =
                            tree.selected?.type === 'topic' && tree.selected.topic.id === t.id
                          return (
                            <li key={t.id} className="tree-node">
                              <div className={`tree-row ${tSelected ? 'tree-row-selected' : ''}`}>
                                <span className="tree-toggle-placeholder" />
                                <button
                                  className={`tree-label ${tSelected ? 'tree-label-selected' : ''}`}
                                  onClick={() => onSelectTopic(t, s.id, s.name, competency.name)}
                                >
                                  📄 {t.name}
                                </button>
                                <div className="tree-actions">
                                  <button
                                    className="tree-action-btn"
                                    title="Редактировать"
                                    onClick={() =>
                                      tree.openModal({
                                        kind: 'topic',
                                        mode: 'edit',
                                        editId: t.id,
                                        parentId: s.id,
                                      })
                                    }
                                  >
                                    ✎
                                  </button>
                                  <button
                                    className="tree-action-btn tree-action-danger"
                                    title="Удалить"
                                    onClick={() => tree.deleteTopic(t.id, s.id)}
                                  >
                                    ×
                                  </button>
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
}
