import { type ReactNode } from 'react'
import { Navigate, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

export function ProtectedRoute() {
  const { isAuthenticated, isChecking } = useAuth()

  if (isChecking) {
    return (
      <div className="container">
        <p>Проверка авторизации...</p>
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/admin/login" replace />
  }

  return <Outlet />
}

export function AdminLayout() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/admin/login', { replace: true })
  }

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `admin-nav-link${isActive ? ' admin-nav-link-active' : ''}`

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <h2 className="admin-sidebar-title">Админка</h2>
        <nav className="admin-nav">
          <NavLink to="/admin" end className={linkClass}>Дашборд</NavLink>
          <NavLink to="/admin/competencies" className={linkClass}>Компетенции</NavLink>
          <NavLink to="/admin/employees" className={linkClass}>Сотрудники</NavLink>
          <NavLink to="/admin/tokens" className={linkClass}>Токены</NavLink>
        </nav>
        <button className="btn btn-danger admin-logout" onClick={handleLogout}>Выйти</button>
      </aside>
      <main className="admin-content">
        <Outlet />
      </main>
    </div>
  )
}

export function AdminPageWrapper({ children }: { children: ReactNode }) {
  return <div className="container admin-container">{children}</div>
}