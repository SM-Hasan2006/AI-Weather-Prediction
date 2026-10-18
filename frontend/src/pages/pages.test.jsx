import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { LocationProvider } from '../context/LocationContext.jsx'
import { alertsFixture, forecastFixture, riskFixture } from '../test/fixtures.js'

vi.mock('../lib/api.js', () => ({
  api: { forecast: vi.fn(), risk: vi.fn(), alerts: vi.fn(), searchLocations: vi.fn(), admin: { list: vi.fn() } },
}))
import { api } from '../lib/api.js'
import Dashboard from './Dashboard.jsx'
import Admin from './Admin.jsx'

const renderPage = (ui) => render(<MemoryRouter><LocationProvider>{ui}</LocationProvider></MemoryRouter>)

beforeEach(() => {
  vi.clearAllMocks()
  window.localStorage.clear()
})

describe('Dashboard', () => {
  it('shows forecast, risk with its method, and the source note', async () => {
    api.forecast.mockResolvedValue(forecastFixture())
    api.risk.mockResolvedValue(riskFixture('HIGH'))
    api.alerts.mockResolvedValue(alertsFixture())
    renderPage(<Dashboard />)
    expect(await screen.findByTestId('current-temp')).toHaveTextContent('29°C')
    expect(await screen.findByText('High risk')).toBeInTheDocument()
    expect(screen.getByText(/Method: RULE_BASED/)).toBeInTheDocument()
    expect(screen.getByText(/Forecast data from/)).toBeInTheDocument()
    expect(screen.getByText(/No active alerts are recorded/)).toBeInTheDocument()
  })

  it('shows an honest error and a retry button when the forecast API fails', async () => {
    api.forecast.mockRejectedValue(Object.assign(new Error('The weather provider is not reachable right now.'), { status: 503 }))
    api.risk.mockRejectedValue(new Error('x'))
    api.alerts.mockResolvedValue(alertsFixture())
    renderPage(<Dashboard />)
    expect(await screen.findByText('Forecast unavailable')).toBeInTheDocument()
    expect(screen.getByText('The weather provider is not reachable right now.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument()
    expect(screen.queryByTestId('current-temp')).not.toBeInTheDocument()
  })

  it('warns when the forecast is served from cache', async () => {
    api.forecast.mockResolvedValue(forecastFixture({ stale: true }))
    api.risk.mockResolvedValue(riskFixture('LOW'))
    api.alerts.mockResolvedValue(alertsFixture())
    renderPage(<Dashboard />)
    expect(await screen.findByText(/last saved forecast/)).toBeInTheDocument()
  })

  it('shows an explicit insufficient-data state instead of a guessed category', async () => {
    api.forecast.mockResolvedValue(forecastFixture())
    api.risk.mockResolvedValue({ ...riskFixture('INSUFFICIENT_DATA'), reasons: ['No daily rainfall values were returned.'] })
    api.alerts.mockResolvedValue(alertsFixture())
    renderPage(<Dashboard />)
    expect(await screen.findByText('Insufficient data risk')).toBeInTheDocument()
    expect(screen.getByText(/did not return enough rainfall data/)).toBeInTheDocument()
  })

  it('keeps official and system alerts visibly different', async () => {
    api.forecast.mockResolvedValue(forecastFixture())
    api.risk.mockResolvedValue(riskFixture('HIGH'))
    const a = { title: 'T', message: 'm', severity: 'RED', affectedAreas: 'Chennai', validFrom: '2026-10-18T00:00:00Z', validTo: '2026-10-19T00:00:00Z', sourceName: 'IMD', sourceUrl: 'https://mausam.imd.gov.in/' }
    api.alerts.mockResolvedValue(alertsFixture({ official: [{ ...a, id: 1, origin: 'ADMIN_ENTERED_OFFICIAL' }], systemGenerated: [{ ...a, origin: 'SYSTEM_GENERATED' }] }))
    renderPage(<Dashboard />)
    expect(await screen.findByText(/not automatically verified/)).toBeInTheDocument()
    expect(screen.getByText(/NOT an official warning/)).toBeInTheDocument()
  })

  it('searches for a location and selects it', async () => {
    api.forecast.mockResolvedValue(forecastFixture())
    api.risk.mockResolvedValue(riskFixture('LOW'))
    api.alerts.mockResolvedValue(alertsFixture())
    api.searchLocations.mockResolvedValue([{ name: 'Tenkasi', admin1: 'Tamil Nadu', admin2: 'Tenkasi', countryCode: 'IN', latitude: 8.96, longitude: 77.31 }])
    renderPage(<Dashboard />)
    await userEvent.type(screen.getByLabelText(/search for a city/i), 'Tenkasi')
    await userEvent.click(screen.getByRole('button', { name: /^search$/i }))
    await userEvent.click(await screen.findByRole('button', { name: /Tenkasi/ }))
    await waitFor(() => expect(screen.getByTestId('selected-location')).toHaveTextContent('Tenkasi, Tamil Nadu'))
    await waitFor(() => expect(api.forecast).toHaveBeenLastCalledWith(expect.objectContaining({ latitude: 8.96 })))
  })
})

describe('Admin', () => {
  it('rejects wrong credentials with a clear message', async () => {
    api.admin.list.mockRejectedValue(Object.assign(new Error('Unauthorized'), { status: 401 }))
    renderPage(<Admin />)
    await userEvent.type(screen.getByLabelText('Username'), 'admin')
    await userEvent.type(screen.getByLabelText('Password'), 'nope')
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }))
    expect(await screen.findByText('Wrong username or password.')).toBeInTheDocument()
    expect(screen.queryByText('New alert')).not.toBeInTheDocument()
  })
})

describe('Alerts page', () => {
  it('says system estimates are unavailable instead of claiming there are none', async () => {
    const { default: Alerts } = await import('./Alerts.jsx')
    api.alerts.mockResolvedValue(alertsFixture({ systemGeneratedNote: 'System estimates are unavailable because the forecast could not be loaded: provider down' }))
    renderPage(<Alerts />)
    expect(await screen.findByText(/System estimates are unavailable/)).toBeInTheDocument()
    expect(screen.queryByText(/found no High or Very high rainfall/)).not.toBeInTheDocument()
  })
})
