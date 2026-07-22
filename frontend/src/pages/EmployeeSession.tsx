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

export default function EmployeeSession() {
  const { sessionId } = useParams<{ sessionId: string }>()
  const navigate = useNavigate()
  const [currentQuestion, setCurrentQuestion] = useState<Question | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    loadQuestion()
  }, [sessionId])

  const loadQuestion = async () => {
    if (!sessionId) return
    setIsLoading(true)
    setError(null)
    try {
      const data = await getCurrentQuestion(sessionId)
      if (data.completed) {
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

  const handleSubmitAnswer = async (rawTranscript: string, finalTranscript: string) => {
    if (!sessionId || !currentQuestion) return
    setIsLoading(true)
    setError(null)
    try {
      const data = await submitAnswer(sessionId, currentQuestion.questionId, rawTranscript, finalTranscript)
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
