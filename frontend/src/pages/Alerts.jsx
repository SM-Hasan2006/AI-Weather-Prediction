import { ExternalLink } from 'lucide-react'
import { api } from '../lib/api.js'
import { useApi } from '../hooks/useApi.js'
import { useLocation } from '../context/LocationContext.jsx'
import LocationPicker from '../components/LocationPicker.jsx'
import AlertCard from '../components/AlertCard.jsx'
import { ErrorState, Loading, PageHeader, Panel } from '../components/ui.jsx'

export default function Alerts() {
  const { location } = useLocation()
  const { data, error, loading, reload } = useApi(() => api.alerts(location), [location.latitude, location.longitude])
  return (
    <div className="space-y-6">
      <PageHeader title="Alerts and notifications">Official warnings and the system’s own rainfall estimates are kept apart so you can always tell them apart.</PageHeader>
      <LocationPicker />
      {loading && <Loading label="Checking alerts" />}
      {error && <ErrorState error={error} onRetry={reload} title="Alerts unavailable" />}
      {data && (
        <>
          <Panel title="Official warnings">
            <p className="muted mb-4">Warnings published by an official authority and recorded here by an administrator. They are <strong>not</strong> fetched automatically, so the official site is always the final word.</p>
            {data.official.length === 0 ? (
              <p className="text-sm text-ink-soft">No official warnings are currently recorded for this area. That does not mean there is none — check the official sources below.</p>
            ) : (
              <div className="space-y-3">{data.official.map((a) => <AlertCard key={a.id} alert={a} />)}</div>
            )}
            <h3 className="mb-2 mt-6 text-sm font-semibold">Check the official source</h3>
            <ul className="space-y-1 text-sm">
              {data.officialLinks.map((l) => (
                <li key={l.url}>
                  <a className="inline-flex items-center gap-1 text-sky-300 underline" href={l.url} target="_blank" rel="noreferrer">{l.label} <ExternalLink className="h-3 w-3" aria-hidden="true" /></a>
                </li>
              ))}
            </ul>
          </Panel>

          <Panel title="System estimates (not official)">
            <p className="muted mb-4">Generated from the rainfall forecast with the rule-based screening method. They are produced on request, so the same event is never listed twice.</p>
            {data.systemGeneratedNote ? (
              <p role="alert" className="rounded-xl border border-risk-moderate/50 bg-risk-moderate/10 p-3 text-sm">{data.systemGeneratedNote}</p>
            ) : data.systemGenerated.length === 0 ? (
              <p className="text-sm text-ink-soft">The screening method found no High or Very high rainfall in the next seven days for this location.</p>
            ) : (
              <div className="space-y-3">{data.systemGenerated.map((a, i) => <AlertCard key={i} alert={a} />)}</div>
            )}
          </Panel>
          <p className="muted">{data.disclaimer}</p>
        </>
      )}
    </div>
  )
}
