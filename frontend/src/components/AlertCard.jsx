import { ExternalLink, ShieldAlert, Cpu } from 'lucide-react'
import { ALERT_SEVERITY } from '../lib/risk.js'
import { fmtInstant } from '../lib/format.js'

const ORIGIN = {
  ADMIN_ENTERED_OFFICIAL: {
    label: 'Official source · entered by an administrator (not automatically verified)',
    Icon: ShieldAlert,
  },
  SYSTEM_GENERATED: {
    label: 'System estimate from forecast data · NOT an official warning',
    Icon: Cpu,
  },
}

export default function AlertCard({ alert }) {
  const sev = ALERT_SEVERITY[alert.severity] || ALERT_SEVERITY.INFO
  const origin = ORIGIN[alert.origin] || ORIGIN.SYSTEM_GENERATED
  const Icon = origin.Icon
  return (
    <article className="rounded-xl border border-navy-700 bg-navy-950 p-4" style={{ borderLeft: `4px solid ${sev.hex}` }}>
      <div className="flex flex-wrap items-center gap-2 text-xs">
        {alert.isTest && <span className="rounded-full bg-sky-400 px-2 py-0.5 font-semibold text-navy-950">TEST ALERT</span>}
        <span className="rounded-full border px-2 py-0.5 font-semibold" style={{ borderColor: sev.hex, color: sev.hex }}>{sev.label}</span>
        <span className="flex items-center gap-1 text-ink-soft"><Icon className="h-3.5 w-3.5" aria-hidden="true" />{origin.label}</span>
      </div>
      <h3 className="mt-2 text-base font-semibold">{alert.title}</h3>
      <p className="mt-1 whitespace-pre-line text-sm text-ink-soft">{alert.message}</p>
      <dl className="mt-3 grid gap-x-6 gap-y-1 text-xs text-ink-mute sm:grid-cols-2">
        <div><dt className="inline">Areas: </dt><dd className="inline text-ink-soft">{alert.affectedAreas}</dd></div>
        <div><dt className="inline">Valid: </dt><dd className="inline text-ink-soft">{fmtInstant(alert.validFrom)} – {fmtInstant(alert.validTo)}</dd></div>
        {alert.issuedAt && <div><dt className="inline">Issued: </dt><dd className="inline text-ink-soft">{fmtInstant(alert.issuedAt)}</dd></div>}
        {alert.sourceUrl && (
          <div>
            <dt className="inline">Source: </dt>
            <dd className="inline">
              <a className="inline-flex items-center gap-1 text-sky-300 underline" href={alert.sourceUrl} target="_blank" rel="noreferrer">
                {alert.sourceName} <ExternalLink className="h-3 w-3" aria-hidden="true" />
              </a>
            </dd>
          </div>
        )}
      </dl>
    </article>
  )
}
