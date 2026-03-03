import { useState, useEffect } from 'react'
import api from '../api/client'

function MetricCard({ label, value, subtitle, color = 'text-white', trend }) {
  return (
    <div className="card-hover">
      <div className="text-sm text-gray-400 mb-1">{label}</div>
      <div className={`stat-value ${color}`}>{value}</div>
      {subtitle && <div className="text-sm text-gray-500 mt-1">{subtitle}</div>}
      {trend && (
        <div className={`text-xs mt-2 ${trend > 0 ? 'text-emerald-400' : 'text-red-400'}`}>
          {trend > 0 ? '↑' : '↓'} {Math.abs(trend)}% vs last month
        </div>
      )}
    </div>
  )
}

function ProgressRing({ percent, label, color }) {
  const radius = 40
  const circumference = 2 * Math.PI * radius
  const offset = circumference - (percent / 100) * circumference
  return (
    <div className="flex flex-col items-center">
      <svg width="100" height="100" className="-rotate-90">
        <circle cx="50" cy="50" r={radius} fill="none" stroke="#1e3a5f" strokeWidth="8" />
        <circle
          cx="50" cy="50" r={radius} fill="none" stroke={color} strokeWidth="8"
          strokeDasharray={circumference} strokeDashoffset={offset}
          strokeLinecap="round" className="transition-all duration-1000"
        />
      </svg>
      <div className="text-2xl font-bold -mt-16 mb-4">{percent}%</div>
      <div className="text-xs text-gray-400 mt-2">{label}</div>
    </div>
  )
}

function ComplianceBar({ label, compliant, partial, nonCompliant }) {
  const total = compliant + partial + nonCompliant
  if (total === 0) return null
  return (
    <div className="mb-4">
      <div className="flex justify-between text-sm mb-1">
        <span className="text-gray-300">{label}</span>
        <span className="text-gray-500">{total} bundles</span>
      </div>
      <div className="flex h-3 rounded-full overflow-hidden bg-forge-800">
        <div className="bg-emerald-500 transition-all" style={{ width: `${(compliant / total) * 100}%` }} />
        <div className="bg-amber-500 transition-all" style={{ width: `${(partial / total) * 100}%` }} />
        <div className="bg-red-500 transition-all" style={{ width: `${(nonCompliant / total) * 100}%` }} />
      </div>
      <div className="flex gap-4 mt-1 text-xs text-gray-500">
        <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-emerald-500"></span> Compliant ({compliant})</span>
        <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-amber-500"></span> Partial ({partial})</span>
        <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-red-500"></span> Non-compliant ({nonCompliant})</span>
      </div>
    </div>
  )
}

function RiskHeatmap() {
  const cells = [
    { app: 'SAP Finance', orphan: 1, ungoverned: 2, risk: 'low' },
    { app: 'Reporting', orphan: 3, ungoverned: 5, risk: 'medium' },
    { app: 'HR Suite', orphan: 0, ungoverned: 1, risk: 'low' },
    { app: 'Payroll', orphan: 0, ungoverned: 0, risk: 'low' },
    { app: 'ITSM', orphan: 0, ungoverned: 0, risk: 'low' },
    { app: 'Legacy Systems', orphan: 8, ungoverned: 12, risk: 'high' },
    { app: 'Citrix Apps', orphan: 4, ungoverned: 6, risk: 'medium' },
    { app: 'VPN/Network', orphan: 2, ungoverned: 3, risk: 'medium' },
  ]
  const riskColor = { low: 'bg-emerald-500/20 border-emerald-500/30', medium: 'bg-amber-500/20 border-amber-500/30', high: 'bg-red-500/20 border-red-500/30' }
  const textColor = { low: 'text-emerald-400', medium: 'text-amber-400', high: 'text-red-400' }

  return (
    <div className="grid grid-cols-4 gap-2">
      {cells.map(c => (
        <div key={c.app} className={`p-3 rounded-lg border ${riskColor[c.risk]}`}>
          <div className="text-xs font-medium text-gray-300 truncate">{c.app}</div>
          <div className={`text-lg font-bold ${textColor[c.risk]}`}>{c.orphan + c.ungoverned}</div>
          <div className="text-[10px] text-gray-500">{c.orphan} orphan, {c.ungoverned} ungoverned</div>
        </div>
      ))}
    </div>
  )
}

