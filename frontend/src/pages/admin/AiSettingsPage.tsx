import { useState, useEffect, FormEvent } from 'react'
import { getAiSettings, updateAiSettings, getAiKeys, updateAiKeys, type AiSettings, type AiKeys } from '../../api/admin'
import { AdminPageWrapper } from '../../components/admin/AdminLayout'

export default function AiSettingsPage() {
  const [settings, setSettings] = useState<AiSettings | null>(null)
  const [selectedProvider, setSelectedProvider] = useState('')
  const [keys, setKeys] = useState<AiKeys>({ geminiApiKey: '', gigachatApiKey: '' })
  const [showGeminiKey, setShowGeminiKey] = useState(false)
  const [showGigachatKey, setShowGigachatKey] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const load = async () => {
    setIsLoading(true)
    setError(null)
    try {
      const [settingsData, keysData] = await Promise.all([getAiSettings(), getAiKeys()])
      setSettings(settingsData)
      setSelectedProvider(settingsData.activeProvider)
      setKeys(keysData)
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

  const handleKeysSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setIsSaving(true)
    setError(null)
    setSuccess(null)
    try {
      const updated = await updateAiKeys(keys)
      setKeys(updated)
      setSuccess('API ключи сохранены')
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
        <>
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

        <div className="card" style={{ marginTop: '1rem' }}>
          <h2 style={{ fontSize: '1.1rem', marginBottom: '0.5rem' }}>API ключи</h2>
          <p style={{ color: '#555', marginBottom: '1rem' }}>Ключи хранятся в базе данных. После сохранения изменений ключи применяются автоматически.</p>
          <form onSubmit={handleKeysSubmit} className="admin-form">
            <div className="form-field">
              <label>Google Gemini API Key</label>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <input
                  type={showGeminiKey ? 'text' : 'password'}
                  value={keys.geminiApiKey}
                  onChange={(e) => setKeys({ ...keys, geminiApiKey: e.target.value })}
                  placeholder="AIza..."
                  style={{ flex: 1 }}
                />
                <button
                  type="button"
                  className="btn"
                  onClick={() => setShowGeminiKey(!showGeminiKey)}
                  style={{ padding: '0.25rem 0.75rem' }}
                >
                  {showGeminiKey ? 'Скрыть' : 'Показать'}
                </button>
              </div>
            </div>
            <div className="form-field">
              <label>Сбер GigaChat API Key</label>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <input
                  type={showGigachatKey ? 'text' : 'password'}
                  value={keys.gigachatApiKey}
                  onChange={(e) => setKeys({ ...keys, gigachatApiKey: e.target.value })}
                  placeholder="eyJ..."
                  style={{ flex: 1 }}
                />
                <button
                  type="button"
                  className="btn"
                  onClick={() => setShowGigachatKey(!showGigachatKey)}
                  style={{ padding: '0.25rem 0.75rem' }}
                >
                  {showGigachatKey ? 'Скрыть' : 'Показать'}
                </button>
              </div>
            </div>
            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={isSaving}>
                {isSaving ? 'Сохранение...' : 'Сохранить ключи'}
              </button>
            </div>
          </form>
        </div>
        </>
      ) : null}
    </AdminPageWrapper>
  )
}