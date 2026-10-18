import { AlertTriangle, Loader2, RefreshCw } from 'lucide-react'

export function Loading({ label = 'Loading' }) {
  return (
    <div role="status" aria-live="polite" className="flex items-center gap-3 p-6 text-ink-soft">
      <Loader2 className="h-5 w-5 animate-spin" aria-hidden="true" />
      <span>{label}…</span>
    </div>
  )
}

export function ErrorState({ error, onRetry, title = 'Could not load this data' }) {
  return (
    <div role="alert" className="flex flex-col gap-3 rounded-2xl border border-risk-veryHigh/40 bg-risk-veryHigh/5 p-5">
      <div className="flex items-center gap-2 font-semibold text-risk-veryHigh">
        <AlertTriangle className="h-5 w-5" aria-hidden="true" />
        {title}
      </div>
      <p className="text-sm text-ink-soft">{error?.message || 'Something went wrong.'}</p>
      {onRetry && (
        <button type="button" className="btn-ghost self-start" onClick={onRetry}>
          <RefreshCw className="h-4 w-4" aria-hidden="true" /> Try again
        </button>
      )}
    </div>
  )
}

export function Panel({ title, action, children, className = '' }) {
  return (
    <section className={`card card-pad ${className}`}>
      {(title || action) && (
        <div className="mb-4 flex items-center justify-between gap-3">
          {title && <h2 className="text-lg font-semibold">{title}</h2>}
          {action}
        </div>
      )}
      {children}
    </section>
  )
}

export function PageHeader({ title, children }) {
  return (
    <div className="mb-6">
      <h1 className="text-2xl font-semibold sm:text-3xl">{title}</h1>
      {children && <p className="mt-1 max-w-3xl text-ink-soft">{children}</p>}
    </div>
  )
}

export function Stat({ icon: Icon, label, value }) {
  return (
    <div className="flex items-center gap-3">
      {Icon && <Icon className="h-5 w-5 text-sky-400" aria-hidden="true" />}
      <div>
        <div className="muted">{label}</div>
        <div className="font-semibold">{value}</div>
      </div>
    </div>
  )
}
