import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import AlertCard from './AlertCard.jsx'
import RiskBadge from './RiskBadge.jsx'
import RainRibbon from './RainRibbon.jsx'
import { StaleNotice } from './SourceNote.jsx'
import { forecastFixture } from '../test/fixtures.js'

const base = { title: 'Heavy rain', message: 'msg', severity: 'ORANGE', affectedAreas: 'Chennai', validFrom: '2026-10-18T00:00:00Z', validTo: '2026-10-19T00:00:00Z', sourceName: 'IMD', sourceUrl: 'https://mausam.imd.gov.in/' }

describe('RiskBadge', () => {
  it.each([['LOW', 'Low risk'], ['MODERATE', 'Moderate risk'], ['HIGH', 'High risk'], ['VERY_HIGH', 'Very high risk'], ['INSUFFICIENT_DATA', 'Insufficient data risk']])('%s shows text, not only colour', (c, text) => {
    render(<RiskBadge category={c} />)
    expect(screen.getByText(text)).toBeInTheDocument()
  })
})

describe('AlertCard labelling', () => {
  it('marks administrator-entered official alerts as not automatically verified', () => {
    render(<AlertCard alert={{ ...base, origin: 'ADMIN_ENTERED_OFFICIAL' }} />)
    expect(screen.getByText(/not automatically verified/i)).toBeInTheDocument()
  })
  it('marks system estimates as NOT an official warning', () => {
    render(<AlertCard alert={{ ...base, origin: 'SYSTEM_GENERATED' }} />)
    expect(screen.getByText(/NOT an official warning/)).toBeInTheDocument()
  })
  it('shows a visible TEST label for test alerts', () => {
    render(<AlertCard alert={{ ...base, origin: 'ADMIN_ENTERED_OFFICIAL', isTest: true }} />)
    expect(screen.getByText('TEST ALERT')).toBeInTheDocument()
  })
})

describe('RainRibbon', () => {
  it('describes each day in text and shows a dash for missing rainfall', () => {
    const f = forecastFixture()
    f.daily[1].precipitationSumMm = null
    render(<RainRibbon daily={f.daily} perDayRisk={[{ date: f.daily[0].date, category: 'HIGH' }]} />)
    expect(screen.getAllByRole('img')).toHaveLength(7)
    expect(screen.getByLabelText(/Today.*High risk/)).toBeInTheDocument()
    expect(screen.getAllByText('—').length).toBeGreaterThan(0)
  })
})

describe('StaleNotice', () => {
  it('warns when cached data is shown', () => {
    render(<StaleNotice forecast={forecastFixture({ stale: true })} />)
    expect(screen.getByRole('alert')).toHaveTextContent(/last saved forecast/)
  })
  it('renders nothing for fresh data', () => {
    const { container } = render(<StaleNotice forecast={forecastFixture()} />)
    expect(container).toBeEmptyDOMElement()
  })
})
