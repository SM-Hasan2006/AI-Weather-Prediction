import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import { PRESET_LOCATIONS, sameLocation } from '../lib/locations.js'

const KEY_SELECTED = 'aiw.selected'
const KEY_SAVED = 'aiw.saved'

function read(key, fallback) {
  try {
    const raw = window.localStorage.getItem(key)
    return raw ? JSON.parse(raw) : fallback
  } catch {
    return fallback
  }
}
function write(key, value) {
  try {
    window.localStorage.setItem(key, JSON.stringify(value))
  } catch {
    /* storage may be disabled; the app still works for this session */
  }
}

const LocationContext = createContext(null)

export function LocationProvider({ children }) {
  const [location, setLocation] = useState(() => read(KEY_SELECTED, PRESET_LOCATIONS[0]))
  const [saved, setSaved] = useState(() => read(KEY_SAVED, []))

  const select = useCallback((l) => {
    const clean = { name: l.name, admin1: l.admin1 || null, latitude: l.latitude, longitude: l.longitude }
    setLocation(clean)
    write(KEY_SELECTED, clean)
  }, [])

  const toggleSaved = useCallback((l) => {
    setSaved((prev) => {
      const exists = prev.some((s) => sameLocation(s, l))
      const next = exists ? prev.filter((s) => !sameLocation(s, l)) : [...prev, { name: l.name, admin1: l.admin1 || null, latitude: l.latitude, longitude: l.longitude }]
      write(KEY_SAVED, next)
      return next
    })
  }, [])

  const value = useMemo(() => ({ location, saved, select, toggleSaved }), [location, saved, select, toggleSaved])
  return <LocationContext.Provider value={value}>{children}</LocationContext.Provider>
}

export const useLocation = () => useContext(LocationContext)
