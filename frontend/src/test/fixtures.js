// TEST FIXTURES ONLY - used by automated tests, never by the running application.
export const forecastFixture = (over = {}) => ({
  location: { latitude: 13.08, longitude: 80.27, timezone: 'Asia/Kolkata' },
  provider: 'Open-Meteo', providerUrl: 'https://open-meteo.com/', fetchedAt: new Date().toISOString(),
  forecastStart: '2026-10-18', forecastEnd: '2026-10-24', stale: false,
  current: { time: '2026-10-18T10:00', temperatureC: 29.4, relativeHumidityPct: 78, precipitationMm: 0, windSpeedKmh: 12, weatherCode: 3 },
  daily: Array.from({ length: 7 }, (_, i) => ({
    date: `2026-10-${18 + i}`, weatherCode: 61, temperatureMaxC: 31, temperatureMinC: 25,
    precipitationSumMm: 5 + i * 10, precipitationProbabilityMaxPct: 60, windSpeedMaxKmh: 20,
  })),
  hourly: Array.from({ length: 24 }, (_, h) => ({
    time: `2026-10-18T${String(h).padStart(2, '0')}:00`, temperatureC: 28, precipitationMm: 0.2, precipitationProbabilityPct: 40, windSpeedKmh: 10, weatherCode: 3,
  })),
  ...over,
})

export const riskFixture = (category = 'HIGH') => ({
  category, method: 'RULE_BASED', methodVersion: '1.0', source: 'Open-Meteo forecast', assessedAt: new Date().toISOString(),
  windowStart: '2026-10-18', windowEnd: '2026-10-24', maxDailyPrecipMm: 65, reasons: ['Forecast rainfall of 65 mm on 2026-10-23 reaches the High screening threshold.'],
  perDay: [{ date: '2026-10-23', category }],
  thresholds: { moderateDailyMm: 15.6, highDailyMm: 64.5, veryHighDailyMm: 115.6, highHourlyMm: 30, minProbabilityPct: 40, note: 'Screening thresholds, not official warnings.' },
  disclaimer: 'Rainfall screening only; flood risk is not assessed.',
})

export const alertsFixture = (over = {}) => ({
  official: [], systemGenerated: [], officialLinks: [{ label: 'IMD', url: 'https://mausam.imd.gov.in/' }], disclaimer: 'Not official.', ...over,
})
