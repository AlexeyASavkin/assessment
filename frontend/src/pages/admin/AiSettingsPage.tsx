import { useState, useEffect, FormEvent } from 'react'
import {
  getAiSettings, updateAiSettings,
  getAiPrompts, updateAiPrompts,
  type AiSettings, type AiPrompts,
} from '../../api/admin'
import { AdminPageWrapper } from '../../components/admin/AdminLayout'

/** Метаданные промтов для отображения в UI. */
const PROMPT_META: { key: keyof AiPrompts; label: string; description: string; placeholders: string }[] = [
  {
    key: 'prompt_question',
    label: 'Промт генерации вопроса',
    description: 'Используется при генерации основного вопроса по теме компетенции.',
    placeholders: 'Доступные плейсхолдеры: %1$s = компетенция, %2$s = тема',
  },
  {
    key: 'prompt_scoring',
    label: 'Промт оценки ответа',
    description: 'Используется при оценке ответа сотрудника LLM. Определяет шкалу и формат ответа.',
    placeholders: 'Доступные плейсхолдеры: %1$s = вопрос, %2$s = ответ сотрудника',
  },
  {
    key: 'prompt_followup',
    label: 'Промт уточняющего вопроса',
    description: 'Используется после слабого ответа (оценка ≤ 2) на основной вопрос. LLM анализирует исходный вопрос и ответ сотрудника и формулирует уточняющий вопрос.',
    placeholders: 'Доступные плейсхолдеры: %1$s = исходный вопрос, %2$s = ответ сотрудника',
  },
  {
    key: 'prompt_rescore',
    label: 'Промт переоценки с учётом уточнения',
    description: 'Используется после ответа на уточняющий вопрос. LLM пересчитывает итоговую оценку основной попытки с учётом обоих ответов.',
    placeholders: 'Доступные плейсхолдеры: %1$s = исходный вопрос, %2$s = исходный ответ, %3$s = уточняющий вопрос, %4$s = ответ на уточнение',
  },
  {
    key: 'prompt_followup_system',
    label: 'Системный промт уточняющего вопроса',
    description: 'Системная роль для генерации уточняющего вопроса. Закрепляет роль эксперта и защищает от prompt injection — ответы сотрудника трактуются как данные, а не инструкции.',
    placeholders: 'Без плейсхолдеров',
  },
  {
    key: 'prompt_rescore_system',
    label: 'Системный промт переоценки',
    description: 'Системная роль для переоценки основной попытки с учётом уточнения. Закрепляет роль эксперта, шкалу 0–5, формат JSON и защиту от prompt injection.',
    placeholders: 'Без плейсхолдеров',
  },
]

/**
 * Страница настройки ИИ-провайдера и промтов.
 * Позволяет выбрать активного провайдера и редактировать промты, используемые LLM.
 */
