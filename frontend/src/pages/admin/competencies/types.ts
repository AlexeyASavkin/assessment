import type { Competency, Section, Topic } from '../../../api/admin'

/**
 * Тип выбранного узла в дереве компетенций.
 */
export type SelectedNode =
  | { type: 'competency'; competency: Competency }
  | { type: 'section'; section: Section; competencyId: string; competencyName: string }
  | { type: 'topic'; topic: Topic; sectionId: string; sectionName: string; competencyName: string }

/**
 * Состояние модального окна для создания или редактирования элемента дерева.
 */
export interface ModalState {
  kind: 'competency' | 'section' | 'topic' | null
  mode: 'create' | 'edit'
  editId?: string
  parentId?: string
}

export const emptyModal: ModalState = { kind: null, mode: 'create' }
