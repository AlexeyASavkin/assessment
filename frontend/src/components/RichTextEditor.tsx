import { useRef, useEffect, useCallback, forwardRef, useImperativeHandle } from 'react'
import { useEditor, EditorContent } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'

/**
 * Преобразует plain text с переносами строк в HTML-параграфы TipTap.
 * Если строка уже содержит HTML-теги — возвращает как есть.
 */
export function plainTextToHtml(text: string): string {
  if (!text) return ''
  // Если уже есть HTML-теги — не трогаем
  if (/<[a-z][\s\S]*>/i.test(text)) return text
  return text
    .split('\n')
    .map((line) => `<p>${line || '<br>'}</p>`)
    .join('')
}

/**
 * Пропсы компонента WYSIWYG-редактора на базе TipTap.
 */
interface RichTextEditorProps {
  /** Начальное содержимое (HTML). */
  content?: string
  /** Вызывается при каждом изменении содержимого. Возвращает HTML. */
  onChange?: (html: string) => void
  /** Placeholder, отображаемый в пустом редакторе. */
  placeholder?: string
  /** Заблокировать редактор (отключить ввод). */
  disabled?: boolean
  /** Минимальная высота редактора (CSS, например '150px'). */
  minHeight?: string
}

/**
 * Публичный интерфейс редактора, доступный через ref.
 */
export interface RichTextEditorHandle {
  /** Вставить текст в текущую позицию курсора. */
  appendText: (text: string) => void
  /** Получить текущее содержимое. */
  getContent: () => { text: string; html: string }
  /** Установить содержимое (HTML). */
  setContent: (html: string) => void
  /** Очистить редактор. */
  clear: () => void
}

/**
 * Компонент WYSIWYG-редактора на базе TipTap.
 * Предоставляет панель инструментов (жирный, курсив, списки)
 * и редактируемую область с поддержкой HTML-вывода.
 */
const RichTextEditor = forwardRef<RichTextEditorHandle, RichTextEditorProps>(function RichTextEditor(
  {
    content = '',
    onChange,
    placeholder = 'Введите текст...',
    disabled = false,
    minHeight = '150px',
  },
  ref,
) {
  const prevContentRef = useRef(content)

  const editor = useEditor({
    extensions: [StarterKit],
    content: plainTextToHtml(content),
    editable: !disabled,
    editorProps: {
      attributes: {
        class: 'tiptap-editor-content',
        style: `min-height: ${minHeight};`,
      },
    },
    onUpdate: ({ editor: e }) => {
      onChange?.(e.getHTML())
    },
  })

  // Синхронизация внешнего content с редактором (только при реальном изменении)
  useEffect(() => {
    if (!editor) return
    if (content !== prevContentRef.current) {
      prevContentRef.current = content
      const { from, to } = editor.state.selection
      editor.commands.setContent(plainTextToHtml(content), { emitUpdate: false })
      const docSize = editor.state.doc.content.size
      try {
        editor.commands.setTextSelection({ from: Math.min(from, docSize), to: Math.min(to, docSize) })
      } catch {
        editor.commands.focus('end')
      }
    }
  }, [content, editor])

  // Синхронизация disabled
  useEffect(() => {
    if (editor) {
      editor.setEditable(!disabled)
    }
  }, [disabled, editor])

  const appendText = useCallback(
    (text: string) => {
      if (!editor) return
      editor.commands.insertContent(text)
    },
    [editor],
  )

  const getContent = useCallback(() => {
    if (!editor) return { text: '', html: '' }
    return { text: editor.getText(), html: editor.getHTML() }
  }, [editor])

  const setContent = useCallback(
    (html: string) => {
      if (!editor) return
      editor.commands.setContent(html)
      prevContentRef.current = html
    },
    [editor],
  )

  const clear = useCallback(() => {
    if (!editor) return
    editor.commands.clearContent()
    prevContentRef.current = ''
  }, [editor])

  useImperativeHandle(ref, () => ({ appendText, getContent, setContent, clear }), [appendText, getContent, setContent, clear])

  if (!editor) return null

  return (
    <div className="tiptap-editor">
      <div className="tiptap-toolbar">
        <button
          type="button"
          className={`tiptap-btn ${editor.isActive('bold') ? 'tiptap-btn-active' : ''}`}
          onClick={() => editor.chain().focus().toggleBold().run()}
          disabled={disabled}
          title="Жирный"
        >
          <b>B</b>
        </button>
        <button
          type="button"
          className={`tiptap-btn ${editor.isActive('italic') ? 'tiptap-btn-active' : ''}`}
          onClick={() => editor.chain().focus().toggleItalic().run()}
          disabled={disabled}
          title="Курсив"
        >
          <i>I</i>
        </button>
        <button
          type="button"
          className={`tiptap-btn ${editor.isActive('bulletList') ? 'tiptap-btn-active' : ''}`}
          onClick={() => editor.chain().focus().toggleBulletList().run()}
          disabled={disabled}
          title="Маркированный список"
        >
          &#8226; Список
        </button>
        <button
          type="button"
          className={`tiptap-btn ${editor.isActive('orderedList') ? 'tiptap-btn-active' : ''}`}
          onClick={() => editor.chain().focus().toggleOrderedList().run()}
          disabled={disabled}
          title="Нумерованный список"
        >
          1. Список
        </button>
        <span className="tiptap-separator" />
        <button
          type="button"
          className={`tiptap-btn ${editor.isActive('code') ? 'tiptap-btn-active' : ''}`}
          onClick={() => editor.chain().focus().toggleCode().run()}
          disabled={disabled}
          title="Инлайн-код"
        >
          {'</>'}
        </button>
        <button
          type="button"
          className={`tiptap-btn ${editor.isActive('codeBlock') ? 'tiptap-btn-active' : ''}`}
          onClick={() => editor.chain().focus().toggleCodeBlock().run()}
          disabled={disabled}
          title="Блок кода"
        >
          {'{ }'}
        </button>
      </div>
      <EditorContent editor={editor} />
      {!editor.getText() && (
        <div className="tiptap-placeholder">{placeholder}</div>
      )}
    </div>
  )
})

export default RichTextEditor
