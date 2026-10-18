import { describe, expect, it } from 'vitest'
import { toApiError } from './api.js'

describe('toApiError', () => {
  it('uses the backend message when the server responded', () => {
    const e = toApiError({ response: { status: 503, data: { code: 'weather_provider_unavailable', message: 'Provider down' } } })
    expect(e.message).toBe('Provider down')
    expect(e.status).toBe(503)
    expect(e.code).toBe('weather_provider_unavailable')
  })
  it('explains a network failure', () => {
    const e = toApiError({ message: 'Network Error' })
    expect(e.code).toBe('network_error')
    expect(e.message).toMatch(/Cannot reach the weather API/)
  })
  it('explains a timeout', () => {
    expect(toApiError({ code: 'ECONNABORTED' }).message).toMatch(/too long/)
  })
})
