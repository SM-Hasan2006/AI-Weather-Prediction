import { useState } from 'react'
import { api } from '../lib/api.js'
import AlertCard from '../components/AlertCard.jsx'
import { ErrorState, PageHeader, Panel } from '../components/ui.jsx'

const EMPTY = { title: '', message: '', severity: 'YELLOW', sourceName: 'India Meteorological Department', sourceUrl: '', issuedAt: '', validFrom: '', validTo: '', affectedAreas: '', centerLatitude: '', centerLongitude: '', radiusKm: '', isTest: false }
const toIso = (v) => (v ? new Date(v).toISOString() : null)
const toLocalInput = (iso) => (iso ? new Date(iso).toISOString().slice(0, 16) : '')

/** Administrator-managed alerts. Credentials are held in memory only and sent as HTTP Basic over HTTPS. */
export default function Admin() {
  const [creds, setCreds] = useState({ username: '', password: '' })
  const [auth, setAuth] = useState(null)
  const [alerts, setAlerts] = useState([])
  const [form, setForm] = useState(EMPTY)
  const [editing, setEditing] = useState(null)
  const [error, setError] = useState(null)
  const [notice, setNotice] = useState(null)

  async function refresh(a = auth) {
    setAlerts(await api.admin.list(a))
  }

  async function login(e) {
    e.preventDefault()
    setError(null)
    try {
      await refresh(creds)
      setAuth(creds)
    } catch (err) {
      setError(err.status === 401 ? new Error('Wrong username or password.') : err)
    }
  }

  async function save(e) {
    e.preventDefault()
    setError(null)
    setNotice(null)
    const num = (v) => (v === '' || v === null || v === undefined ? null : Number(v))
    const body = { ...form, issuedAt: toIso(form.issuedAt), validFrom: toIso(form.validFrom), validTo: toIso(form.validTo),
      centerLatitude: num(form.centerLatitude), centerLongitude: num(form.centerLongitude), radiusKm: num(form.radiusKm) }
    try {
      if (editing) await api.admin.update(auth, editing, body)
      else await api.admin.create(auth, body)
      setForm(EMPTY)
      setEditing(null)
      setNotice(editing ? 'Alert updated.' : 'Alert created.')
      await refresh()
    } catch (err) {
      setError(err)
    }
  }

  async function remove(id) {
    if (!window.confirm('Delete this alert?')) return
    try {
      await api.admin.remove(auth, id)
      setNotice('Alert deleted.')
      await refresh()
    } catch (err) {
      setError(err)
    }
  }

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })
  const field = (k, label, props = {}) => (
    <div className={props.wide ? 'sm:col-span-2' : ''}>
      <label className="label" htmlFor={k}>{label}</label>
      {props.area ? <textarea id={k} className="input" rows={3} required value={form[k]} onChange={set(k)} /> : <input id={k} className="input" required={props.required !== false} step={props.type === 'number' ? 'any' : undefined} type={props.type || 'text'} value={form[k]} onChange={set(k)} />}
    </div>
  )

  if (!auth) {
    return (
      <div className="max-w-md space-y-4">
        <PageHeader title="Administrator sign-in">Only authorised administrators can record verified official alerts.</PageHeader>
        <form onSubmit={login} className="card card-pad space-y-4">
          <div><label className="label" htmlFor="u">Username</label><input id="u" className="input" autoComplete="username" value={creds.username} onChange={(e) => setCreds({ ...creds, username: e.target.value })} required /></div>
          <div><label className="label" htmlFor="p">Password</label><input id="p" type="password" className="input" autoComplete="current-password" value={creds.password} onChange={(e) => setCreds({ ...creds, password: e.target.value })} required /></div>
          <button className="btn-primary" type="submit">Sign in</button>
        </form>
        {error && <ErrorState error={error} title="Sign-in failed" />}
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <PageHeader title="Alert management">Record a warning exactly as published by the official source. Always include the source link. Entries are labelled “administrator-entered”, never “verified by IMD”.</PageHeader>
      {notice && <p role="status" className="rounded-xl border border-risk-low/40 bg-risk-low/10 p-3 text-sm">{notice}</p>}
      {error && <ErrorState error={error} title="Request failed" />}

      <Panel title={editing ? `Edit alert #${editing}` : 'New alert'}>
        <form onSubmit={save} className="grid gap-4 sm:grid-cols-2">
          {field('title', 'Title', { wide: true })}
          {field('message', 'Message (as published)', { wide: true, area: true })}
          <div>
            <label className="label" htmlFor="severity">Severity (IMD colour code)</label>
            <select id="severity" className="input" value={form.severity} onChange={set('severity')}>
              <option value="INFO">Information</option><option value="YELLOW">Yellow</option><option value="ORANGE">Orange</option><option value="RED">Red</option>
            </select>
          </div>
          {field('sourceName', 'Source name')}
          {field('sourceUrl', 'Source URL (https://…)', { type: 'url', wide: true })}
          {field('issuedAt', 'Issued at', { type: 'datetime-local' })}
          {field('validFrom', 'Valid from', { type: 'datetime-local' })}
          {field('validTo', 'Valid until', { type: 'datetime-local' })}
          {field('affectedAreas', 'Affected areas (district names)', { wide: true })}
          <fieldset className="grid gap-4 sm:col-span-2 sm:grid-cols-3">
            <legend className="label">Optional map area (fill all three, or leave empty to show the alert for every location)</legend>
            {field('centerLatitude', 'Centre latitude', { type: 'number', required: false })}
            {field('centerLongitude', 'Centre longitude', { type: 'number', required: false })}
            {field('radiusKm', 'Radius (km)', { type: 'number', required: false })}
          </fieldset>
          <label className="flex items-center gap-2 text-sm sm:col-span-2"><input type="checkbox" checked={form.isTest} onChange={set('isTest')} /> Mark as a TEST alert (shown with a visible “TEST ALERT” label)</label>
          <div className="flex gap-3 sm:col-span-2">
            <button className="btn-primary" type="submit">{editing ? 'Save changes' : 'Create alert'}</button>
            {editing && <button type="button" className="btn-ghost" onClick={() => { setEditing(null); setForm(EMPTY) }}>Cancel</button>}
          </div>
        </form>
      </Panel>

      <Panel title="Recorded alerts">
        {alerts.length === 0 && <p className="text-sm text-ink-soft">No alerts yet.</p>}
        <div className="space-y-4">
          {alerts.map((a) => (
            <div key={a.id}>
              <AlertCard alert={a} />
              <div className="mt-2 flex gap-2">
                <button type="button" className="btn-ghost px-3 py-1.5" onClick={() => { setEditing(a.id); setForm({ ...a, centerLatitude: a.centerLatitude ?? '', centerLongitude: a.centerLongitude ?? '', radiusKm: a.radiusKm ?? '', issuedAt: toLocalInput(a.issuedAt), validFrom: toLocalInput(a.validFrom), validTo: toLocalInput(a.validTo) }) }}>Edit</button>
                <button type="button" className="btn-danger px-3 py-1.5" onClick={() => remove(a.id)}>Delete</button>
              </div>
            </div>
          ))}
        </div>
      </Panel>
      <button type="button" className="btn-ghost" onClick={() => { setAuth(null); setAlerts([]) }}>Sign out</button>
    </div>
  )
}
