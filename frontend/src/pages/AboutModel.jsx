import { useState } from 'react'
import { api } from '../lib/api.js'
import { useApi } from '../hooks/useApi.js'
import { useLocation } from '../context/LocationContext.jsx'
import { ErrorState, Loading, PageHeader, Panel } from '../components/ui.jsx'
import { fmtMm } from '../lib/format.js'

function Metric({ label, value }) {
  return <div><div className="muted">{label}</div><div className="font-semibold">{value}</div></div>
}
const num = (v, d = 3) => (typeof v === 'number' ? v.toFixed(d) : '—')

export default function AboutModel() {
  const { location } = useLocation()
  const status = useApi(() => api.mlStatus(), [])
  const [pred, setPred] = useState({ state: 'idle' })

  async function run() {
    setPred({ state: 'loading' })
    try {
      setPred({ state: 'done', data: await api.mlPredict(location) })
    } catch (e) {
      setPred({ state: 'error', error: e })
    }
  }

  const info = status.data?.model
  const reg = info?.regression
  const clf = info?.classification
  const regTest = reg?.test_metrics?.[reg?.selected_model]
  const clfTest = clf?.test_metrics?.[clf?.selected_model]

  return (
    <div className="space-y-6">
      <PageHeader title="About the prediction model">Three different things appear in this application. Knowing which is which matters.</PageHeader>

      <Panel title="1 · Provider forecast">
        <p className="text-sm text-ink-soft">Rainfall, temperature and wind forecasts come from the weather provider’s numerical models (Open-Meteo). The application displays them; it does not alter them.</p>
      </Panel>
      <Panel title="2 · Rule-based risk screening">
        <p className="text-sm text-ink-soft">The heavy-rain risk category comes from fixed, configurable rainfall thresholds applied to the provider forecast. It is a transparent screening rule, not a trained model, and not an official warning. Thresholds are shown next to every assessment.</p>
      </Panel>
      <Panel title="3 · Trained machine-learning model">
        <p className="mb-4 text-sm text-ink-soft">A separate model trained on past daily weather estimates tomorrow’s rainfall from the last two weeks of observations. It looks only one day ahead and has no access to forecasts.</p>
        {status.loading && <Loading label="Checking model status" />}
        {status.error && <ErrorState error={status.error} onRetry={status.reload} title="Status unavailable" />}
        {status.data && !status.data.available && (
          <div className="rounded-xl border border-risk-moderate/50 bg-risk-moderate/10 p-4 text-sm">
            <strong>Model not available.</strong> {status.data.reason} No accuracy figures are shown because none exist yet.
          </div>
        )}
        {info?.available && (
          <div className="space-y-5">
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
              <Metric label="Version" value={info.model_version} />
              <Metric label="Trained" value={info.trained_at_utc?.slice(0, 10)} />
              <Metric label="Data" value={`${info.data.date_range[0]} → ${info.data.date_range[1]}`} />
              <Metric label="Test period" value={`${info.split.test[0]} → ${info.split.test[1]}`} />
            </div>
            <div>
              <h3 className="mb-2 font-semibold">Rainfall amount (test period)</h3>
              {regTest ? (
                <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
                  <Metric label="MAE" value={fmtMm(regTest.mae_mm)} />
                  <Metric label="RMSE" value={fmtMm(regTest.rmse_mm)} />
                  <Metric label="Baseline MAE (tomorrow = today)" value={fmtMm(reg.test_metrics['baseline_persistence (tomorrow = today)']?.mae_mm)} />
                  <Metric label="Baseline MAE (average)" value={fmtMm(reg.test_metrics.baseline_train_mean?.mae_mm)} />
                </div>
              ) : <p className="muted">No regression results.</p>}
            </div>
            <div>
              <h3 className="mb-2 font-semibold">Heavy-rain detection (≥ {clf?.heavy_rain_threshold_mm} mm, test period)</h3>
              {clf?.status !== 'trained' ? (
                <p className="text-sm text-ink-soft">Not trained: {clf?.detail || clf?.status}</p>
              ) : clfTest ? (
                <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
                  <Metric label="Precision" value={num(clfTest.precision)} />
                  <Metric label="Recall" value={num(clfTest.recall)} />
                  <Metric label="F1" value={num(clfTest.f1)} />
                  <Metric label="PR-AUC" value={num(clfTest.pr_auc)} />
                  <Metric label="Missed heavy days (FN)" value={`${clfTest.confusion_matrix.fn} of ${clfTest.positives}`} />
                  <Metric label="False alarms (FP)" value={clfTest.confusion_matrix.fp} />
                </div>
              ) : null}
            </div>
            <ul className="list-disc space-y-1 pl-5 text-sm text-ink-mute">
              {info.limitations.map((l) => <li key={l}>{l}</li>)}
            </ul>
          </div>
        )}

        <div className="mt-6 border-t border-navy-700 pt-4">
          <button type="button" className="btn-primary" onClick={run} disabled={pred.state === 'loading'}>Run model for {location.name}</button>
          {pred.state === 'loading' && <Loading label="Running the model" />}
          {pred.state === 'error' && <div className="mt-3"><ErrorState error={pred.error} title="Prediction failed" /></div>}
          {pred.state === 'done' && (
            <div className="mt-4 space-y-2 text-sm" aria-live="polite">
              {pred.data.rainfall && <p>Estimated rainfall on {pred.data.rainfall.target_date}: <strong>{fmtMm(pred.data.rainfall.predicted_rainfall_mm)}</strong></p>}
              {pred.data.heavyRain && <p>Heavy-rain flag for {pred.data.heavyRain.target_date}: <strong>{pred.data.heavyRain.predicted_heavy_rain ? 'flagged' : 'not flagged'}</strong> (score {pred.data.heavyRain.score}; not a calibrated probability)</p>}
              {pred.data.messages?.map((m) => <p key={m} className="text-ink-mute">{m}</p>)}
              <p className="text-ink-mute">Model estimate from past observations only. Not an official warning.</p>
            </div>
          )}
        </div>
      </Panel>
    </div>
  )
}
