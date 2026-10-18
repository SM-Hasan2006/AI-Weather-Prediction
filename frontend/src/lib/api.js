import axios from 'axios'

export const API_BASE_URL = (import.meta.env?.VITE_API_BASE_URL || 'http://localhost:8080/api/v1').replace(/\/$/, '')

const http = axios.create({ baseURL: API_BASE_URL, timeout: 20000 })

/** Turns any axios failure into an Error with a readable message and a machine code. */
export function toApiError(err) {
  if (err?.response) {
    const body = err.response.data || {}
    const e = new Error(body.message || `The server answered with HTTP ${err.response.status}.`)
    e.status = err.response.status
    e.code = body.code || 'http_error'
    return e
  }
  const e = new Error(
    err?.code === 'ECONNABORTED'
      ? 'The weather service took too long to respond. Try again in a moment.'
      : `Cannot reach the weather API at ${API_BASE_URL}. Check that the backend is running.`,
  )
  e.status = 0
  e.code = 'network_error'
  return e
}

async function get(path, params, auth) {
  try {
    return (await http.get(path, { params, auth })).data
  } catch (err) {
    throw toApiError(err)
  }
}

const coords = (l) => ({ latitude: l.latitude, longitude: l.longitude })

export const api = {
  health: () => get('/health'),
  searchLocations: (q) => get('/locations/search', { q }),
  forecast: (l) => get('/weather/forecast', coords(l)),
  history: (l, days) => get('/weather/history', { ...coords(l), days }),
  risk: (l) => get('/risk', coords(l)),
  alerts: (l) => get('/alerts', coords(l)),
  mlStatus: () => get('/ml/status'),
  mlPredict: async (l) => {
    try {
      return (await http.post('/ml/predict', null, { params: coords(l) })).data
    } catch (err) {
      throw toApiError(err)
    }
  },
  admin: {
    list: (auth) => get('/admin/alerts', undefined, auth),
    create: async (auth, body) => send('post', '/admin/alerts', auth, body),
    update: async (auth, id, body) => send('put', `/admin/alerts/${id}`, auth, body),
    remove: async (auth, id) => send('delete', `/admin/alerts/${id}`, auth),
  },
}

async function send(method, url, auth, data) {
  try {
    return (await http.request({ method, url, data, auth })).data
  } catch (err) {
    throw toApiError(err)
  }
}
