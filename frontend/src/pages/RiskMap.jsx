import { useEffect, useState } from 'react'
import { CircleMarker, MapContainer, Popup, TileLayer } from 'react-leaflet'
import { api } from '../lib/api.js'
import { PRESET_LOCATIONS } from '../lib/locations.js'
import { riskMeta } from '../lib/risk.js'
import { fmtMm } from '../lib/format.js'
import { useLocation } from '../context/LocationContext.jsx'
import { Link } from 'react-router-dom'
import { Loading, PageHeader, Panel } from '../components/ui.jsx'
import RiskBadge from '../components/RiskBadge.jsx'

export default function RiskMap() {
  const { select } = useLocation()
  const [rows, setRows] = useState(null)

  useEffect(() => {
    let live = true
    Promise.all(
      PRESET_LOCATIONS.map((l) =>
        api.risk(l).then(
          (risk) => ({ ...l, risk }),
          (error) => ({ ...l, error: error.message }),
        ),
      ),
    ).then((r) => live && setRows(r))
    return () => { live = false }
  }, [])

  const ok = rows?.filter((r) => r.risk) ?? []
  const failed = rows?.filter((r) => r.error) ?? []

  return (
    <div className="space-y-6">
      <PageHeader title="Heavy-rain risk map">
        Rule-based screening of forecast rainfall for major Tamil Nadu cities over the next seven days. This is rainfall screening, not a flood map.
      </PageHeader>

      {!rows && <Loading label="Assessing each city" />}
      {failed.length > 0 && (
        <p role="alert" className="rounded-xl border border-risk-moderate/50 bg-risk-moderate/10 p-3 text-sm">
          Risk could not be assessed for: {failed.map((f) => f.name).join(', ')}. These are shown in grey or omitted — they are not “low risk”. First error: {failed[0].error}
        </p>
      )}

      <Panel>
        <div className="h-[420px] overflow-hidden rounded-xl" role="region" aria-label="Map of risk categories by city">
          <MapContainer center={[10.9, 78.4]} zoom={7} scrollWheelZoom={false} style={{ height: '100%', width: '100%' }}>
            <TileLayer attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors' url="https://tile.openstreetmap.org/{z}/{x}/{y}.png" />
            {(rows ?? []).map((r) => {
              const meta = riskMeta(r.risk?.category)
              return (
                <CircleMarker key={r.name} center={[r.latitude, r.longitude]} radius={12} pathOptions={{ color: meta.hex, fillColor: meta.hex, fillOpacity: 0.7 }}>
                  <Popup>
                    <strong>{r.name}</strong><br />
                    {r.risk ? `${meta.label} risk` : 'Risk unavailable'}
                  </Popup>
                </CircleMarker>
              )
            })}
          </MapContainer>
        </div>
      </Panel>

      <Panel title="City list">
        <ul className="divide-y divide-navy-700">
          {(rows ?? []).map((r) => (
            <li key={r.name} className="flex flex-wrap items-center justify-between gap-3 py-3">
              <div>
                <div className="font-medium">{r.name}</div>
                {r.risk && <div className="muted">Peak day {fmtMm(r.risk.maxDailyPrecipMm)} · {r.risk.reasons[0]}</div>}
                {r.error && <div className="muted">Unavailable: {r.error}</div>}
              </div>
              <div className="flex items-center gap-3">
                {r.risk && <RiskBadge category={r.risk.category} />}
                <Link to="/" className="btn-ghost px-3 py-1.5" onClick={() => select(r)}>Open dashboard</Link>
              </div>
            </li>
          ))}
        </ul>
        {ok.length > 0 && <p className="muted mt-3">Map tiles © OpenStreetMap contributors. Method and thresholds are shown on each city’s dashboard.</p>}
      </Panel>
    </div>
  )
}