export default function CISODashboard() {
  const [data, setData] = useState(null)
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function load() {
      try {
        const [dash, sum] = await Promise.all([
          api.complianceDashboard().catch(() => null),
          api.entitlementSummary().catch(() => null),
        ])
        setData(dash)
        setSummary(sum)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  // Demo fallback data if API isn't connected
  const d = data || { totalBundles: 49, activeBundles: 34, bundlesCompliant: 28, bundlesPartiallyCompliant: 12, bundlesNonCompliant: 3 }
  const s = summary || { total: 4500, governed: 900, mapped: 340, discovered: 1960, orphan: 1300, deprecated: 0 }
  const coveragePercent = s.total > 0 ? Math.round(((s.governed + s.mapped) / s.total) * 100) : 0
  const governedPercent = s.total > 0 ? Math.round((s.governed / s.total) * 100) : 0
  const bundleCompliance = d.totalBundles > 0 ? Math.round((d.bundlesCompliant / d.totalBundles) * 100) : 0

  return (
    <div>
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-white">Executive Dashboard</h1>
          <p className="text-gray-400 text-sm mt-1">IAM Governance posture at a glance</p>
        </div>
        <div className="flex items-center gap-3">
          <span className="badge-green">Live</span>
          <span className="text-xs text-gray-500">Last sync: {new Date().toLocaleTimeString()}</span>
        </div>
      </div>

      {/* Top-level metrics */}
      <div className="grid grid-cols-5 gap-4 mb-8">
        <MetricCard label="Total Security Groups" value={s.total?.toLocaleString()} subtitle="Across all AD domains" trend={-3} />
        <MetricCard label="Governed by Roles" value={`${coveragePercent}%`} color="text-emerald-400" subtitle={`${(s.governed + s.mapped)?.toLocaleString()} of ${s.total?.toLocaleString()}`} trend={12} />
        <MetricCard label="Orphan SGs" value={s.orphan?.toLocaleString()} color="text-red-400" subtitle="No owner, no mapping" trend={-8} />
        <MetricCard label="Active Bundles" value={d.activeBundles} color="text-forge-400" subtitle={`${d.totalBundles} total`} trend={15} />
        <MetricCard label="KC27 Compliant" value={`${bundleCompliance}%`} color={bundleCompliance >= 70 ? 'text-emerald-400' : 'text-amber-400'} subtitle="Of active bundles" trend={5} />
      </div>

      {/* Middle row */}
      <div className="grid grid-cols-3 gap-6 mb-8">
        {/* Coverage rings */}
        <div className="card">
          <h3 className="text-sm font-medium text-gray-400 mb-6">Governance Coverage</h3>
          <div className="flex justify-around">
            <ProgressRing percent={governedPercent} label="SailPoint Governed" color="#10b981" />
            <ProgressRing percent={coveragePercent} label="Role-Mapped" color="#3b82f6" />
            <ProgressRing percent={bundleCompliance} label="KC27 Compliant" color="#8b5cf6" />
          </div>
        </div>

        {/* Compliance bar */}
        <div className="card">
          <h3 className="text-sm font-medium text-gray-400 mb-4">Bundle Compliance Status</h3>
          <ComplianceBar label="Access Bundles" compliant={d.bundlesCompliant} partial={d.bundlesPartiallyCompliant} nonCompliant={d.bundlesNonCompliant} />
          <div className="mt-6 space-y-2">
            <div className="flex justify-between text-sm">
              <span className="text-gray-400">Bundles pending review</span>
              <span className="text-amber-400 font-medium">7</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-gray-400">Recertification due (30d)</span>
              <span className="text-red-400 font-medium">4</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-gray-400">New bundles this month</span>
              <span className="text-emerald-400 font-medium">6</span>
            </div>
          </div>
        </div>

        {/* Entitlement breakdown */}
        <div className="card">
          <h3 className="text-sm font-medium text-gray-400 mb-4">Entitlement Classification</h3>
          <div className="space-y-3">
            {[
              { label: 'Governed (SailPoint)', count: s.governed, pct: Math.round((s.governed / s.total) * 100), color: 'bg-emerald-500' },
              { label: 'Mapped (CMDB matched)', count: s.mapped, pct: Math.round((s.mapped / s.total) * 100), color: 'bg-blue-500' },
              { label: 'Discoverable', count: s.discovered, pct: Math.round((s.discovered / s.total) * 100), color: 'bg-amber-500' },
              { label: 'Orphan (unknown)', count: s.orphan, pct: Math.round((s.orphan / s.total) * 100), color: 'bg-red-500' },
            ].map(row => (
              <div key={row.label}>
                <div className="flex justify-between text-sm mb-1">
                  <span className="text-gray-300">{row.label}</span>
                  <span className="text-gray-400">{row.count?.toLocaleString()} ({row.pct}%)</span>
                </div>
                <div className="progress-bar">
                  <div className={`progress-fill ${row.color}`} style={{ width: `${row.pct}%` }} />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Bottom row */}
      <div className="grid grid-cols-2 gap-6">
        {/* Risk heatmap */}
        <div className="card">
          <h3 className="text-sm font-medium text-gray-400 mb-4">Application Risk Heatmap</h3>
          <p className="text-xs text-gray-600 mb-3">Ungoverned entitlements by application area</p>
          <RiskHeatmap />
        </div>

        {/* Recent activity */}
        <div className="card">
          <h3 className="text-sm font-medium text-gray-400 mb-4">Recent Governance Activity</h3>
          <div className="space-y-3">
            {[
              { time: '2 min ago', action: 'Bundle approved', detail: 'Finance-Analyst-EMEA v2', badge: 'badge-green', badgeText: 'Approved' },
              { time: '15 min ago', action: 'Role mining completed', detail: 'IT Ops department — 4 roles suggested', badge: 'badge-blue', badgeText: 'Mining' },
              { time: '1 hour ago', action: 'KC27 assessment ran', detail: 'HR-Admin-Global — 2 gaps found', badge: 'badge-yellow', badgeText: 'Assessment' },
              { time: '2 hours ago', action: 'Discovery scan', detail: '12 new SGs found in AD sync', badge: 'badge-purple', badgeText: 'Discovery' },
              { time: '3 hours ago', action: 'Bundle activated', detail: 'HR-Specialist-Global v1', badge: 'badge-green', badgeText: 'Active' },
              { time: 'Yesterday', action: 'ServiceNow catalog updated', detail: '3 new request items published', badge: 'badge-blue', badgeText: 'SNOW' },
            ].map((item, i) => (
              <div key={i} className="flex items-start gap-3 py-2 border-b border-forge-800/30 last:border-0">
                <span className="text-xs text-gray-600 w-20 shrink-0 pt-0.5">{item.time}</span>
                <div className="flex-1">
                  <div className="text-sm text-gray-300">{item.action}</div>
                  <div className="text-xs text-gray-500">{item.detail}</div>
                </div>
                <span className={item.badge}>{item.badgeText}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
