import { Component, type ErrorInfo, type ReactNode } from 'react'
import { RefreshCw } from 'lucide-react'

interface Props {
  children: ReactNode
}

interface State {
  hasError: boolean
}

export default class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false }

  static getDerivedStateFromError(): State {
    return { hasError: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('TaskFlow render error:', error, info.componentStack)
  }

  render() {
    if (this.state.hasError) {
      return (
        <main className="app-error">
          <span className="error-mark"><RefreshCw size={20} /></span>
          <p className="eyebrow">SOMETHING GOT OFF TRACK</p>
          <h1>Let's get you back in flow.</h1>
          <p>TaskFlow hit an unexpected display error. Your saved preferences are still here.</p>
          <button className="button button-primary" onClick={() => window.location.reload()}><RefreshCw size={14} /> Reload TaskFlow</button>
        </main>
      )
    }
    return this.props.children
  }
}
