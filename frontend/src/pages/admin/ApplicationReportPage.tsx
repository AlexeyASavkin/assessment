import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getApplicationReport, type ApplicationReport, type AttemptDetail } from '../../api/admin'
import { AdminPageWrapper } from '../../components/admin/AdminLayout'

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
   * Возвращает цветовой код для уровня компетенции.
   * @param level - уровень (SENIOR, MIDDLE, JUNIOR)
   */
  const getLevelColor = (level: string | null): string => {
    if (!level) return '#6c757d'
    switch (level) {
      case 'SENIOR': return '#28a745'
      case 'MIDDLE': return '#ffc107'
      default: return '#dc3545'
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

      {/* Итоговый уровень и рекомендация */}
      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <h2>Итоговый уровень: <span style={{ color: getLevelColor(report.compositeLevel), fontWeight: 700 }}>{report.compositeLevel}</span></h2>
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
            <p><strong>Уровень:</strong> <span style={{ color: getLevelColor(comp.achievedLevel), fontWeight: 600 }}>{comp.achievedLevel}</span></p>
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

      {/* Все ответы сотрудника */}
      <h2 style={{ marginTop: '2rem' }}>Ответы сотрудника</h2>
      {report.attempts.length === 0 ? (
        <p>Ответов нет.</p>
      ) : (
        report.attempts.map((attempt: AttemptDetail) => {
          const isFollowup = attempt.followupDepth > 0
          return (
            <div key={attempt.attemptId} className="card" style={{ marginBottom: '1rem' }}>
              <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                {attempt.topicName ?? 'Без темы'}
                {isFollowup && <span className="badge" style={{ backgroundColor: '#ffc107', color: '#000', fontSize: '0.75rem', padding: '0.1rem 0.5rem', borderRadius: '4px' }}>уточнение</span>}
                {!isFollowup && <span className="badge" style={{ backgroundColor: '#6c757d', color: '#fff', fontSize: '0.75rem', padding: '0.1rem 0.5rem', borderRadius: '4px' }}>основной</span>}
              </h3>
              {attempt.sectionName && (
                <p style={{ color: '#6c757d', fontSize: '0.9rem' }}>
                  {attempt.competencyName} — {attempt.sectionName}
                </p>
              )}

              <div style={{ marginTop: '0.5rem' }}>
                <strong>Вопрос:</strong>
                <p>{attempt.questionText}</p>
              </div>

              <div style={{ marginTop: '0.5rem' }}>
                <strong>Ответ сотрудника:</strong>
                <p style={{ whiteSpace: 'pre-wrap', backgroundColor: '#f8f9fa', padding: '0.75rem', borderRadius: '4px' }}>
                  {attempt.finalTranscript ?? '(пусто)'}
                </p>
              </div>

              {attempt.rawTranscript && attempt.rawTranscript !== attempt.finalTranscript && (
                <div style={{ marginTop: '0.5rem', fontSize: '0.85rem', color: '#6c757d' }}>
                  <strong>Сырой транскрипт:</strong>
                  <p style={{ whiteSpace: 'pre-wrap' }}>{attempt.rawTranscript}</p>
                </div>
              )}

              <div style={{ marginTop: '0.5rem', display: 'flex', gap: '1.5rem', flexWrap: 'wrap' }}>
                <span><strong>Оценка:</strong> {attempt.score != null ? attempt.score.toFixed(2) : '—'}</span>
                <span><strong>Уверенность:</strong> {attempt.confidence ?? '—'}</span>
                <span><strong>Валидна:</strong> {attempt.validJudge === false ? 'нет' : 'да'}</span>
              </div>

              {attempt.feedback && (
                <div style={{ marginTop: '0.5rem' }}>
                  <strong>Feedback ИИ:</strong>
                  <p style={{ whiteSpace: 'pre-wrap', backgroundColor: '#eef', padding: '0.75rem', borderRadius: '4px' }}>
                    {attempt.feedback}
                  </p>
                </div>
              )}

              <p style={{ marginTop: '0.5rem', fontSize: '0.8rem', color: '#6c757d' }}>
                {formatDate(attempt.createdAt)}
              </p>
            </div>
          )
        })
      )}

      <button className="btn btn-secondary" onClick={() => navigate('/admin/applications')} style={{ marginTop: '1rem' }}>
        Назад к заявкам
      </button>
    </AdminPageWrapper>
  )
}