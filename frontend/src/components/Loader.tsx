/**
 * Пропсы компонента Loader.
 */
interface LoaderProps {
  /** Основной текст индикатора. */
  text?: string
  /** Дополнительный поясняющий текст меньшего размера. */
  subtext?: string
  /** Если true — рендерится как полноэкранный оверлей поверх контента. */
  overlay?: boolean
}

/**
 * Индикатор загрузки со спиннером.
 * Используется для отображения длительных операций (скоринг ответа ИИ,
 * генерация следующего вопроса, загрузка отчёта).
 * @param props - пропсы компонента
 * @return JSX-элемент индикатора загрузки
 */
export default function Loader({ text = 'Загрузка...', subtext, overlay = false }: LoaderProps) {
  if (overlay) {
    return (
      <div className="loading-overlay" role="status" aria-live="polite">
        <div className="loading-spinner" />
        <div className="loading-text">{text}</div>
        {subtext && <div className="loading-subtext">{subtext}</div>}
      </div>
    )
  }
  return (
    <div role="status" aria-live="polite" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.75rem', padding: '2rem' }}>
      <div className="loading-spinner" />
      <div className="loading-text">{text}</div>
      {subtext && <div className="loading-subtext">{subtext}</div>}
    </div>
  )
}