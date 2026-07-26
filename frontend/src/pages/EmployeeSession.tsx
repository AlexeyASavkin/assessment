import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import QuestionDisplay from '../components/QuestionDisplay'
import { getCurrentQuestion, submitAnswer } from '../api/employee'

interface Question {
  questionId: string
  questionText: string
  topicId: string
  isFollowUp: boolean
  completed?: boolean
  followupParentId?: string
}

/**
 * Страница сессии оценки сотрудника.
 * Показывает текущий вопрос, поддерживает голосовой ввод через SpeechRecognition API,
 * отправляет ответы на сервер. При завершении сессии перенаправляет на страницу отчёта.
 * Автоматически озвучивает текст вопроса через SpeechSynthesis.
 */
export default function EmployeeSession() {
  const { sessionId } = useParams<{ sessionId: string }>()
  const navigate = useNavigate()
  /** Текущий вопрос сессии, получаемый с сервера */
  const [currentQuestion, setCurrentQuestion] = useState<Question | null>(null)
  /** Флаг загрузки данных с сервера */
  const [isLoading, setIsLoading] = useState(false)
  /** Сообщение об ошибке при загрузке или отправке данных */
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    loadQuestion()
  }, [sessionId])

  /**
   * Загружает текущий вопрос с сервера.
   * Если сессия завершена, перенаправляет на страницу отчёта.
   */
  const loadQuestion = async () => {
    if (!sessionId) return
    setIsLoading(true)
    setError(null)
    try {
      const data = await getCurrentQuestion(sessionId)
      if (data.completed) {
        navigate(`/session/${sessionId}/report`)
      } else if (data.error === 'Session completed') {
        navigate(`/session/${sessionId}/report`)
      } else {
        setCurrentQuestion(data)
      }
    } catch (err) {
      setError('Ошибка загрузки вопроса')
    } finally {
      setIsLoading(false)
    }
  }

  /**
   * Отправляет ответ сотрудника на сервер и загружает следующий вопрос.
   * При завершении сессии перенаправляет на страницу отчёта.
   * @param finalTranscript - Отредактированный итоговый текст ответа
   */
  const handleSubmitAnswer = async (finalTranscript: string) => {
    if (!sessionId || !currentQuestion) return
    setIsLoading(true)
    setError(null)
    try {
      const data = await submitAnswer(sessionId, currentQuestion.questionId, finalTranscript)
      if (data.completed) {
        navigate(`/session/${sessionId}/report`)
      } else {
        setCurrentQuestion({
          questionId: data.nextQuestionId,
          questionText: data.nextQuestionText || '',
          topicId: data.topicId || '',
          isFollowUp: data.isFollowUp,
          followupParentId: data.followupParentId,
        })
      }
    } catch (err) {
      setError('Ошибка отправки ответа')
    } finally {
      setIsLoading(false)
    }
  }

  if (error) {
    return (
      <div className="container">
        <div className="card">
          <h2>Ошибка</h2>
          <p>{error}</p>
          <button className="btn btn-primary" onClick={loadQuestion}>Попробовать снова</button>
        </div>
      </div>
    )
  }

  if (!currentQuestion) {
    return (
      <div className="container">
        <p>Загрузка...</p>
      </div>
    )
  }

  return (
    <div className="container">
      <h1>Ассессмент компетенций</h1>
      <QuestionDisplay
        questionText={currentQuestion.questionText}
        isFollowUp={currentQuestion.isFollowUp}
        onSubmit={handleSubmitAnswer}
        isLoading={isLoading}
      />
    </div>
  )
}
