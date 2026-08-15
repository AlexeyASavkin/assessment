import { type DragEvent, type RefObject, useRef, useState } from 'react'
import {
  deleteQuestion,
  generateTopicQuestions,
  listTopicQuestions,
  type QuestionBankItem,
  reorderTopicQuestions,
  updateQuestion,
} from '../../../api/admin'
import type { RichTextEditorHandle } from '../../../components/RichTextEditor'

/**
 * API-поверхность хука вопросов темы, передаваемая панели деталей и списку вопросов.
 */
export interface TopicQuestionsApi {
  topicQuestions: QuestionBankItem[]
  generating: boolean
  generateCount: number
  editingQuestionId: string | null
  editQuestionText: string
  savingQuestion: boolean
  dragOverIndex: number | null
  questionEditorRef: RefObject<RichTextEditorHandle | null>
  setGenerateCount: (count: number) => void
  selectTopic: (topicId: string) => void
  generate: (topicId: string) => void
  deleteQuestion: (questionId: string, topicId: string) => void
  startEditQuestion: (question: QuestionBankItem) => void
  cancelEditQuestion: () => void
  saveQuestion: (questionId: string, topicId: string) => void
  dragStart: (e: DragEvent, index: number) => void
  dragOver: (e: DragEvent, index: number) => void
  dragLeave: () => void
  drop: (e: DragEvent, index: number, topicId: string) => void
}

/**
 * Управляет вопросами выбранной темы: загрузка, генерация через ИИ,
 * редактирование, удаление и изменение порядка перетаскиванием.
 * @param setError - колбэк установки ошибки страницы
 */
export function useTopicQuestions(setError: (message: string | null) => void): TopicQuestionsApi {
  const [topicQuestions, setTopicQuestions] = useState<QuestionBankItem[]>([])
  const [generating, setGenerating] = useState(false)
  const [generateCount, setGenerateCount] = useState(2)
  const [dragOverIndex, setDragOverIndex] = useState<number | null>(null)
  const dragIndexRef = useRef<number | null>(null)
  const [editingQuestionId, setEditingQuestionId] = useState<string | null>(null)
  const [editQuestionText, setEditQuestionText] = useState('')
  const [savingQuestion, setSavingQuestion] = useState(false)
  const questionEditorRef = useRef<RichTextEditorHandle>(null)

  /**
   * Загружает вопросы для указанной темы.
   */
  const loadQuestions = async (topicId: string) => {
    try {
      const questions = await listTopicQuestions(topicId)
      setTopicQuestions(questions)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки вопросов')
    }
  }

  /**
   * Выбирает тему: сбрасывает и загружает её вопросы для правой панели.
   */
  const selectTopic = (topicId: string) => {
    setTopicQuestions([])
    loadQuestions(topicId)
  }

  /**
   * Генерирует вопросы для темы через ИИ и обновляет список вопросов.
   */
  const generate = async (topicId: string) => {
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

  /**
   * Удаляет вопрос из темы после подтверждения и обновляет список.
   */
  const handleDeleteQuestion = async (questionId: string, topicId: string) => {
    if (!confirm('Удалить вопрос?')) return
    try {
      await deleteQuestion(questionId)
      await loadQuestions(topicId)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка удаления')
    }
  }

  const startEditQuestion = (q: QuestionBankItem) => {
    setEditingQuestionId(q.id)
    setEditQuestionText(q.questionText)
  }

  const cancelEditQuestion = () => {
    setEditingQuestionId(null)
    setEditQuestionText('')
  }

  const handleSaveQuestion = async (questionId: string, topicId: string) => {
    const content = questionEditorRef.current?.getContent()
    const textToSave = content?.html || editQuestionText.trim()
    if (!textToSave) return
    setSavingQuestion(true)
    try {
      await updateQuestion(questionId, textToSave)
      setEditingQuestionId(null)
      setEditQuestionText('')
      await loadQuestions(topicId)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка сохранения вопроса')
    } finally {
      setSavingQuestion(false)
    }
  }

  /**
   * Начинает перетаскивание вопроса для изменения порядка.
   */
  const handleDragStart = (e: DragEvent, index: number) => {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', String(index))
    dragIndexRef.current = index
  }

  /**
   * Обрабатывает наведение перетаскиваемого вопроса на другую позицию.
   */
  const handleDragOver = (e: DragEvent, index: number) => {
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
    setDragOverIndex(index)
  }

  /**
   * Сбрасывает индикатор перетаскивания при уходе курсора из зоны вопроса.
   */
  const handleDragLeave = () => {
    setDragOverIndex(null)
  }

  /**
   * Завершает перетаскивание вопроса, меняет порядок и сохраняет новый порядок на сервере.
   */
  const handleDrop = async (e: DragEvent, dropIndex: number, topicId: string) => {
    e.preventDefault()
    setDragOverIndex(null)
    const fromIndex = dragIndexRef.current
    dragIndexRef.current = null
    if (fromIndex === null || fromIndex === dropIndex) return

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

  return {
    topicQuestions,
    generating,
    generateCount,
    editingQuestionId,
    editQuestionText,
    savingQuestion,
    dragOverIndex,
    questionEditorRef,
    setGenerateCount,
    selectTopic,
    generate,
    deleteQuestion: handleDeleteQuestion,
    startEditQuestion,
    cancelEditQuestion,
    saveQuestion: handleSaveQuestion,
    dragStart: handleDragStart,
    dragOver: handleDragOver,
    dragLeave: handleDragLeave,
    drop: handleDrop,
  }
}
