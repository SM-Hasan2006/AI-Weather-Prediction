import { describe, expect, it } from 'vitest'
import { dayLabel, fmtAge, fmtMm, fmtPct, fmtTemp, hourLabel, minutesSince, parseDate, shortDate } from './format.js'

describe('format', () => {
  it('formats numbers and shows a dash for missing values instead of inventing zero', () => {
    expect(fmtMm(2.345)).toBe('2.3 mm')
    expect(fmtMm(64.5)).toBe('65 mm')
    expect(fmtMm(null)).toBe('—')
    expect(fmtMm(undefined)).toBe('—')
    expect(fmtTemp(28.6)).toBe('29°')
    expect(fmtPct(NaN)).toBe('—')
  })
  it('parses dates without timezone drift', () => {
    const d = parseDate('2026-10-18')
    expect([d.getFullYear(), d.getMonth(), d.getDate()]).toEqual([2026, 9, 18])
    expect(shortDate('2026-10-18')).toBe('18 Oct')
  })
  it('labels today and weekdays', () => {
    expect(dayLabel('2026-10-18', '2026-10-18')).toBe('Today')
    expect(dayLabel('2026-10-19', '2026-10-18')).toBe('Mon')
  })
  it('extracts the hour', () => expect(hourLabel('2026-10-18T14:00')).toBe('14:00'))
  it('computes data age', () => {
    const now = Date.parse('2026-10-18T12:00:00Z')
    expect(minutesSince('2026-10-18T11:30:00Z', now)).toBe(30)
    expect(fmtAge('2026-10-18T11:30:00Z', now)).toBe('30 min ago')
    expect(fmtAge('2026-10-18T08:00:00Z', now)).toBe('4 h ago')
    expect(fmtAge('garbage', now)).toBe('unknown')
  })
})
