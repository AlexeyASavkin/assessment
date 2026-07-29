# language: ru
Функционал: Управление компетенциями, разделами и темами

  Предыстория:
    Допустим администратор авторизован

  Сценарий: Создание и получение компетенции
    Когда отправляю POST "/api/admin/competencies" с JSON
      """
      {"name":"Java","description":"Java description"}
      """
    Тогда статус ответа 201
    И JSON содержит "name" = "Java"
    И сохраняю "id" как "competencyId"

  Сценарий: Список компетенций
    Когда отправляю GET "/api/admin/competencies"
    Тогда статус ответа 200
    И JSON содержит хотя бы один элемент с "name" = "Java"

  Сценарий: Создание раздела в компетенции
    Допустим существует "competencyId"
    Когда отправляю POST "/api/admin/competencies/{competencyId}/sections" с JSON
      """
      {"name":"Java Core"}
      """
    Тогда статус ответа 201
    И сохраняю "id" как "sectionId"

  Сценарий: Создание темы в разделе
    Допустим существует "sectionId"
    Когда отправляю POST "/api/admin/sections/{sectionId}/topics" с JSON
      """
      {"name":"Stream API"}
      """
    Тогда статус ответа 201
    И сохраняю "id" как "topicId"

  Сценарий: Удаление компетенции
    Допустим существует "competencyId"
    Когда отправляю DELETE "/api/admin/competencies/{competencyId}"
    Тогда статус ответа 204
    Когда отправляю GET "/api/admin/competencies/{competencyId}"
    Тогда статус ответа 404
