import { useSpeechRecognition } from '../hooks/useSpeechRecognition'

/**
 * Пропсы компонента отображения вопроса.
 */
interface QuestionDisplayProps {
  questionText: string
  isFollowUp: boolean
  onSubmit: (rawTranscript: string, finalTranscript: string) => void
  isLoading: boolean
}

/**
 * Компонент отображения вопроса для сотрудника.
 * Показывает текст вопроса, индикатор уточняющего вопроса,
 * кнопки голосового ввода, текстовое поле для редактирования транскрипта
 * и кнопку отправки ответа.
 * @param props - пропсы компонента
 * @return JSX-элемент с интерфейсом ответа на вопрос
 */
export default function QuestionDisplay({ questionText, isFollowUp, onSubmit, isLoading }: QuestionDisplayProps) {
  const {
    isRecording,
    interimTranscript,
    finalTranscript,
    startRecording,
    stopRecording,
    resetTranscript,
    setFinalTranscript,
  } = useSpeechRecognition()

  const handleSubmit = () => {
    if (finalTranscript.trim()) {
      onSubmit(finalTranscript, finalTranscript)
      resetTranscript()
    }
  }

  return (
    <div className="card">
      {isFollowUp && <p style={{ color: '#666', fontStyle: 'italic' }}>Уточняющий вопрос:</p>}
      <div className="question-text">{questionText}</div>

      <div style={{ marginBottom: '20px' }}>
        <button
          className={`btn ${isRecording ? 'btn-danger' : 'btn-primary'}`}
          onClick={isRecording ? stopRecording : startRecording}
          disabled={isLoading}
        >
          {isRecording ? 'Остановить запись' : 'Начать запись'}
        </button>
        {isRecording && <span className="recording-indicator"> ● Запись...</span>}
      </div>

      <div style={{ marginBottom: '20px' }}>
        <label>Расшифровка (можно отредактировать):</label>
        <textarea
          value={finalTranscript + interimTranscript}
          onChange={(e) => setFinalTranscript(e.target.value)}
          placeholder="Нажмите 'Начать запись' и говорите..."
          disabled={isLoading}
        />
      </div>

      <button
        className="btn btn-success"
        onClick={handleSubmit}
        disabled={!finalTranscript.trim() || isLoading}
      >
        {isLoading ? 'Отправка...' : 'Отправить ответ'}
      </button>
    </div>
  )
}
