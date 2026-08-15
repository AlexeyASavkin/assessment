import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import type { AttemptDetail } from '../api/admin'
import Loader from '../components/Loader'
import { plainTextToHtml } from '../components/RichTextEditor'

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
  /** Полный список попыток (основные + уточняющие) — появился в C8. */
  attempts?: AttemptDetail[]
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
      const response = await fetch(`/api/employee/sessions/${sessionId}/report`, {
        credentials: 'include',
      })
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
          <button className="btn btn-primary" onClick={loadReport}>
            Попробовать снова
          </button>
        </div>
      </div>
    )
  }

  if (isLoading || !report) {
    return (
      <div className="container">
        <Loader text="Формируем отчёт..." subtext="Подсчитываем баллы и готовим рекомендации." />
      </div>
    )
  }

  return (
    <div className="container">
      <h1>Результаты ассессмента</h1>
      <p>
        <strong>Сотрудник:</strong> {report.employeeName}
      </p>

      <div className="card">
        <h2>
          Результат:{' '}
          <span style={{ color: report.passed ? '#28a745' : '#dc3545' }}>
            {report.passed ? 'Пройден' : 'Не пройден'}
          </span>
        </h2>
        <p>{report.overallRecommendation}</p>
      </div>

      <h2>Оценки по темам</h2>
      {report.competencies.map((comp) => (
        <div key={comp.topicId} className="card">
          <h3>
            {comp.competencyName} — {comp.sectionName} — {comp.topicName}
          </h3>
          <p>
            <strong>Средний балл:</strong> {comp.averageScore}
          </p>
          <p>
            <strong>Результат:</strong>{' '}
            <span style={{ color: comp.passed ? '#28a745' : '#dc3545' }}>
              {comp.passed ? 'Пройден' : 'Не пройден'}
            </span>
          </p>
          {comp.followUpScores.length > 0 && (
            <p>
              <strong>Баллы за уточнения:</strong> {comp.followUpScores.join(', ')}
            </p>
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

      {/* Ход оценки с уточняющими вопросами */}
      {report.attempts &&
        report.attempts.length > 0 &&
        (() => {
          const childrenByParent = new Map<string, AttemptDetail[]>()
          for (const a of report.attempts) {
            if (a.followupParentId) {
              const arr = childrenByParent.get(a.followupParentId) ?? []
              arr.push(a)
              childrenByParent.set(a.followupParentId, arr)
            }
          }
          const mainAttempts = report.attempts.filter((a) => !a.followupParentId)
          if (mainAttempts.length === 0) return null
          return (
            <>
              <h2 style={{ marginTop: '1.5rem' }}>Ход оценки</h2>
              <p style={{ color: '#555', fontSize: '0.9rem' }}>
                Ниже — основные вопросы, ответы и (если был задан) уточняющий вопрос с переоценкой.
              </p>
              {mainAttempts.map((main) => {
                const children = childrenByParent.get(main.attemptId) ?? []
                const hasRescore =
                  main.baseScore != null && main.score != null && main.baseScore !== main.score
                return (
                  <div key={main.attemptId} className="card" style={{ marginBottom: '1rem' }}>
                    <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      {main.topicName ?? 'Без темы'}
                      {hasRescore && (
                        <span
                          className="badge"
                          style={{
                            backgroundColor: '#ffc107',
                            color: '#000',
                            fontSize: '0.7rem',
                            padding: '0.1rem 0.4rem',
                            borderRadius: '4px',
                          }}
                        >
                          оценка пересчитана
                        </span>
                      )}
                    </h3>

                    <div style={{ marginTop: '0.25rem' }}>
                      <strong>Вопрос:</strong>
                      <div
                        className="rich-content"
                        dangerouslySetInnerHTML={{ __html: plainTextToHtml(main.questionText) }}
                      />
                    </div>
                    <div style={{ marginTop: '0.25rem' }}>
                      <strong>Ваш ответ:</strong>
                      <div
                        className="rich-content"
                        style={{
                          backgroundColor: '#f8f9fa',
                          padding: '0.5rem',
                          borderRadius: '4px',
                        }}
                        dangerouslySetInnerHTML={{
                          __html: plainTextToHtml(main.finalTranscript ?? '(пусто)'),
                        }}
                      />
                    </div>
                    <div style={{ marginTop: '0.25rem' }}>
                      <strong>Оценка:</strong> {main.score != null ? main.score.toFixed(2) : '—'}
                      {hasRescore && (
                        <span style={{ marginLeft: '0.5rem', fontSize: '0.85rem' }}>
                          (<span style={{ color: '#dc3545' }}>{main.baseScore!.toFixed(2)}</span>
                          {' → '}
                          <span style={{ color: '#28a745', fontWeight: 600 }}>
                            {main.score!.toFixed(2)}
                          </span>
                          )
                        </span>
                      )}
                    </div>

                    {children.length > 0 && (
                      <div
                        style={{
                          marginTop: '1rem',
                          paddingLeft: '1rem',
                          borderLeft: '3px solid #ffc107',
                        }}
                      >
                        <p
                          style={{
                            marginBottom: '0.5rem',
                            color: '#7a5c00',
                            fontWeight: 600,
                            fontSize: '0.9rem',
                          }}
                        >
                          Уточняющие вопросы ({children.length})
                        </p>
                        {children.map((fu) => (
                          <div
                            key={fu.attemptId}
                            style={{
                              marginBottom: '0.75rem',
                              padding: '0.75rem',
                              backgroundColor: '#fffbf0',
                              borderRadius: '4px',
                            }}
                          >
                            <h4 style={{ marginTop: 0 }}>Уточняющий вопрос</h4>
                            <div style={{ marginTop: '0.25rem' }}>
                              <strong>Вопрос:</strong>
                              <div
                                className="rich-content"
                                dangerouslySetInnerHTML={{
                                  __html: plainTextToHtml(fu.questionText),
                                }}
                              />
                            </div>
                            <div style={{ marginTop: '0.25rem' }}>
                              <strong>Ваш ответ:</strong>
                              <div
                                className="rich-content"
                                style={{
                                  backgroundColor: '#f8f9fa',
                                  padding: '0.5rem',
                                  borderRadius: '4px',
                                }}
                                dangerouslySetInnerHTML={{
                                  __html: plainTextToHtml(fu.finalTranscript ?? '(пусто)'),
                                }}
                              />
                            </div>
                            <div style={{ marginTop: '0.25rem' }}>
                              <strong>Оценка уточнения:</strong>{' '}
                              {fu.score != null ? fu.score.toFixed(2) : '—'}
                            </div>
                            {fu.feedback && (
                              <div style={{ marginTop: '0.25rem' }}>
                                <strong>Рекомендация:</strong>
                                <div
                                  className="rich-content"
                                  dangerouslySetInnerHTML={{ __html: plainTextToHtml(fu.feedback) }}
                                />
                              </div>
                            )}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )
              })}
            </>
          )
        })()}
    </div>
  )
}
