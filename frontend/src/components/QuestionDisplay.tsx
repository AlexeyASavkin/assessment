import { useRef, useEffect } from 'react'
import { useSpeechRecognition } from '../hooks/useSpeechRecognition'
import RichTextEditor, { type RichTextEditorHandle, plainTextToHtml } from './RichTextEditor'

/**
 * Пропсы компонента отображения вопроса.
 */
interface QuestionDisplayProps {
  questionText: string
  isFollowUp: boolean
  onSubmit: (finalTranscript: string) => void
  isLoading: boolean
}

/**
 * Компонент отображения вопроса для сотрудника.
 * Показывает текст вопроса, индикатор уточняющего вопроса,
 * кнопки голосового ввода, WYSIWYG-редактор для редактирования транскрипта
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
  } = useSpeechRecognition()

  const editorRef = useRef<RichTextEditorHandle>(null)
  const prevFinalLenRef = useRef(0)

  // При поступлении нового текста от распознавания речи — дозаписываем в редактор
  useEffect(() => {
    if (finalTranscript.length > prevFinalLenRef.current) {
      const delta = finalTranscript.slice(prevFinalLenRef.current)
      editorRef.current?.appendText(delta)
      prevFinalLenRef.current = finalTranscript.length
    }
  }, [finalTranscript])

  // При сбросе распознавания — очищаем и длину, и редактор
  useEffect(() => {
    if (finalTranscript === '') {
      prevFinalLenRef.current = 0
      editorRef.current?.clear()
    }
  }, [finalTranscript])

  const handleSubmit = () => {
    const content = editorRef.current?.getContent()
    if (content && content.text.trim()) {
      onSubmit(content.html)
      resetTranscript()
      prevFinalLenRef.current = 0
    }
  }

  return (
    <div className="card">
      {isFollowUp && <p style={{ color: '#666', fontStyle: 'italic' }}>Уточняющий вопрос:</p>}
      <div className="question-text" dangerouslySetInnerHTML={{ __html: plainTextToHtml(questionText) }} />

      <div style={{ marginBottom: '20px' }}>
        <button
          className={`btn ${isRecording ? 'btn-danger' : 'btn-primary'}`}
          onClick={isRecording ? stopRecording : startRecording}
          disabled={isLoading}
        >
          {isRecording ? 'Остановить запись' : 'Начать запись'}
        </button>
        {isRecording && <span className="recording-indicator"> ● Запись...</span>}
        {isRecording && interimTranscript && (
          <div style={{ marginTop: '8px', color: '#999', fontStyle: 'italic', fontSize: '13px' }}>
            {interimTranscript}
          </div>
        )}
      </div>

      <div style={{ marginBottom: '20px' }}>
        <RichTextEditor
          ref={editorRef}
          placeholder=""
          disabled={isLoading}
          minHeight="150px"
        />
      </div>

      <button
        className="btn btn-success"
        onClick={handleSubmit}
        disabled={isLoading}
      >
        {isLoading ? 'Отправка...' : 'Отправить ответ'}
      </button>
    </div>
  )
}
