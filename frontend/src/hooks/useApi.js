import { useCallback, useEffect, useRef, useState } from 'react'

/** Runs an async function whenever `deps` change. Returns { data, error, loading, reload }. */
export function useApi(fn, deps) {
  const [state, setState] = useState({ data: null, error: null, loading: true })
  const run = useRef(0)

  const load = useCallback(() => {
    const id = ++run.current
    setState((s) => ({ ...s, loading: true, error: null }))
    fn().then(
      (data) => id === run.current && setState({ data, error: null, loading: false }),
      (error) => id === run.current && setState({ data: null, error, loading: false }),
    )
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)

  useEffect(() => {
    load()
    return () => {
      run.current++
    }
  }, [load])

  return { ...state, reload: load }
}
