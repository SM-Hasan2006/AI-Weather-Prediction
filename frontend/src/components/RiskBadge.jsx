import { riskMeta } from '../lib/risk.js'

/** Colour AND text, so the meaning never depends on colour alone. */
export default function RiskBadge({ category, size = 'md' }) {
  const m = riskMeta(category)
  const pad = size === 'lg' ? 'px-4 py-2 text-base' : 'px-2.5 py-1 text-xs'
  return (
    <span className={`inline-flex items-center gap-2 rounded-full border ${m.ring} bg-navy-950 font-semibold ${m.text} ${pad}`}>
      <span className={`h-2.5 w-2.5 rounded-full ${m.dot}`} aria-hidden="true" />
      {m.label} risk
    </span>
  )
}
