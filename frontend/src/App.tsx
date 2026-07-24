import { BrowserRouter, Routes, Route } from 'react-router-dom'
import EmployeeSession from './pages/EmployeeSession'
import EmployeeReport from './pages/EmployeeReport'
import AdminLogin from './pages/admin/AdminLogin'
import AdminDashboard from './pages/admin/AdminDashboard'
import AdminCompetenciesTreePage from './pages/admin/AdminCompetenciesTreePage'
import EmployeesPage from './pages/admin/EmployeesPage'
import TokensPage from './pages/admin/TokensPage'
import AiSettingsPage from './pages/admin/AiSettingsPage'
import QuestionsPage from './pages/admin/QuestionsPage'
import { AuthProvider } from './context/AuthContext'
import { ProtectedRoute, AdminLayout } from './components/admin/AdminLayout'

/**
 * Корневой компонент приложения.
 * Настраивает React Router, проверяет браузер (только Chrome),
 * и определяет маршруты для сотрудников и администраторов.
 * @return JSX-элемент с маршрутизацией или предупреждением о браузере
 */
function App() {
  const isChrome = navigator.userAgent.includes('Chrome') && !navigator.userAgent.includes('Edg')

  if (!isChrome) {
    return (
      <div className="container">
        <h1>Предупреждение</h1>
        <p>Приложение работает лучше в Google Chrome. Пожалуйста, используйте Chrome для голосового ввода.</p>
      </div>
    )
  }

  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* Employee routes */}
          <Route path="/session/:sessionId" element={<EmployeeSession />} />
          <Route path="/session/:sessionId/report" element={<EmployeeReport />} />

          {/* Admin login (public) */}
          <Route path="/admin/login" element={<AdminLogin />} />

          {/* Protected admin routes */}
          <Route element={<ProtectedRoute />}>
            <Route element={<AdminLayout />}>
              <Route path="/admin" element={<AdminDashboard />} />
              <Route path="/admin/competencies" element={<AdminCompetenciesTreePage />} />
              <Route path="/admin/competencies/:competencyId/questions" element={<QuestionsPage />} />
              <Route path="/admin/employees" element={<EmployeesPage />} />
              <Route path="/admin/tokens" element={<TokensPage />} />
              <Route path="/admin/settings" element={<AiSettingsPage />} />
            </Route>
          </Route>

          <Route path="*" element={<div className="container"><h1>Ассессмент компетенций</h1></div>} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App