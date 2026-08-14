/**
 * Барrel-модуль административного API.
 * Все функции и типы сгруппированы по доменам в отдельных модулях:
 * @see ./shared — общие типы и HTTP-хелперы
 * @see ./authApi — аутентификация администратора
 * @see ./competenciesApi — компетенции
 * @see ./sectionsApi — разделы
 * @see ./topicsApi — темы
 * @see ./employeesApi — сотрудники
 * @see ./tokensApi — пригласительные токены
 * @see ./aiSettingsApi — настройки ИИ и промты
 * @see ./questionsApi — банк вопросов
 * @see ./applicationsApi — заявки и отчёты
 */

export * from './aiSettingsApi'
export * from './applicationsApi'
export * from './authApi'
export * from './competenciesApi'
export * from './employeesApi'
export * from './questionsApi'
export * from './sectionsApi'
export * from './shared'
export * from './tokensApi'
export * from './topicsApi'
