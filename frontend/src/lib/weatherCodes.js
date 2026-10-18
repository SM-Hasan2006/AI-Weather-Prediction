import { Cloud, CloudDrizzle, CloudFog, CloudLightning, CloudRain, CloudRainWind, CloudSnow, CloudSun, Sun, CircleHelp } from 'lucide-react'

// WMO weather interpretation codes as used by Open-Meteo.
const TABLE = [
  [[0], 'Clear sky', Sun],
  [[1], 'Mainly clear', Sun],
  [[2], 'Partly cloudy', CloudSun],
  [[3], 'Overcast', Cloud],
  [[45, 48], 'Fog', CloudFog],
  [[51, 53, 55, 56, 57], 'Drizzle', CloudDrizzle],
  [[61, 63, 65, 66, 67], 'Rain', CloudRain],
  [[71, 73, 75, 77, 85, 86], 'Snow', CloudSnow],
  [[80, 81, 82], 'Rain showers', CloudRainWind],
  [[95, 96, 99], 'Thunderstorm', CloudLightning],
]

export function describeWeather(code) {
  const hit = TABLE.find(([codes]) => codes.includes(code))
  return hit ? { label: hit[1], Icon: hit[2] } : { label: 'Unknown', Icon: CircleHelp }
}
