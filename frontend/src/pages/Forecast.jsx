import { api } from '../lib/api.js'
import { useApi } from '../hooks/useApi.js'
import { useLocation } from '../context/LocationContext.jsx'
import LocationPicker from '../components/LocationPicker.jsx'
import { ErrorState, Loading, PageHeader, Panel } from '../components/ui.jsx'
import DailyCards from '../components/DailyCards.jsx'
import SourceNote, { StaleNotice } from '../components/SourceNote.jsx'
import { dayLabel, fmtMm, fmtPct, fmtTemp, fmtWind, shortDate } from '../lib/format.js'
import { describeWeather } from '../lib/weatherCodes.js'

export default function Forecast() {
  const { location } = useLocation()
  const { data: f, error, loading, reload } = useApi(() => api.forecast(location), [location.latitude, location.longitude])
  return (
    <div className="space-y-6">
      <PageHeader title="Seven-day forecast">Today, tomorrow, the day after and the rest of the week for the selected place.</PageHeader>
      <LocationPicker />
      {loading && <Loading label="Fetching the forecast" />}
      {error && <ErrorState error={error} onRetry={reload} title="Forecast unavailable" />}
      {f && (
        <>
          <StaleNotice forecast={f} />
          <Panel title="Daily overview"><DailyCards daily={f.daily} /></Panel>
          <Panel title="All measurements">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[640px] text-left text-sm">
                <caption className="sr-only">Daily forecast values</caption>
                <thead className="text-ink-mute">
                  <tr>{['Day', 'Conditions', 'High', 'Low', 'Rain', 'Chance of rain', 'Max wind'].map((h) => <th key={h} scope="col" className="pb-2 pr-4 font-medium">{h}</th>)}</tr>
                </thead>
                <tbody>
                  {f.daily.map((d) => (
                    <tr key={d.date} className="border-t border-navy-700">
                      <th scope="row" className="py-2 pr-4 font-medium">{dayLabel(d.date, f.daily[0].date)} <span className="text-ink-mute">{shortDate(d.date)}</span></th>
                      <td className="pr-4">{describeWeather(d.weatherCode).label}</td>
                      <td className="pr-4">{fmtTemp(d.temperatureMaxC)}C</td>
                      <td className="pr-4">{fmtTemp(d.temperatureMinC)}C</td>
                      <td className="pr-4">{fmtMm(d.precipitationSumMm)}</td>
                      <td className="pr-4">{fmtPct(d.precipitationProbabilityMaxPct)}</td>
                      <td>{fmtWind(d.windSpeedMaxKmh)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Panel>
          <SourceNote forecast={f} />
        </>
      )}
    </div>
  )
}
