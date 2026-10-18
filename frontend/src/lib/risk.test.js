import { describe, expect, it } from 'vitest'
import { RISK_META, riskMeta } from './risk.js'

describe('risk display metadata', () => {
  it('uses green / yellow / orange / red for the four categories', () => {
    expect(RISK_META.LOW.hex).toBe('#34d399')
    expect(RISK_META.MODERATE.hex).toBe('#facc15')
    expect(RISK_META.HIGH.hex).toBe('#fb923c')
    expect(RISK_META.VERY_HIGH.hex).toBe('#f87171')
  })
  it('never maps an unknown category to "low"', () => {
    expect(riskMeta(undefined).label).toBe('Insufficient data')
    expect(riskMeta('SOMETHING_NEW').label).toBe('Insufficient data')
  })
})
