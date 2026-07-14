import { Link } from 'react-router-dom'
import { AdminPageWrapper } from '../../components/admin/AdminLayout'

export default function AdminDashboard() {
  const cards = [
    { to: '/admin/competencies', title: 'Компетенции', desc: 'Управление компетенциями, критериями и уровнями требований' },
    { to: '/admin/employees', title: 'Сотрудники', desc: 'Учёт сотрудников и генерация пригласительных ссылок' },
    { to: '/admin/tokens', title: 'Токены', desc: 'Список выданных пригласительных токенов' },
  ]

  return (
    <AdminPageWrapper>
      <h1>Панель администратора</h1>
      <p>Выберите раздел для управления.</p>
      <div className="admin-dashboard-grid">
        {cards.map((c) => (
          <Link key={c.to} to={c.to} className="card admin-dashboard-card">
            <h2>{c.title}</h2>
            <p>{c.desc}</p>
          </Link>
        ))}
      </div>
    </AdminPageWrapper>
  )
}