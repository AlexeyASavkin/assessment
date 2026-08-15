import type { Competency, Section, Topic } from '../../../api/admin'
import CompetencyForm from './CompetencyForm'
import SectionForm from './SectionForm'
import TopicForm from './TopicForm'
import type { ModalState } from './types'
import type { CompetencyTreeApi } from './useCompetencyTree'

/**
 * Пропсы модальной формы дерева.
 */
interface TreeModalFormProps {
  tree: CompetencyTreeApi
}

/**
 * Модальное окно создания или редактирования компетенции, секции или темы.
 * Находит редактируемую сущность в загруженных данных дерева и обновляет их после сохранения.
 */
export default function TreeModalForm({ tree }: TreeModalFormProps) {
  const { modal } = tree
  if (!modal.kind) return null

  return (
    <ModalForm
      modal={modal}
      onClose={tree.closeModal}
      onCompetencySaved={async () => {
        await tree.refreshCompetencies()
        tree.closeModal()
      }}
      onSectionSaved={async (competencyId) => {
        await tree.refreshSections(competencyId)
        tree.closeModal()
      }}
      onTopicSaved={async (sectionId) => {
        await tree.refreshTopics(sectionId)
        tree.closeModal()
      }}
      competency={
        modal.kind === 'competency' && modal.mode === 'edit'
          ? tree.competencies.find((c) => c.id === modal.editId)
          : undefined
      }
      section={
        modal.kind === 'section' && modal.mode === 'edit'
          ? Object.values(tree.sectionsByCompetency)
              .flat()
              .find((s) => s.id === modal.editId)
          : undefined
      }
      topic={
        modal.kind === 'topic' && modal.mode === 'edit'
          ? Object.values(tree.topicsBySection)
              .flat()
              .find((t) => t.id === modal.editId)
          : undefined
      }
    />
  )
}

/**
 * Пропсы модальной формы для создания и редактирования элементов дерева.
 */
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

/**
 * Модальное окно с формой для создания или редактирования компетенции, секции или темы.
 */
function ModalForm({
  modal,
  onClose,
  onCompetencySaved,
  onSectionSaved,
  onTopicSaved,
  competency,
  section,
  topic,
}: ModalFormProps) {
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
