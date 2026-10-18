import { Bar, CartesianGrid, ComposedChart, Line, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api } from '../lib/api.js'
import { useApi } from '../hooks/useApi.js'
import { useLocation } from '../context/LocationContext.jsx'
import LocationPicker from '../components/LocationPicker.jsx'
import { ErrorState, Loading, PageHeader, Panel } from '../components/ui.jsx'
import HourlyChart from '../components/HourlyChart.jsx'
import SourceNote, { StaleNotice } from '../components/SourceNote.jsx'
import { dayLabel, shortDate } from '../lib/format.js'

const axis = { stroke: '#8397bd', fontSize: 12 }

export default function Rainfall() {
  const { location } = useLocation()
  const { data: f, error, loading, reload } = useApi(() => api.forecast(location), [location.latitude, location.longitude])
  const daily = f?.daily?.map((d) => ({ day: `${dayLabel(d.date, f.daily[0].date)} ${shortDate(d.date)}`, mm: d.precipitationSumMm, prob: d.precipitationProbabilityMaxPct }))
  const total = f?.daily?.reduce((s, d) => s + (d.precipitationSumMm || 0), 0)
  const hourly48 = f?.hourly?.slice(0, 48)

  return (
    <div className="space-y-6">
      <PageHeader title="Rainfall analysis">How much rain is expected, when it is likely to fall, and how sure the forecast is that it will rain at all.</PageHeader>
      <LocationPicker />
      {loading && <Loading label="Fetching rainfall data" />}
      {error && <ErrorState error={error} onRetry={reload} title="Rainfall data unavailable" />}
      {f && (
        <>
          <StaleNotice forecast={f} />
          <Panel title="Daily rainfall and chance of rain">
            <div style={{ width: '100%', height: 300 }} role="img" aria-label="Daily forecast rainfall in millimetres and chance of rain">
              <ResponsiveContainer>
                <ComposedChart data={daily} margin={{ top: 8, right: 8, left: -12, bottom: 0 }}>
                  <CartesianGrid stroke="#1c3262" vertical={false} />
                  <XAxis dataKey="day" {...axis} />
                  <YAxis yAxisId="mm" {...axis} unit=" mm" />
                  <YAxis yAxisId="p" orientation="right" domain={[0, 100]} {...axis} unit="%" />
                  <Tooltip contentStyle={{ background: '#0d1a33', border: '1px solid #2a457f', borderRadius: 12 }} />
                  <Bar yAxisId="mm" dataKey="mm" name="Rain (mm)" fill="#1fb0d8" radius={[4, 4, 0, 0]} />
                  <Line yAxisId="p" dataKey="prob" name="Chance of rain (%)" stroke="#7fdcf5" strokeWidth={2} />
                </ComposedChart>
              </ResponsiveContainer>
            </div>
            <p className="muted mt-3">Forecast total for the period: <strong className="text-ink">{total?.toFixed(1)} mm</strong>.</p>
          </Panel>
          <Panel title="Next 48 hours"><HourlyChart hourly={hourly48} /></Panel>
          <Panel title="How to read this">
            <ul className="list-disc space-y-2 pl-5 text-sm text-ink-soft">
              <li><strong>Chance of rain</strong> is the probability that measurable rain falls. It says nothing about how much. A 90 % chance can mean a drizzle.</li>
              <li><strong>Rainfall (mm)</strong> is the forecast amount. Amounts, especially in thunderstorms, are the least reliable part of a forecast, and local totals can differ a lot from the grid-cell value.</li>
              <li>Heavy rain does not automatically mean flooding. Flooding depends on terrain, drainage, rivers and recent rain, which this application does not model.</li>
            </ul>
          </Panel>
          <SourceNote forecast={f} />
        </>
      )}
    </div>
  )
}