export default function AiSettingsPage() {
  const [settings, setSettings] = useState<AiSettings | null>(null)
  const [selectedProvider, setSelectedProvider] = useState('')
  const [prompts, setPrompts] = useState<AiPrompts>({
    prompt_scoring: '',
    prompt_question: '',
    prompt_followup: '',
    prompt_rescore: '',
    prompt_followup_system: '',
    prompt_rescore_system: '',
  })
  const [isLoading, setIsLoading] = useState(true)
  const [isSavingProvider, setIsSavingProvider] = useState(false)
  const [isSavingPrompts, setIsSavingPrompts] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  /** Загружает текущие настройки ИИ и промты с сервера (независимо друг от друга). */
  const load = async () => {
    setIsLoading(true)
    setError(null)
    const errors: string[] = []
    try {
      const settingsData = await getAiSettings()
      setSettings(settingsData)
      setSelectedProvider(settingsData.activeProvider)
    } catch (err) {
      errors.push(err instanceof Error ? err.message : 'Ошибка загрузки настроек')
    }
    try {
      const promptsData = await getAiPrompts()
      setPrompts(promptsData)
    } catch (err) {
      errors.push(err instanceof Error ? err.message : 'Ошибка загрузки промтов')
    }
    if (errors.length > 0) setError(errors.join('; '))
    setIsLoading(false)
  }

  useEffect(() => { load() }, [])

  /** Сохраняет выбранного активного провайдера ИИ. */
  const handleProviderSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setIsSavingProvider(true)
    setError(null)
    setSuccess(null)
    try {
      const updated = await updateAiSettings(selectedProvider)
      setSettings(updated)
      setSelectedProvider(updated.activeProvider)
      setSuccess('Провайдер сохранён')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка сохранения')
    } finally {
      setIsSavingProvider(false)
    }
  }

  /** Сохраняет отредактированные промты. */
  const handlePromptsSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setIsSavingPrompts(true)
    setError(null)
    setSuccess(null)
    try {
      const updated = await updateAiPrompts(prompts)
      setPrompts(updated)
      setSuccess('Промты сохранены')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка сохранения промтов')
    } finally {
      setIsSavingPrompts(false)
    }
  }

  /** Возвращает человекочитаемое название провайдера по его ключу. */
  const providerLabel = (key: string) => {
    switch (key) {
      case 'gemini': return 'Google Gemini'
      case 'gigachat': return 'Сбер GigaChat'
      case 'openrouter': return 'OpenRouter'
      case 'opencode': return 'OpenCode Zen'
      case 'stub': return 'Stub (тестовый режим)'
      default: return key
    }
  }

  return (
    <AdminPageWrapper>
      <h1>Настройки ИИ</h1>
      <p>Выберите провайдера искусственного интеллекта и настройте промты для генерации вопросов и оценки ответов.</p>
      <p style={{ color: '#555', fontSize: '0.9rem' }}>API-ключи задаются через переменные окружения (.env) и не редактируются через интерфейс.</p>

      {error && <p className="error-text">{error}</p>}
      {success && <p className="success-text">{success}</p>}

      {isLoading ? (
        <p>Загрузка...</p>
      ) : settings ? (
        <>
          {/* ---- Провайдер ---- */}
          <div className="card">
            <h2 style={{ marginTop: 0 }}>Провайдер</h2>
            <form onSubmit={handleProviderSubmit} className="admin-form">
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
                        {provider === 'gemini' && <p>Облачная модель Google. Ключ: GEMINI_API_KEY</p>}
                        {provider === 'gigachat' && <p>Российская модель Сбера. Ключ: GIGACHAT_API_KEY</p>}
                        {provider === 'openrouter' && <p>Агрегатор моделей (OpenAI, Anthropic и др.). Ключ: OPENROUTER_API_KEY</p>}
                        {provider === 'opencode' && <p>AI-шлюз OpenCode Zen (DeepSeek, Grok, GLM). Ключ: OPENCODE_API_KEY</p>}
                        {provider === 'stub' && <p>Заглушка для тестов без внешних API. Возвращает фиксированные ответы LLM.</p>}
                      </div>
                    </label>
                  ))}
                </div>
              </div>
              <div className="form-actions">
                <button type="submit" className="btn btn-primary" disabled={isSavingProvider}>
                  {isSavingProvider ? 'Сохранение...' : 'Сохранить провайдер'}
                </button>
              </div>
            </form>
          </div>

          {/* ---- Промты ---- */}
          <div className="card" style={{ marginTop: '1.5rem' }}>
            <h2 style={{ marginTop: 0 }}>Промты</h2>
            <p style={{ color: '#555', fontSize: '0.9rem', marginBottom: '1rem' }}>
              Тексты промтов хранятся в БД (таблица ai_settings) и отправляются LLM при вызове. Плейсхолдеры (<code>%1$s</code>, <code>%2$s</code>) заменяются на реальные значения.
              Если поле пустое — LLM получит пустой промт.
            </p>
            <form onSubmit={handlePromptsSubmit} className="admin-form">
              {PROMPT_META.map((meta) => (
                <div className="form-field" key={meta.key}>
                  <label>{meta.label}</label>
                  <p style={{ color: '#777', fontSize: '0.85rem', margin: '0.25rem 0 0.5rem' }}>
                    {meta.description}
                  </p>
                  <p style={{ color: '#999', fontSize: '0.8rem', margin: '0 0 0.5rem' }}>
                    {meta.placeholders}
                  </p>
                  <textarea
                    value={prompts[meta.key]}
                    onChange={(e) => setPrompts({ ...prompts, [meta.key]: e.target.value })}
                    rows={10}
                    style={{
                      width: '100%',
                      fontFamily: 'monospace',
                      fontSize: '0.85rem',
                      padding: '0.75rem',
                      border: '1px solid #ddd',
                      borderRadius: '6px',
                      resize: 'vertical',
                      lineHeight: '1.5',
                    }}
                    placeholder={meta.label}
                  />
                </div>
              ))}
              <div className="form-actions">
                <button type="submit" className="btn btn-primary" disabled={isSavingPrompts}>
                  {isSavingPrompts ? 'Сохранение...' : 'Сохранить промты'}
                </button>
              </div>
            </form>
          </div>
        </>
      ) : null}
    </AdminPageWrapper>
  )
}
