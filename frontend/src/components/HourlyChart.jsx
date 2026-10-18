import { Bar, CartesianGrid, ComposedChart, Line, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { hourLabel } from '../lib/format.js'

const axis = { stroke: '#8397bd', fontSize: 12 }

export default function HourlyChart({ hourly = [], height = 260 }) {
  if (!hourly.length) return <p className="muted">No hourly data for this period.</p>
  const data = hourly.map((h) => ({
    t: hourLabel(h.time),
    mm: h.precipitationMm,
    prob: h.precipitationProbabilityPct,
  }))
  return (
    <div style={{ width: '100%', height }} role="img" aria-label="Hourly rainfall amount and rain probability">
      <ResponsiveContainer>
        <ComposedChart data={data} margin={{ top: 8, right: 8, left: -12, bottom: 0 }}>
          <CartesianGrid stroke="#1c3262" vertical={false} />
          <XAxis dataKey="t" {...axis} interval="preserveStartEnd" minTickGap={24} />
          <YAxis yAxisId="mm" {...axis} unit=" mm" />
          <YAxis yAxisId="p" orientation="right" domain={[0, 100]} {...axis} unit="%" />
          <Tooltip contentStyle={{ background: '#0d1a33', border: '1px solid #2a457f', borderRadius: 12 }} labelStyle={{ color: '#eaf1ff' }} />
          <Bar yAxisId="mm" dataKey="mm" name="Rain (mm)" fill="#1fb0d8" radius={[4, 4, 0, 0]} />
          <Line yAxisId="p" dataKey="prob" name="Chance of rain (%)" stroke="#7fdcf5" strokeWidth={2} dot={false} />
        </ComposedChart>
      </ResponsiveContainer>
    </div>
  )
}
