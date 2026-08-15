import { useCallback, useEffect, useState } from 'react'
import {
  type Competency,
  deleteCompetency,
  deleteSection,
  deleteTopic,
  listCompetencies,
  listSections,
  listTopics,
  type Section,
  type Topic,
} from '../../../api/admin'
import { emptyModal, type ModalState, type SelectedNode } from './types'

/**
 * API-поверхность хука дерева компетенций, передаваемая компонентам дерева и модалки.
 */
export interface CompetencyTreeApi {
  competencies: Competency[]
  expanded: Set<string>
  selected: SelectedNode | null
  isLoading: boolean
  error: string | null
  modal: ModalState
  sectionsByCompetency: Record<string, Section[]>
  topicsBySection: Record<string, Topic[]>
  handleExpandCompetency: (competency: Competency) => void
  handleExpandSection: (section: Section, competencyId: string, competencyName: string) => void
  selectCompetency: (competency: Competency) => void
  selectTopic: (
    topic: Topic,
    sectionId: string,
    sectionName: string,
    competencyName: string,
  ) => void
  openModal: (modal: ModalState) => void
  closeModal: () => void
  setError: (message: string | null) => void
  refreshCompetencies: () => Promise<void>
  refreshSections: (competencyId: string) => Promise<void>
  refreshTopics: (sectionId: string) => Promise<void>
  deleteCompetency: (id: string) => Promise<void>
  deleteSection: (id: string, competencyId: string) => Promise<void>
  deleteTopic: (id: string, sectionId: string) => Promise<void>
}

/**
 * Управляет состоянием дерева компетенций: список компетенций, секций и тем,
 * раскрытие узлов, выделение, загрузка данных и CRUD-операции.
 */
export function useCompetencyTree(): CompetencyTreeApi {
  const [competencies, setCompetencies] = useState<Competency[]>([])
  const [expanded, setExpanded] = useState<Set<string>>(new Set())
  const [selected, setSelected] = useState<SelectedNode | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [modal, setModal] = useState<ModalState>(emptyModal)
  const [sectionsByCompetency, setSectionsByCompetency] = useState<Record<string, Section[]>>({})
  const [topicsBySection, setTopicsBySection] = useState<Record<string, Topic[]>>({})

  /**
   * Загружает список компетенций с сервера.
   */
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

  /**
   * Загружает секции для указанной компетенции.
   */
  const loadSections = useCallback(async (competencyId: string) => {
    try {
      const sections = await listSections(competencyId)
      setSectionsByCompetency((prev) => ({ ...prev, [competencyId]: sections }))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки секций')
    }
  }, [])

  /**
   * Загружает темы для указанной секции.
   */
  const loadTopics = useCallback(async (sectionId: string) => {
    try {
      const topics = await listTopics(sectionId)
      setTopicsBySection((prev) => ({ ...prev, [sectionId]: topics }))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки тем')
    }
  }, [])

  /**
   * Разворачивает или сворачивает компетенцию и загружает её секции при первом раскрытии.
   */
  const handleExpandCompetency = (competency: Competency) => {
    toggleExpand(competency.id)
    if (!sectionsByCompetency[competency.id]) {
      loadSections(competency.id)
    }
  }

  /**
   * Разворачивает или сворачивает секцию, загружает темы при первом раскрытии и выбирает секцию.
   */
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

  /**
   * Удаляет компетенцию вместе со всеми секциями и темами после подтверждения.
   */
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

  /**
   * Удаляет секцию вместе со всеми темами после подтверждения.
   */
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

  /**
   * Удаляет тему вместе со всеми вопросами после подтверждения.
   */
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

  return {
    competencies,
    expanded,
    selected,
    isLoading,
    error,
    modal,
    sectionsByCompetency,
    topicsBySection,
    handleExpandCompetency,
    handleExpandSection,
    selectCompetency: (competency) => setSelected({ type: 'competency', competency }),
    selectTopic: (topic, sectionId, sectionName, competencyName) =>
      setSelected({ type: 'topic', topic, sectionId, sectionName, competencyName }),
    openModal: setModal,
    closeModal: () => setModal(emptyModal),
    setError,
    refreshCompetencies,
    refreshSections,
    refreshTopics,
    deleteCompetency: handleDeleteCompetency,
    deleteSection: handleDeleteSection,
    deleteTopic: handleDeleteTopic,
  }
}
