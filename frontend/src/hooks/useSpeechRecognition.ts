import { useState, useEffect, useRef, useCallback } from 'react'

interface SpeechRecognitionEvent extends Event {
  results: SpeechRecognitionResultList
  resultIndex: number
}

interface SpeechRecognitionInstance extends EventTarget {
  continuous: boolean
  interimResults: boolean
  lang: string
  start(): void
  stop(): void
  abort(): void
  onresult: (event: SpeechRecognitionEvent) => void
  onerror: (event: Event) => void
  onend: () => void
}

declare global {
  interface Window {
    webkitSpeechRecognition: new () => SpeechRecognitionInstance
  }
}

export function useSpeechRecognition() {
  const [isRecording, setIsRecording] = useState(false)
  const [interimTranscript, setInterimTranscript] = useState('')
  const [finalTranscript, setFinalTranscript] = useState('')
  const recognitionRef = useRef<SpeechRecognitionInstance | null>(null)

  useEffect(() => {
    if (typeof window !== 'undefined' && 'webkitSpeechRecognition' in window) {
      const recognition = new window.webkitSpeechRecognition()
      recognition.continuous = true
      recognition.interimResults = true
      recognition.lang = 'ru-RU'

      recognition.onresult = (event: SpeechRecognitionEvent) => {
        let interim = ''
        let final = ''

        for (let i = event.resultIndex; i < event.results.length; i++) {
          const result = event.results[i]
          if (!result) continue
          const transcript = result[0]?.transcript ?? ''
          if (result.isFinal) {
            final += transcript
          } else {
            interim += transcript
          }
        }

        if (final) {
          setFinalTranscript(prev => prev + final)
        }
        setInterimTranscript(interim)
      }

      recognition.onerror = () => {
        setIsRecording(false)
      }

      recognition.onend = () => {
        setIsRecording(false)
      }

      recognitionRef.current = recognition
    }

    return () => {
      recognitionRef.current?.abort()
    }
  }, [])

  const startRecording = useCallback(() => {
    if (recognitionRef.current) {
      setInterimTranscript('')
      recognitionRef.current.start()
      setIsRecording(true)
    }
  }, [])

  const stopRecording = useCallback(() => {
    if (recognitionRef.current) {
      recognitionRef.current.stop()
      setIsRecording(false)
    }
  }, [])

  const resetTranscript = useCallback(() => {
    setFinalTranscript('')
    setInterimTranscript('')
  }, [])

  return {
    isRecording,
    interimTranscript,
    finalTranscript,
    startRecording,
    stopRecording,
    resetTranscript,
    setFinalTranscript,
  }
}
