import { NavLink, Route, Routes } from 'react-router-dom'
import { CloudRainWind } from 'lucide-react'
import Dashboard from './pages/Dashboard.jsx'
import Forecast from './pages/Forecast.jsx'
import Rainfall from './pages/Rainfall.jsx'
import RiskMap from './pages/RiskMap.jsx'
import Alerts from './pages/Alerts.jsx'
import History from './pages/History.jsx'
import AboutModel from './pages/AboutModel.jsx'
import Admin from './pages/Admin.jsx'

const NAV = [
  ['/', 'Dashboard'],
  ['/forecast', '7-day forecast'],
  ['/rainfall', 'Rainfall'],
  ['/risk-map', 'Risk map'],
  ['/alerts', 'Alerts'],
  ['/history', 'History'],
  ['/about-model', 'About the model'],
]

export default function App() {
  return (
    <div className="min-h-screen">
      <a href="#main" className="sr-only focus:not-sr-only focus:absolute focus:left-3 focus:top-3 focus:z-50 focus:rounded-lg focus:bg-sky-400 focus:px-3 focus:py-2 focus:text-navy-950">
        Skip to content
      </a>
      <header className="border-b border-navy-700/70 bg-navy-900/80 backdrop-blur">
        <div className="mx-auto flex max-w-6xl flex-col gap-3 px-4 py-4">
          <div className="flex items-center gap-3">
            <CloudRainWind className="h-7 w-7 text-sky-400" aria-hidden="true" />
            <div>
              <div className="font-display text-lg font-semibold leading-tight">AI Weather &amp; Heavy Rain Prediction System</div>
              <div className="text-xs text-ink-mute">Rainfall forecasts, heavy-rain risk screening and alert centre</div>
            </div>
          </div>
          <nav aria-label="Main" className="-mx-1 flex gap-1 overflow-x-auto pb-1">
            {NAV.map(([to, label]) => (
              <NavLink
                key={to}
                to={to}
                end={to === '/'}
                className={({ isActive }) =>
                  `whitespace-nowrap rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${isActive ? 'bg-sky-400/15 text-sky-300' : 'text-ink-soft hover:bg-navy-800'}`
                }
              >
                {label}
              </NavLink>
            ))}
          </nav>
        </div>
      </header>

      <main id="main" className="mx-auto max-w-6xl px-4 py-6">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/forecast" element={<Forecast />} />
          <Route path="/rainfall" element={<Rainfall />} />
          <Route path="/risk-map" element={<RiskMap />} />
          <Route path="/alerts" element={<Alerts />} />
          <Route path="/history" element={<History />} />
          <Route path="/about-model" element={<AboutModel />} />
          <Route path="/admin" element={<Admin />} />
          <Route path="*" element={<p className="p-6">Page not found.</p>} />
        </Routes>
      </main>

      <footer className="mx-auto max-w-6xl px-4 pb-10 text-xs text-ink-mute">
        Decision-support tool only. It is not an official disaster-warning service and cannot tell you that a place is safe. For official warnings use the{' '}
        <a className="text-sky-300 underline" href="https://mausam.imd.gov.in/" target="_blank" rel="noreferrer">India Meteorological Department</a>{' '}
        and your local authorities. <NavLink className="underline" to="/admin">Administrator</NavLink>
      </footer>
    </div>
  )
}
