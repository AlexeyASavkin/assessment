import { type FormEvent, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { adminLogin } from '../../api/admin'
import { useAuth } from '../../context/AuthContext'

/**
 * Страница входа администратора.
 * Форма с логином и паролем. После успешной авторизации перенаправляет на панель администратора.
 */
export default function AdminLogin() {
  const navigate = useNavigate()
  const location = useLocation()
  const { login, refresh } = useAuth()
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  const from = (location.state as { from?: string } | null)?.from || '/admin'

  /**
   * Обрабатывает отправку формы входа.
   * Вызывает API авторизации, обновляет состояние аутентификации и перенаправляет администратора.
   */
  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setIsLoading(true)
    try {
      await adminLogin(username, password)
      // Verify actual auth state with a protected endpoint probe
      await refresh()
      login()
      navigate(from, { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка входа')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="container">
      <h1>Вход в админку</h1>
      <div className="card">
        <form onSubmit={handleSubmit} className="admin-form">
          <div className="form-field">
            <label htmlFor="username">Логин</label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              required
            />
          </div>
          <div className="form-field">
            <label htmlFor="password">Пароль</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
            />
          </div>
          {error && <p className="error-text">{error}</p>}
          <button type="submit" className="btn btn-primary" disabled={isLoading}>
            {isLoading ? 'Вход...' : 'Войти'}
          </button>
        </form>
      </div>
    </div>
  )
}
