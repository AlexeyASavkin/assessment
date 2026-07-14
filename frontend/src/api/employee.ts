const API_BASE = '/api'

export async function getCurrentQuestion(sessionId: string) {
  const response = await fetch(`${API_BASE}/employee/sessions/${sessionId}/questions`, {
    credentials: 'include',
  })
  if (!response.ok) throw new Error('Failed to get question')
  return response.json()
}

export async function submitAnswer(sessionId: string, questionAttemptId: string, rawTranscript: string, finalTranscript: string) {
  const response = await fetch(`${API_BASE}/employee/sessions/${sessionId}/answers`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ questionAttemptId, rawTranscript, finalTranscript }),
  })
  if (!response.ok) throw new Error('Failed to submit answer')
  return response.json()
}

export async function getReport(sessionId: string) {
  const response = await fetch(`${API_BASE}/employee/sessions/${sessionId}/report`, {
    credentials: 'include',
  })
  if (!response.ok) throw new Error('Failed to get report')
  return response.json()
}
