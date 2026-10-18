import { useState } from 'react'
import { Bar, CartesianGrid, ComposedChart, Line, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api } from '../lib/api.js'
import { useApi } from '../hooks/useApi.js'
import { useLocation } from '../context/LocationContext.jsx'
import LocationPicker from '../components/LocationPicker.jsx'
import { ErrorState, Loading, PageHeader, Panel } from '../components/ui.jsx'
import { fmtInstant, fmtMm, shortDate } from '../lib/format.js'

const axis = { stroke: '#8397bd', fontSize: 12 }

export default function History() {
  const { location } = useLocation()
  const [days, setDays] = useState(30)
  const { data: h, error, loading, reload } = useApi(() => api.history(location, days), [location.latitude, location.longitude, days])
  const rows = h?.daily?.map((d) => ({ date: shortDate(d.date), observed: d.precipitationSumMm, forecast: d.storedForecastPrecipitationMm }))
  const total = h?.daily?.reduce((s, d) => s + (d.precipitationSumMm || 0), 0)
  const compared = h?.daily?.filter((d) => d.storedForecastPrecipitationMm != null).length ?? 0

  return (
    <div className="space-y-6">
      <PageHeader title="Historical weather">Past daily rainfall for the selected place, with earlier forecasts overlaid wherever this system saved one.</PageHeader>
      <LocationPicker />
      <div>
        <label htmlFor="days" className="label">Period</label>
        <select id="days" className="input max-w-[200px]" value={days} onChange={(e) => setDays(Number(e.target.value))}>
          {[7, 14, 30, 60, 90].map((d) => <option key={d} value={d}>Last {d} days</option>)}
        </select>
      </div>
      {loading && <Loading label="Loading history" />}
      {error && <ErrorState error={error} onRetry={reload} title="History unavailable" />}
      {h && (
        <>
          <Panel title="Daily rainfall">
            <div style={{ width: '100%', height: 300 }} role="img" aria-label="Historical daily rainfall compared with earlier forecasts">
              <ResponsiveContainer>
                <ComposedChart data={rows} margin={{ top: 8, right: 8, left: -12, bottom: 0 }}>
                  <CartesianGrid stroke="#1c3262" vertical={false} />
                  <XAxis dataKey="date" {...axis} minTickGap={20} />
                  <YAxis {...axis} unit=" mm" />
                  <Tooltip contentStyle={{ background: '#0d1a33', border: '1px solid #2a457f', borderRadius: 12 }} />
                  <Bar dataKey="observed" name="Archive rainfall (mm)" fill="#1fb0d8" radius={[3, 3, 0, 0]} />
                  <Line dataKey="forecast" name="Saved forecast (mm)" stroke="#facc15" strokeWidth={2} dot connectNulls={false} />
                </ComposedChart>
              </ResponsiveContainer>
            </div>
            <p className="muted mt-3">Total over the period: <strong className="text-ink">{fmtMm(total)}</strong>. Forecast comparison available for {compared} of {h.daily.length} days.</p>
          </Panel>
          <Panel title="About this data">
            <p className="text-sm text-ink-soft">
              {h.dataDescription} Source: <a className="text-sky-300 underline" href={h.providerUrl} target="_blank" rel="noreferrer">{h.provider}</a>, retrieved {fmtInstant(h.fetchedAt)}. The most recent days can be missing or revised because archive data arrives with a delay. A saved forecast appears only if someone viewed this location on the day before.
            </p>
          </Panel>
        </>
      )}
    </div>
  )
}
