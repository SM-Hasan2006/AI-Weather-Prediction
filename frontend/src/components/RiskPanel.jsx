import RiskBadge from './RiskBadge.jsx'
import { fmtInstant, fmtMm, fmtPct, shortDate } from '../lib/format.js'

export default function RiskPanel({ risk, compact = false }) {
  const t = risk.thresholds
  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-3">
        <RiskBadge category={risk.category} size="lg" />
        <span className="muted">
          {shortDate(risk.windowStart)} – {shortDate(risk.windowEnd)}
        </span>
      </div>

      {risk.category === 'INSUFFICIENT_DATA' && (
        <p className="text-sm text-ink-soft">The provider did not return enough rainfall data to assess this period, so no category is given.</p>
      )}

      <div>
        <h3 className="mb-1 text-sm font-semibold">Why this category</h3>
        <ul className="list-disc space-y-1 pl-5 text-sm text-ink-soft">
          {risk.reasons.map((r) => <li key={r}>{r}</li>)}
        </ul>
      </div>

      {!compact && t && (
        <div className="rounded-xl border border-navy-700 bg-navy-950 p-3 text-sm">
          <h3 className="mb-1 font-semibold">Screening thresholds used (daily rainfall)</h3>
          <p className="text-ink-soft">
            Moderate ≥ {fmtMm(t.moderateDailyMm)} · High ≥ {fmtMm(t.highDailyMm)} · Very high ≥ {fmtMm(t.veryHighDailyMm)}; hourly intensity High ≥ {fmtMm(t.highHourlyMm)}; rain-chance gate ≥ {fmtPct(t.minProbabilityPct)}.
          </p>
          <p className="mt-1 text-ink-mute">{t.note}</p>
        </div>
      )}

      <p className="text-xs text-ink-mute">
        Method: {risk.method} v{risk.methodVersion} · Source: {risk.source} · Assessed {fmtInstant(risk.assessedAt)}.{' '}
        {risk.disclaimer}
      </p>
    </div>
  )
}
