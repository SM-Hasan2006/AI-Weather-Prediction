// Display metadata for risk categories. Category assignment itself happens in the backend risk engine.
export const RISK_META = {
  LOW: { label: 'Low', dot: 'bg-risk-low', text: 'text-risk-low', ring: 'border-risk-low/50', hex: '#34d399', rank: 0 },
  MODERATE: { label: 'Moderate', dot: 'bg-risk-moderate', text: 'text-risk-moderate', ring: 'border-risk-moderate/50', hex: '#facc15', rank: 1 },
  HIGH: { label: 'High', dot: 'bg-risk-high', text: 'text-risk-high', ring: 'border-risk-high/50', hex: '#fb923c', rank: 2 },
  VERY_HIGH: { label: 'Very high', dot: 'bg-risk-veryHigh', text: 'text-risk-veryHigh', ring: 'border-risk-veryHigh/50', hex: '#f87171', rank: 3 },
  INSUFFICIENT_DATA: { label: 'Insufficient data', dot: 'bg-risk-none', text: 'text-risk-none', ring: 'border-risk-none/50', hex: '#94a3b8', rank: -1 },
}

export const riskMeta = (category) => RISK_META[category] || RISK_META.INSUFFICIENT_DATA

export const ALERT_SEVERITY = {
  INFO: { label: 'Information', hex: '#7fdcf5' },
  YELLOW: { label: 'Yellow', hex: '#facc15' },
  ORANGE: { label: 'Orange', hex: '#fb923c' },
  RED: { label: 'Red', hex: '#f87171' },
}
