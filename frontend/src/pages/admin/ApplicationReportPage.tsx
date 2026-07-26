import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getApplicationReport, type ApplicationReport, type AttemptDetail } from '../../api/admin'
import { AdminPageWrapper } from '../../components/admin/AdminLayout'
import { plainTextToHtml } from '../../components/RichTextEditor'

/**
 * Страница детального отчёта по заявке.
 * Показывает оценки по темам, все ответы сотрудника, feedback ИИ и общую рекомендацию.
 */
export default function ApplicationReportPage() {
  const { sessionId } = useParams<{ sessionId: string }>()
  const navigate = useNavigate()
  const [report, setReport] = useState<ApplicationReport | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    loadReport()
  }, [sessionId])

  /**
   * Загружает отчёт с сервера.
   */
  const loadReport = async () => {
    if (!sessionId) return
    setIsLoading(true)
    setError(null)
    try {
      const data = await getApplicationReport(sessionId)
      setReport(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки отчёта')
    } finally {
      setIsLoading(false)
    }
  }

  /**
   * Форматирует ISO-дату в локальный формат.
   * @param iso - ISO-строка даты
   */
  const formatDate = (iso: string | null): string => {
    if (!iso) return '—'
    return new Date(iso).toLocaleString('ru-RU', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      timeZone: 'Europe/Moscow',
    })
  }

  /**
   * Возвращает отображаемое имя статуса сессии.
   */
  const renderStatus = (status: string): string => {
    switch (status) {
      case 'COMPLETED': return 'Завершена'
      case 'ACTIVE': return 'Активна'
      default: return status
    }
  }

  if (error) {
    return (
      <AdminPageWrapper>
        <h1>Ошибка</h1>
        <p className="error-text">{error}</p>
        <button className="btn btn-primary" onClick={loadReport}>Попробовать снова</button>
        <button className="btn btn-secondary" onClick={() => navigate('/admin/applications')} style={{ marginLeft: '0.5rem' }}>
          Назад к заявкам
        </button>
      </AdminPageWrapper>
    )
  }

  if (isLoading || !report) {
    return (
      <AdminPageWrapper>
        <p>Загрузка отчёта...</p>
        <button className="btn btn-secondary" onClick={() => navigate('/admin/applications')}>Назад</button>
      </AdminPageWrapper>
    )
  }

  return (
    <AdminPageWrapper>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
        <h1>Отчёт по заявке</h1>
        <button className="btn btn-secondary" onClick={() => navigate('/admin/applications')}>Назад к заявкам</button>
      </div>

      {/* Общая информация */}
      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <h2>{report.employeeName}</h2>
        <p><strong>Компетенция:</strong> {report.competencyName ?? '—'}</p>
        <p><strong>Статус:</strong> {renderStatus(report.sessionStatus)}</p>
        <p><strong>Создана:</strong> {formatDate(report.createdAt)}</p>
        <p><strong>Завершена:</strong> {formatDate(report.updatedAt)}</p>
      </div>

      {/* Итоговый результат и рекомендация */}
      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <h2>Результат: <span style={{ color: report.passed ? '#28a745' : '#dc3545', fontWeight: 700 }}>
          {report.passed ? 'Пройден' : 'Не пройден'}
        </span></h2>
        <p>{report.overallRecommendation}</p>
      </div>

      {/* Оценки по темам */}
      <h2>Оценки по темам</h2>
      {report.competencies.length === 0 ? (
        <p>Нет данных по темам.</p>
      ) : (
        report.competencies.map((comp) => (
          <div key={comp.topicId} className="card" style={{ marginBottom: '1rem' }}>
            <h3>{comp.competencyName} — {comp.sectionName} — {comp.topicName}</h3>
            <p><strong>Средний балл:</strong> {comp.averageScore.toFixed(2)}</p>
            <p><strong>Результат:</strong> <span style={{ color: comp.passed ? '#28a745' : '#dc3545', fontWeight: 600 }}>
              {comp.passed ? 'Пройден' : 'Не пройден'}
            </span></p>
            {comp.followUpScores.length > 0 && (
              <p><strong>Баллы за уточнения:</strong> {comp.followUpScores.map(s => s.toFixed(2)).join(', ')}</p>
            )}
            {comp.feedbacks.length > 0 && (
              <div>
                <strong>Feedback:</strong>
                <ul>
                  {comp.feedbacks.map((fb, idx) => (
                    <li key={idx}>{fb}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        ))
      )}

      {/* Все ответы сотрудника — основные карты с вложенными уточнениями */}
      <h2 style={{ marginTop: '2rem' }}>Ответы сотрудника</h2>
      {report.attempts.length === 0 ? (
        <p>Ответов нет.</p>
      ) : (
        (() => {
          // Группируем: основные попытки как parents, уточнения как children
          const childrenByParent = new Map<string, AttemptDetail[]>()
          for (const a of report.attempts) {
            if (a.followupParentId) {
              const arr = childrenByParent.get(a.followupParentId) ?? []
              arr.push(a)
              childrenByParent.set(a.followupParentId, arr)
            }
          }
          const mainAttempts = report.attempts.filter(a => !a.followupParentId)

          return mainAttempts.map((main: AttemptDetail) => {
            const children = childrenByParent.get(main.attemptId) ?? []
            const hasRescore = main.baseScore != null && main.score != null && main.baseScore !== main.score
            return (
              <div key={main.attemptId} className="card" style={{ marginBottom: '1rem' }}>
                <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  {main.topicName ?? 'Без темы'}
                  <span className="badge" style={{ backgroundColor: '#6c757d', color: '#fff', fontSize: '0.75rem', padding: '0.1rem 0.5rem', borderRadius: '4px' }}>основной</span>
                  {hasRescore && (
                    <span className="badge" style={{ backgroundColor: '#ffc107', color: '#000', fontSize: '0.7rem', padding: '0.1rem 0.4rem', borderRadius: '4px' }}>
                      переоценка
                    </span>
                  )}
                </h3>
                {main.sectionName && (
                  <p style={{ color: '#6c757d', fontSize: '0.9rem' }}>
                    {main.competencyName} — {main.sectionName}
                  </p>
                )}

                <div style={{ marginTop: '0.5rem' }}>
                  <strong>Вопрос:</strong>
                  <div className="rich-content" dangerouslySetInnerHTML={{ __html: plainTextToHtml(main.questionText) }} />
                </div>

                <div style={{ marginTop: '0.5rem' }}>
                  <strong>Ответ сотрудника:</strong>
                  <div className="rich-content" style={{ backgroundColor: '#f8f9fa', padding: '0.75rem', borderRadius: '4px' }}
                       dangerouslySetInnerHTML={{ __html: plainTextToHtml(main.finalTranscript ?? '(пусто)') }} />
                </div>

                <div style={{ marginTop: '0.5rem', display: 'flex', gap: '1.5rem', flexWrap: 'wrap', alignItems: 'center' }}>
                  <span>
                    <strong>Оценка:</strong>{' '}
                    {main.score != null ? main.score.toFixed(2) : '—'}
                    {hasRescore && (
                      <span style={{ marginLeft: '0.5rem', fontSize: '0.85rem' }}>
                        (<span style={{ color: '#dc3545' }}>{main.baseScore!.toFixed(2)}</span>
                        {' → '}
                        <span style={{ color: '#28a745', fontWeight: 600 }}>{main.score!.toFixed(2)}</span>)
                      </span>
                    )}
                  </span>
                  <span><span className="info-hint" data-tooltip="Уверенность ИИ в собственной оценке: HIGH — уверен, ответ однозначен; MEDIUM — умеренно; LOW — не уверен, ответ неоднозначен"><strong>Уверенность</strong></span>: {main.confidence ?? '—'}</span>
                  <span><span className="info-hint" data-tooltip="Оценка засчитана: да — если балл > 0 (ответ по теме), нет — если 0 (ответ некорректный, не по теме)"><strong>Валидна</strong></span>: {main.validJudge === false ? 'нет' : 'да'}</span>
                </div>

                {main.feedback && (
                  <div style={{ marginTop: '0.5rem' }}>
                    <strong>Feedback ИИ:</strong>
                    <div className="rich-content" style={{ backgroundColor: '#eef', padding: '0.75rem', borderRadius: '4px' }}
                         dangerouslySetInnerHTML={{ __html: plainTextToHtml(main.feedback) }} />
                  </div>
                )}

                {/* Вложенные уточняющие вопросы */}
                {children.length > 0 && (
                  <div style={{ marginTop: '1rem', paddingLeft: '1rem', borderLeft: '3px solid #ffc107' }}>
                    <p style={{ marginBottom: '0.5rem', color: '#7a5c00', fontWeight: 600, fontSize: '0.9rem' }}>
                      Уточняющие вопросы ({children.length})
                    </p>
                    {children.map((fu: AttemptDetail) => (
                      <div key={fu.attemptId} className="card" style={{ marginBottom: '0.75rem', backgroundColor: '#fffbf0', padding: '0.75rem' }}>
                        <h4 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: 0 }}>
                          Уточняющий вопрос
                          <span className="badge" style={{ backgroundColor: '#ffc107', color: '#000', fontSize: '0.7rem', padding: '0.1rem 0.4rem', borderRadius: '4px' }}>уточнение</span>
                        </h4>
                        <div style={{ marginTop: '0.25rem' }}>
                          <strong>Вопрос:</strong>
                          <div className="rich-content" dangerouslySetInnerHTML={{ __html: plainTextToHtml(fu.questionText) }} />
                        </div>
                        <div style={{ marginTop: '0.25rem' }}>
                          <strong>Ответ сотрудника:</strong>
                          <div className="rich-content" style={{ backgroundColor: '#f8f9fa', padding: '0.5rem', borderRadius: '4px' }}
                               dangerouslySetInnerHTML={{ __html: plainTextToHtml(fu.finalTranscript ?? '(пусто)') }} />
                        </div>
                        <div style={{ marginTop: '0.25rem', display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
                          <span><strong>Оценка уточнения:</strong> {fu.score != null ? fu.score.toFixed(2) : '—'}</span>
                          <span><strong>Уверенность:</strong> {fu.confidence ?? '—'}</span>
                        </div>
                        {fu.feedback && (
                          <div style={{ marginTop: '0.25rem' }}>
                            <strong>Feedback:</strong>
                            <div className="rich-content" style={{ backgroundColor: '#eef', padding: '0.5rem', borderRadius: '4px' }}
                                 dangerouslySetInnerHTML={{ __html: plainTextToHtml(fu.feedback) }} />
                          </div>
                        )}
                        <p style={{ marginTop: '0.25rem', fontSize: '0.75rem', color: '#6c757d' }}>
                          {formatDate(fu.createdAt)}
                        </p>
                      </div>
                    ))}
                  </div>
                )}

                <p style={{ marginTop: '0.5rem', fontSize: '0.8rem', color: '#6c757d' }}>
                  {formatDate(main.createdAt)}
                </p>
              </div>
            )
          })
        })()
      )}

      <button className="btn btn-secondary" onClick={() => navigate('/admin/applications')} style={{ marginTop: '1rem' }}>
        Назад к заявкам
      </button>
    </AdminPageWrapper>
  )
}