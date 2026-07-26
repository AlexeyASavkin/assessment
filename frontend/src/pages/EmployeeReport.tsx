import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'

interface CompetencyReport {
  topicId: string
  topicName: string
  sectionName: string
  competencyName: string
  averageScore: number
  passed: boolean
  followUpScores: number[]
  feedbacks: string[]
}

interface Report {
  sessionId: string
  employeeName: string
  competencies: CompetencyReport[]
  passed: boolean
  overallRecommendation: string
}

/**
 * Страница итогового отчёта по оценке компетенций.
 * Отображает баллы по каждой компетенции, общий уровень и рекомендации ИИ.
 * Обрабатывает 403 (редирект на сессию, если оценка не завершена) и 404 (формирование отчёта).
 */
export default function EmployeeReport() {
  const { sessionId } = useParams<{ sessionId: string }>()
  const navigate = useNavigate()
  /** Данные итогового отчёта, полученные с сервера */
  const [report, setReport] = useState<Report | null>(null)
  /** Флаг загрузки отчёта с сервера */
  const [isLoading, setIsLoading] = useState(true)
  /** Сообщение об ошибке при загрузке отчёта */
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    loadReport()
  }, [sessionId])

  /**
   * Загружает итоговый отчёт с сервера.
   * При статусе 403 перенаправляет на страницу сессии (оценка ещё не завершена).
   */
  const loadReport = async () => {
    if (!sessionId) return
    setIsLoading(true)
    setError(null)
    try {
      const response = await fetch(`/api/employee/sessions/${sessionId}/report`, { credentials: 'include' })
      if (response.status === 403) {
        navigate(`/session/${sessionId}`)
        return
      }
      if (!response.ok) throw new Error('Ошибка загрузки отчёта')
      const data = await response.json()
      setReport(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки отчёта')
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
          <button className="btn btn-primary" onClick={loadReport}>Попробовать снова</button>
        </div>
      </div>
    )
  }

  if (isLoading || !report) {
    return (
      <div className="container">
        <p>Загрузка отчёта...</p>
      </div>
    )
  }

  return (
    <div className="container">
      <h1>Результаты ассессмента</h1>
      <p><strong>Сотрудник:</strong> {report.employeeName}</p>

      <div className="card">
        <h2>Результат: <span style={{ color: report.passed ? '#28a745' : '#dc3545' }}>
          {report.passed ? 'Пройден' : 'Не пройден'}
        </span></h2>
        <p>{report.overallRecommendation}</p>
      </div>

      <h2>Оценки по темам</h2>
      {report.competencies.map((comp) => (
        <div key={comp.topicId} className="card">
          <h3>{comp.competencyName} — {comp.sectionName} — {comp.topicName}</h3>
          <p><strong>Средний балл:</strong> {comp.averageScore}</p>
          <p><strong>Результат:</strong> <span style={{ color: comp.passed ? '#28a745' : '#dc3545' }}>
            {comp.passed ? 'Пройден' : 'Не пройден'}
          </span></p>
          {comp.followUpScores.length > 0 && (
            <p><strong>Баллы за уточнения:</strong> {comp.followUpScores.join(', ')}</p>
          )}
          {comp.feedbacks.length > 0 && (
            <div>
              <strong>Рекомендации:</strong>
              <ul>
                {comp.feedbacks.map((feedback, idx) => (
                  <li key={idx}>{feedback}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      ))}
    </div>
  )
}
