// Quick-pick shortcuts for the Tamil Nadu focus. These only pre-fill the selector; the
// forecast always comes from the weather provider. Use search for any other place.
export const PRESET_LOCATIONS = [
  { name: 'Chennai', admin1: 'Tamil Nadu', latitude: 13.0827, longitude: 80.2707 },
  { name: 'Coimbatore', admin1: 'Tamil Nadu', latitude: 11.0168, longitude: 76.9558 },
  { name: 'Madurai', admin1: 'Tamil Nadu', latitude: 9.9252, longitude: 78.1198 },
  { name: 'Tiruchirappalli', admin1: 'Tamil Nadu', latitude: 10.7905, longitude: 78.7047 },
  { name: 'Salem', admin1: 'Tamil Nadu', latitude: 11.6643, longitude: 78.146 },
  { name: 'Cuddalore', admin1: 'Tamil Nadu', latitude: 11.748, longitude: 79.7714 },
  { name: 'Nagapattinam', admin1: 'Tamil Nadu', latitude: 10.7672, longitude: 79.8449 },
  { name: 'Thoothukudi', admin1: 'Tamil Nadu', latitude: 8.7642, longitude: 78.1348 },
  { name: 'Tirunelveli', admin1: 'Tamil Nadu', latitude: 8.7139, longitude: 77.7567 },
  { name: 'Vellore', admin1: 'Tamil Nadu', latitude: 12.9165, longitude: 79.1325 },
]

export const sameLocation = (a, b) =>
  Math.abs(a.latitude - b.latitude) < 1e-4 && Math.abs(a.longitude - b.longitude) < 1e-4

export function locationLabel(l) {
  return [l.name, l.admin1].filter(Boolean).join(', ')
}
