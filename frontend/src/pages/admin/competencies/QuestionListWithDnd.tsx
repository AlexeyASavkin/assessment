import RichTextEditor, { plainTextToHtml } from '../../../components/RichTextEditor'
import type { TopicQuestionsApi } from './useTopicQuestions'

/**
 * Пропсы списка вопросов темы с перетаскиванием.
 */
interface QuestionListWithDndProps {
  topicId: string
  questions: TopicQuestionsApi
}

/**
 * Список вопросов выбранной темы: отображение, редактирование через RichTextEditor,
 * удаление и изменение порядка перетаскиванием.
 */
export default function QuestionListWithDnd({ topicId, questions }: QuestionListWithDndProps) {
  if (questions.topicQuestions.length === 0) {
    return <p className="tree-detail-empty">Вопросов пока нет.</p>
  }

  return (
    <div className="admin-list">
      {questions.topicQuestions.map((q, index) => (
        <div
          key={q.id}
          className={`card admin-list-item draggable-question ${questions.dragOverIndex === index ? 'drag-over' : ''}`}
          draggable={questions.editingQuestionId !== q.id}
          onDragStart={(e) => questions.dragStart(e, index)}
          onDragOver={(e) => questions.dragOver(e, index)}
          onDragLeave={questions.dragLeave}
          onDrop={(e) => questions.drop(e, index, topicId)}
        >
          <div className="drag-handle" title="Перетащить">
            ⠿
          </div>
          {questions.editingQuestionId === q.id ? (
            <>
              <RichTextEditor
                ref={questions.questionEditorRef}
                content={questions.editQuestionText}
                minHeight="80px"
              />
              <div className="question-edit-actions">
                <button
                  className="btn btn-primary btn-sm"
                  disabled={questions.savingQuestion || !questions.editQuestionText.trim()}
                  onClick={() => questions.saveQuestion(q.id, topicId)}
                >
                  {questions.savingQuestion ? '...' : 'Сохранить'}
                </button>
                <button
                  className="btn btn-sm"
                  onClick={questions.cancelEditQuestion}
                  disabled={questions.savingQuestion}
                >
                  Отмена
                </button>
              </div>
            </>
          ) : (
            <>
              <div className="admin-list-info">
                <div dangerouslySetInnerHTML={{ __html: plainTextToHtml(q.questionText) }} />
              </div>
              <button className="btn btn-sm" onClick={() => questions.startEditQuestion(q)}>
                Ред.
              </button>
              <button
                className="btn btn-danger btn-sm"
                onClick={() => questions.deleteQuestion(q.id, topicId)}
              >
                Удалить
              </button>
            </>
          )}
        </div>
      ))}
    </div>
  )
}
