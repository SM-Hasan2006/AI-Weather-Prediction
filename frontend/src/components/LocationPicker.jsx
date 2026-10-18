import { useState } from 'react'
import { MapPin, Search, Star } from 'lucide-react'
import { api } from '../lib/api.js'
import { PRESET_LOCATIONS, locationLabel, sameLocation } from '../lib/locations.js'
import { useLocation } from '../context/LocationContext.jsx'

export default function LocationPicker() {
  const { location, saved, select, toggleSaved } = useLocation()
  const [q, setQ] = useState('')
  const [results, setResults] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const isSaved = saved.some((s) => sameLocation(s, location))

  async function search(e) {
    e.preventDefault()
    if (q.trim().length < 2) return
    setBusy(true)
    setError(null)
    try {
      setResults(await api.searchLocations(q.trim()))
    } catch (err) {
      setResults(null)
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  function choose(l) {
    select(l)
    setResults(null)
    setQ('')
  }

  const chips = [...saved, ...PRESET_LOCATIONS.filter((p) => !saved.some((s) => sameLocation(s, p)))]

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center gap-3">
        <div className="flex items-center gap-2 text-lg font-semibold">
          <MapPin className="h-5 w-5 text-sky-400" aria-hidden="true" />
          <span data-testid="selected-location">{locationLabel(location)}</span>
        </div>
        <button
          type="button"
          className="btn-ghost px-3 py-1.5"
          aria-pressed={isSaved}
          onClick={() => toggleSaved(location)}
        >
          <Star className={`h-4 w-4 ${isSaved ? 'fill-risk-moderate text-risk-moderate' : ''}`} aria-hidden="true" />
          {isSaved ? 'Saved' : 'Save location'}
        </button>
      </div>

      <form onSubmit={search} className="flex max-w-xl gap-2" role="search">
        <label htmlFor="loc-search" className="sr-only">Search for a city or district</label>
        <input
          id="loc-search"
          className="input"
          placeholder="Search a city or district, e.g. Madurai"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <button type="submit" className="btn-primary" disabled={busy || q.trim().length < 2}>
          <Search className="h-4 w-4" aria-hidden="true" /> Search
        </button>
      </form>

      {error && <p role="alert" className="text-sm text-risk-veryHigh">{error}</p>}
      {results && results.length === 0 && <p className="muted">No places match “{q}”.</p>}
      {results && results.length > 0 && (
        <ul className="max-w-xl divide-y divide-navy-700 overflow-hidden rounded-xl border border-navy-700 bg-navy-950" aria-label="Search results">
          {results.map((r) => (
            <li key={`${r.latitude},${r.longitude}`}>
              <button type="button" className="w-full px-3 py-2 text-left text-sm hover:bg-navy-800" onClick={() => choose(r)}>
                <span className="font-medium">{r.name}</span>
                <span className="text-ink-mute">{[r.admin2, r.admin1, r.countryCode].filter(Boolean).map((x) => `, ${x}`).join('')}</span>
              </button>
            </li>
          ))}
        </ul>
      )}

      <div className="flex flex-wrap gap-2" aria-label="Quick locations">
        {chips.map((c) => (
          <button
            key={`${c.latitude},${c.longitude}`}
            type="button"
            onClick={() => select(c)}
            aria-pressed={sameLocation(c, location)}
            className={`rounded-full border px-3 py-1 text-sm transition-colors ${
              sameLocation(c, location) ? 'border-sky-400 bg-sky-400/15 text-sky-300' : 'border-navy-600 text-ink-soft hover:bg-navy-800'
            }`}
          >
            {c.name}
          </button>
        ))}
      </div>
    </div>
  )
}
