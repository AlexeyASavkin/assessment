import { useState, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { getReport } from '../api/employee'

interface CompetencyReport {
  criteriaId: string
  criteriaName: string
  competencyName: string
  averageScore: number
  achievedLevel: string
  followUpScores: number[]
  feedbacks: string[]
}

interface Report {
  sessionId: string
  employeeName: string
  competencies: CompetencyReport[]
  compositeLevel: string
  overallRecommendation: string
}

export default function EmployeeReport() {
  const { sessionId } = useParams<{ sessionId: string }>()
  const [report, setReport] = useState<Report | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    loadReport()
  }, [sessionId])

  const loadReport = async () => {
    if (!sessionId) return
    setIsLoading(true)
    setError(null)
    try {
      const data = await getReport(sessionId)
      setReport(data)
    } catch (err) {
      setError('Ошибка загрузки отчёта')
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

  const getLevelColor = (level: string) => {
    switch (level) {
      case 'SENIOR': return '#28a745'
      case 'MIDDLE': return '#ffc107'
      default: return '#dc3545'
    }
  }

  return (
    <div className="container">
      <h1>Результаты ассессмента</h1>
      <p><strong>Сотрудник:</strong> {report.employeeName}</p>

      <div className="card">
        <h2>Итоговый уровень: <span style={{ color: getLevelColor(report.compositeLevel) }}>{report.compositeLevel}</span></h2>
        <p>{report.overallRecommendation}</p>
      </div>

      <h2>Оценки по критериям</h2>
      {report.competencies.map((comp) => (
        <div key={comp.criteriaId} className="card">
          <h3>{comp.competencyName} — {comp.criteriaName}</h3>
          <p><strong>Средний балл:</strong> {comp.averageScore}</p>
          <p><strong>Уровень:</strong> <span style={{ color: getLevelColor(comp.achievedLevel) }}>{comp.achievedLevel}</span></p>
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
