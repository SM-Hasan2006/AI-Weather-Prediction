import { Droplets, Wind } from 'lucide-react'
import { dayLabel, fmtMm, fmtPct, fmtTemp, fmtWind, shortDate } from '../lib/format.js'
import { describeWeather } from '../lib/weatherCodes.js'

export default function DailyCards({ daily = [] }) {
  const today = daily[0]?.date
  return (
    <ul className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-7">
      {daily.slice(0, 7).map((d) => {
        const { label, Icon } = describeWeather(d.weatherCode)
        return (
          <li key={d.date} className="rounded-xl border border-navy-700 bg-navy-950 p-3">
            <div className="text-sm font-semibold">{dayLabel(d.date, today)}</div>
            <div className="muted">{shortDate(d.date)}</div>
            <Icon className="my-2 h-7 w-7 text-sky-400" aria-label={label} role="img" />
            <div className="font-display text-lg">
              {fmtTemp(d.temperatureMaxC)} <span className="text-ink-mute">{fmtTemp(d.temperatureMinC)}</span>
            </div>
            <div className="mt-2 space-y-1 text-xs text-ink-soft">
              <div className="flex items-center gap-1.5"><Droplets className="h-3.5 w-3.5" aria-hidden="true" />{fmtMm(d.precipitationSumMm)} · {fmtPct(d.precipitationProbabilityMaxPct)}</div>
              <div className="flex items-center gap-1.5"><Wind className="h-3.5 w-3.5" aria-hidden="true" />{fmtWind(d.windSpeedMaxKmh)}</div>
            </div>
          </li>
        )
      })}
    </ul>
  )
}
