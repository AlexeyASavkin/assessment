import { type ReactNode, useState } from 'react'
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
  const [collapsed, setCollapsed] = useState<boolean>(
    () => localStorage.getItem('sidebar-collapsed') === 'true'
  )

  const handleLogout = () => {
    logout()
    navigate('/admin/login', { replace: true })
  }

  const toggleCollapsed = () => {
    setCollapsed((prev) => {
      const next = !prev
      localStorage.setItem('sidebar-collapsed', String(next))
      return next
    })
  }

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `admin-nav-link${isActive ? ' admin-nav-link-active' : ''}`

  const navItems = [
    { to: '/admin', label: 'Дашборд', end: true },
    { to: '/admin/competencies', label: 'Компетенции', end: false },
    { to: '/admin/employees', label: 'Заявки', end: false },
    { to: '/admin/tokens', label: 'Токены', end: false },
    { to: '/admin/settings', label: 'Настройки ИИ', end: false },
  ]

  return (
    <div className="admin-shell">
      <aside className={`admin-sidebar${collapsed ? ' admin-sidebar--collapsed' : ''}`}>
        <button
          className="admin-sidebar-toggle"
          onClick={toggleCollapsed}
          title={collapsed ? 'Развернуть' : 'Свернуть'}
          aria-label={collapsed ? 'Развернуть' : 'Свернуть'}
        >
          {collapsed ? '»' : '«'}
        </button>
        <h2 className="admin-sidebar-title">Админка</h2>
        <nav className="admin-nav">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={linkClass}
              title={collapsed ? item.label : undefined}
            >
              {collapsed ? item.label.charAt(0) : item.label}
            </NavLink>
          ))}
        </nav>
        <button
          className="btn btn-danger admin-logout"
          onClick={handleLogout}
          title={collapsed ? 'Выйти' : undefined}
        >
          {collapsed ? '✕' : 'Выйти'}
        </button>
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