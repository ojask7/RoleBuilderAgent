import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

export default function Login() {
  const [user, setUser] = useState('agent')
  const [pass, setPass] = useState('agent-secret')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  async function handleSubmit(e) {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const res = await fetch('/actuator/health')
      if (!res.ok) throw new Error('API not reachable')
      localStorage.setItem('af_user', user)
      localStorage.setItem('af_pass', pass)
      navigate('/')
    } catch {
      // Allow login even if backend is down (demo mode)
      localStorage.setItem('af_user', user)
      localStorage.setItem('af_pass', pass)
      navigate('/')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-forge-950">
      <div className="w-full max-w-md">
        {/* Brand */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-forge-600 rounded-2xl mb-4">
            <span className="text-3xl font-bold text-white">F</span>
          </div>
          <h1 className="text-3xl font-bold text-white tracking-tight">AccessForge</h1>
          <p className="text-forge-400 mt-2">Intelligent Role Governance Platform</p>
          <p className="text-xs text-forge-600 mt-1">From AD Chaos to Governed Access in Days</p>
        </div>

        {/* Login form */}
        <form onSubmit={handleSubmit} className="card space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-400 mb-1">Username</label>
            <input
              type="text"
              value={user}
              onChange={e => setUser(e.target.value)}
              className="w-full bg-forge-800 border border-forge-700 rounded-lg px-4 py-2.5 text-white focus:outline-none focus:border-forge-500"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-400 mb-1">Password</label>
            <input
              type="password"
              value={pass}
              onChange={e => setPass(e.target.value)}
              className="w-full bg-forge-800 border border-forge-700 rounded-lg px-4 py-2.5 text-white focus:outline-none focus:border-forge-500"
              required
            />
          </div>
          {error && <p className="text-red-400 text-sm">{error}</p>}
          <button
            type="submit"
            disabled={loading}
            className="w-full btn-primary py-3 text-base"
          >
            {loading ? 'Connecting...' : 'Sign In to AccessForge'}
          </button>
          <p className="text-xs text-gray-600 text-center">
            Connects to your AccessForge API instance
          </p>
        </form>

        {/* Footer */}
        <p className="text-center text-xs text-forge-700 mt-6">
          AccessForge v1.0 — Enterprise IAM Role Governance
        </p>
      </div>
    </div>
  )
}
