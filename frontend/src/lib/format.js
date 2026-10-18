const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
const DAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

const isNum = (v) => typeof v === 'number' && Number.isFinite(v)

export const fmtMm = (v) => (isNum(v) ? `${v < 10 ? v.toFixed(1) : Math.round(v)} mm` : '—')
export const fmtTemp = (v) => (isNum(v) ? `${Math.round(v)}°` : '—')
export const fmtPct = (v) => (isNum(v) ? `${Math.round(v)}%` : '—')
export const fmtWind = (v) => (isNum(v) ? `${Math.round(v)} km/h` : '—')

/** 'YYYY-MM-DD' -> Date at local midnight (no timezone shifting). */
export function parseDate(s) {
  const [y, m, d] = String(s).slice(0, 10).split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function dayLabel(dateStr, todayStr) {
  if (todayStr && dateStr === todayStr) return 'Today'
  const d = parseDate(dateStr)
  return DAYS[d.getDay()]
}

export function shortDate(dateStr) {
  const d = parseDate(dateStr)
  return `${d.getDate()} ${MONTHS[d.getMonth()]}`
}

/** 'YYYY-MM-DDTHH:mm' (provider local time) -> '14:00' */
export const hourLabel = (iso) => String(iso).slice(11, 16)

export function fmtInstant(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleString(undefined, { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })
}

export function minutesSince(iso, now = Date.now()) {
  const t = new Date(iso).getTime()
  return Number.isNaN(t) ? null : Math.max(0, Math.round((now - t) / 60000))
}

export function fmtAge(iso, now = Date.now()) {
  const m = minutesSince(iso, now)
  if (m === null) return 'unknown'
  if (m < 1) return 'just now'
  if (m < 60) return `${m} min ago`
  const h = Math.floor(m / 60)
  return h < 24 ? `${h} h ago` : `${Math.floor(h / 24)} d ago`
}
