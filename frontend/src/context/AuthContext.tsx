import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react'
import { checkAuth } from '../api/admin'

/**
 * Значение контекста аутентификации администратора.
 * Содержит состояние авторизации, флаг проверки и функции управления сессией.
 */
interface AuthContextValue {
  isAuthenticated: boolean
  isChecking: boolean
  login: () => void
  logout: () => void
  refresh: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

/**
 * Провайдер контекста аутентификации.
 * При монтировании проверяет активную сессию администратора.
 * Предоставляет дочерним компонентам состояние и функции входа/выхода.
 * @param props.children - дочерние React-элементы
 * @return JSX-элемент провайдера контекста
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [isChecking, setIsChecking] = useState(true)

  const refresh = useCallback(async () => {
    setIsChecking(true)
    const ok = await checkAuth()
    setIsAuthenticated(ok)
    setIsChecking(false)
  }, [])

  useEffect(() => {
    refresh()
  }, [refresh])

  const login = useCallback(() => {
    setIsAuthenticated(true)
    setIsChecking(false)
  }, [])

  const logout = useCallback(() => {
    setIsAuthenticated(false)
  }, [])

  return (
    <AuthContext.Provider value={{ isAuthenticated, isChecking, login, logout, refresh }}>
      {children}
    </AuthContext.Provider>
  )
}

/**
 * Хук для доступа к контексту аутентификации администратора.
 * Должен использоваться внутри AuthProvider.
 * @return текущее значение контекста аутентификации
 * @throws Error если используется вне AuthProvider
 */
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}