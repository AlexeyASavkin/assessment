import { useState, useEffect, FormEvent } from 'react'
import { getAiSettings, updateAiSettings, type AiSettings } from '../../api/admin'
import { AdminPageWrapper } from '../../components/admin/AdminLayout'

export default function AiSettingsPage() {
  const [settings, setSettings] = useState<AiSettings | null>(null)
  const [selectedProvider, setSelectedProvider] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const load = async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await getAiSettings()
      setSettings(data)
      setSelectedProvider(data.activeProvider)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки настроек')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setIsSaving(true)
    setError(null)
    setSuccess(null)
    try {
      const updated = await updateAiSettings(selectedProvider)
      setSettings(updated)
      setSelectedProvider(updated.activeProvider)
      setSuccess('Настройки сохранены')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка сохранения')
    } finally {
      setIsSaving(false)
    }
  }

  const providerLabel = (key: string) => {
    switch (key) {
      case 'gemini': return 'Google Gemini'
      case 'gigachat': return 'Сбер GigaChat'
      default: return key
    }
  }

  return (
    <AdminPageWrapper>
      <h1>Настройки ИИ</h1>
      <p>Выберите провайдера искусственного интеллекта для генерации вопросов и оценки ответов.</p>

      {error && <p className="error-text">{error}</p>}
      {success && <p className="success-text">{success}</p>}

      {isLoading ? (
        <p>Загрузка...</p>
      ) : settings ? (
        <div className="card">
          <form onSubmit={handleSubmit} className="admin-form">
            <div className="form-field">
              <label>Активный провайдер</label>
              <div className="ai-provider-options">
                {settings.availableProviders.map((provider) => (
                  <label key={provider} className="ai-provider-option">
                    <input
                      type="radio"
                      name="aiProvider"
                      value={provider}
                      checked={selectedProvider === provider}
                      onChange={() => setSelectedProvider(provider)}
                    />
                    <div className="ai-provider-card">
                      <strong>{providerLabel(provider)}</strong>
                      {provider === 'gemini' && <p>Облачная модель Google. Требуется API ключ GEMINI_API_KEY.</p>}
                      {provider === 'gigachat' && <p>Российская модель Сбера. Требуется API ключ GIGACHAT_API_KEY.</p>}
                    </div>
                  </label>
                ))}
              </div>
            </div>
            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={isSaving}>
                {isSaving ? 'Сохранение...' : 'Сохранить'}
              </button>
            </div>
          </form>
        </div>
      ) : null}
    </AdminPageWrapper>
  )
}