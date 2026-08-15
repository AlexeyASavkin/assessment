import { adminJson, type Employee } from './shared'

/**
 * Возвращает список всех сотрудников.
 * @return массив сотрудников
 */
export async function listEmployees(): Promise<Employee[]> {
  return adminJson<Employee[]>('/employees')
}

/**
 * Получает сотрудника по идентификатору.
 * @param id - идентификатор сотрудника
 * @return объект сотрудника
 */
export async function getEmployee(id: string): Promise<Employee> {
  return adminJson<Employee>(`/employees/${id}`)
}

/**
 * Создает нового сотрудника.
 * @param fullName - полное имя
 * @param position - должность
 * @param department - отдел
 * @param competencyId - идентификатор компетенции (может быть null)
 * @return созданный сотрудник
 */
export async function createEmployee(
  fullName: string,
  position: string,
  department: string,
  competencyId: string | null,
): Promise<Employee> {
  return adminJson<Employee>('/employees', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fullName, position, department, competencyId }),
  })
}

/**
 * Обновляет данные сотрудника.
 * @param id - идентификатор сотрудника
 * @param fullName - полное имя
 * @param position - должность
 * @param department - отдел
 * @param competencyId - идентификатор компетенции (может быть null)
 * @return обновленный сотрудник
 */
export async function updateEmployee(
  id: string,
  fullName: string,
  position: string,
  department: string,
  competencyId: string | null,
): Promise<Employee> {
  return adminJson<Employee>(`/employees/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fullName, position, department, competencyId }),
  })
}

/**
 * Удаляет сотрудника по идентификатору.
 * @param id - идентификатор сотрудника
 */
export async function deleteEmployee(id: string): Promise<void> {
  await adminJson<void>(`/employees/${id}`, { method: 'DELETE' })
}
