import { ExternalLink, Info } from 'lucide-react'
import { fmtAge, fmtInstant, shortDate } from '../lib/format.js'

export function StaleNotice({ forecast }) {
  if (!forecast?.stale) return null
  return (
    <div role="alert" className="mb-4 rounded-xl border border-risk-moderate/50 bg-risk-moderate/10 p-3 text-sm">
      The weather provider could not be reached, so this is the <strong>last saved forecast</strong> fetched {fmtAge(forecast.fetchedAt)}. Do not treat it as current.
    </div>
  )
}

export default function SourceNote({ forecast }) {
  if (!forecast) return null
  return (
    <footer className="mt-6 flex gap-3 rounded-2xl border border-navy-700 bg-navy-900/60 p-4 text-sm text-ink-soft">
      <Info className="mt-0.5 h-4 w-4 shrink-0 text-sky-400" aria-hidden="true" />
      <div className="space-y-1">
        <p>
          Forecast data from{' '}
          <a className="inline-flex items-center gap-1 text-sky-300 underline" href={forecast.providerUrl} target="_blank" rel="noreferrer">
            {forecast.provider} <ExternalLink className="h-3 w-3" aria-hidden="true" />
          </a>
          . Fetched {fmtInstant(forecast.fetchedAt)} ({fmtAge(forecast.fetchedAt)}). Period {shortDate(forecast.forecastStart)} – {shortDate(forecast.forecastEnd)}, times in {forecast.location?.timezone}.
        </p>
        <p className="text-ink-mute">
          Forecasts are model estimates and can be wrong, especially for rainfall amounts and timing. This application is a decision-support tool, not an official warning service.
        </p>
      </div>
    </footer>
  )
}
