import { dayLabel, fmtMm, fmtPct, shortDate } from '../lib/format.js'
import { riskMeta } from '../lib/risk.js'

/**
 * Seven daily columns. Bar height = forecast rainfall (mm), the cap colour = rule-based risk
 * category for that day. Each bar carries its numbers as text so nothing relies on colour alone.
 */
export default function RainRibbon({ daily = [], perDayRisk = [] }) {
  const byDate = Object.fromEntries(perDayRisk.map((d) => [d.date, d.category]))
  const max = Math.max(20, ...daily.map((d) => d.precipitationSumMm || 0))
  const today = daily[0]?.date
  return (
    <ol className="grid grid-cols-7 gap-1.5 sm:gap-3" aria-label="Seven-day rainfall forecast">
      {daily.slice(0, 7).map((d) => {
        const mm = d.precipitationSumMm
        const h = mm == null ? 0 : Math.max(4, (mm / max) * 100)
        const cat = byDate[d.date]
        const cap = cat ? riskMeta(cat).hex : '#38c9ee'
        return (
          <li key={d.date} className="flex flex-col items-center gap-1 text-center">
            <span className="text-xs font-semibold text-ink-soft">{fmtMm(mm)}</span>
            <div className="flex h-36 w-full items-end rounded-lg bg-navy-950 p-1" role="img"
                 aria-label={`${dayLabel(d.date, today)} ${shortDate(d.date)}: ${fmtMm(mm)} rain, ${fmtPct(d.precipitationProbabilityMaxPct)} chance${cat ? `, ${riskMeta(cat).label} risk` : ''}`}>
              <div className="w-full rounded-md bg-sky-500/70" style={{ height: `${h}%`, borderTop: `4px solid ${cap}` }} />
            </div>
            <span className="text-xs text-ink-mute">{fmtPct(d.precipitationProbabilityMaxPct)}</span>
            <span className="text-xs font-semibold">{dayLabel(d.date, today)}</span>
          </li>
        )
      })}
    </ol>
  )
}
