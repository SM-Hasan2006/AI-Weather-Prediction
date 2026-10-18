import { Link } from 'react-router-dom'
import { Droplets, Thermometer, Wind, Gauge } from 'lucide-react'
import { api } from '../lib/api.js'
import { useApi } from '../hooks/useApi.js'
import { useLocation } from '../context/LocationContext.jsx'
import LocationPicker from '../components/LocationPicker.jsx'
import { ErrorState, Loading, Panel, Stat } from '../components/ui.jsx'
import RainRibbon from '../components/RainRibbon.jsx'
import DailyCards from '../components/DailyCards.jsx'
import HourlyChart from '../components/HourlyChart.jsx'
import RiskPanel from '../components/RiskPanel.jsx'
import AlertCard from '../components/AlertCard.jsx'
import SourceNote, { StaleNotice } from '../components/SourceNote.jsx'
import { describeWeather } from '../lib/weatherCodes.js'
import { fmtMm, fmtPct, fmtTemp, fmtWind } from '../lib/format.js'

export default function Dashboard() {
  const { location } = useLocation()
  const deps = [location.latitude, location.longitude]
  const forecast = useApi(() => api.forecast(location), deps)
  const risk = useApi(() => api.risk(location), deps)
  const alerts = useApi(() => api.alerts(location), deps)

  const f = forecast.data
  const today = f?.daily?.[0]?.date
  const todayHours = f?.hourly?.filter((h) => h.time.startsWith(today)) ?? []
  const cur = f?.current
  const weather = cur ? describeWeather(cur.weatherCode) : null
  const latestAlerts = alerts.data ? [...alerts.data.official, ...alerts.data.systemGenerated].slice(0, 3) : []

  return (
    <div className="space-y-6">
      <LocationPicker />

      {forecast.loading && <Loading label="Fetching the latest forecast" />}
      {forecast.error && <ErrorState error={forecast.error} onRetry={forecast.reload} title="Forecast unavailable" />}

      {f && (
        <>
          <StaleNotice forecast={f} />
          <div className="grid gap-6 lg:grid-cols-5">
            <Panel className="lg:col-span-3" title="Right now">
              <div className="flex flex-wrap items-center gap-6">
                <div>
                  <div className="font-display text-6xl font-semibold leading-none" data-testid="current-temp">{fmtTemp(cur?.temperatureC)}C</div>
                  {weather && (
                    <div className="mt-2 flex items-center gap-2 text-ink-soft">
                      <weather.Icon className="h-5 w-5 text-sky-400" aria-hidden="true" /> {weather.label}
                    </div>
                  )}
                </div>
                <div className="grid grid-cols-2 gap-x-8 gap-y-4">
                  <Stat icon={Droplets} label="Rain now" value={fmtMm(cur?.precipitationMm)} />
                  <Stat icon={Wind} label="Wind" value={fmtWind(cur?.windSpeedKmh)} />
                  <Stat icon={Gauge} label="Humidity" value={fmtPct(cur?.relativeHumidityPct)} />
                  <Stat icon={Thermometer} label="Today" value={`${fmtTemp(f.daily[0]?.temperatureMaxC)} / ${fmtTemp(f.daily[0]?.temperatureMinC)}`} />
                </div>
              </div>
              <div className="mt-6 grid grid-cols-2 gap-4 border-t border-navy-700 pt-4">
                <Stat label="Expected rain today" value={fmtMm(f.daily[0]?.precipitationSumMm)} />
                <Stat label="Chance of rain today" value={fmtPct(f.daily[0]?.precipitationProbabilityMaxPct)} />
              </div>
            </Panel>

            <Panel className="lg:col-span-2" title="Heavy-rain risk (next 7 days)">
              {risk.loading && <Loading label="Assessing risk" />}
              {risk.error && <ErrorState error={risk.error} onRetry={risk.reload} title="Risk assessment unavailable" />}
              {risk.data && <RiskPanel risk={risk.data} compact />}
            </Panel>
          </div>

          <Panel title="Seven-day rainfall" action={<Link className="text-sm text-sky-300 underline" to="/forecast">Full forecast</Link>}>
            <RainRibbon daily={f.daily} perDayRisk={risk.data?.perDay} />
            <p className="muted mt-3">Bar height is forecast rainfall; the coloured cap is the rule-based risk screening for that day. The percentage is the chance of any measurable rain, not an amount.</p>
          </Panel>

          <Panel title="Today, hour by hour">
            <HourlyChart hourly={todayHours} />
          </Panel>

          <Panel title="Next seven days">
            <DailyCards daily={f.daily} />
          </Panel>
        </>
      )}

      <Panel title="Latest alerts" action={<Link className="text-sm text-sky-300 underline" to="/alerts">Alert centre</Link>}>
        {alerts.loading && <Loading label="Checking alerts" />}
        {alerts.error && <ErrorState error={alerts.error} onRetry={alerts.reload} title="Alerts unavailable" />}
        {alerts.data && latestAlerts.length === 0 && (
          <p className="text-sm text-ink-soft">No active alerts are recorded for this area. This is not confirmation that conditions are safe — check the official IMD warnings.</p>
        )}
        <div className="space-y-3">{latestAlerts.map((a) => <AlertCard key={`${a.origin}-${a.id ?? a.title}`} alert={a} />)}</div>
      </Panel>

      <SourceNote forecast={f} />
    </div>
  )
}
