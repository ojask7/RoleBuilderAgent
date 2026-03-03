import { useState, useEffect } from 'react'
import api from '../api/client'

const STATUS_CONFIG = {
  GOVERNED:   { badge: 'badge-green',  label: 'Governed' },
  MAPPED:     { badge: 'badge-blue',   label: 'Mapped' },
  DISCOVERED: { badge: 'badge-yellow', label: 'Discovered' },
  ORPHAN:     { badge: 'badge-red',    label: 'Orphan' },
  DEPRECATED: { badge: 'badge-gray',   label: 'Deprecated' },
}

// Demo data fallback
const DEMO_ENTITLEMENTS = [
  { id: 1, sourceSgName: 'CH_SG_SAP_FI_STG_Read', sourceSystem: 'AD', applicationService: 'SAP-FI-STG', businessApp: 'SAP-Finance', owner: 'alice@corp.com', status: 'GOVERNED', confidence: 0.98 },
  { id: 2, sourceSgName: 'CH_SG_SAP_FI_PRD_Read', sourceSystem: 'AD', applicationService: 'SAP-FI-PRD', businessApp: 'SAP-Finance', owner: 'alice@corp.com', status: 'GOVERNED', confidence: 0.98 },
  { id: 3, sourceSgName: 'SG_SAP_RFC_FI_ReadOnly', sourceSystem: 'AD', applicationService: 'SAP-FI-PRD', businessApp: 'SAP-Finance', owner: 'alice@corp.com', status: 'MAPPED', confidence: 0.85 },
  { id: 4, sourceSgName: 'SG_PowerBI_Finance_View', sourceSystem: 'AD', applicationService: 'PowerBI-Finance', businessApp: 'Reporting', owner: 'bob@corp.com', status: 'GOVERNED', confidence: 0.95 },
  { id: 5, sourceSgName: 'SG_Legacy_Reports', sourceSystem: 'AD', applicationService: 'LegacyReports', businessApp: 'Reporting', owner: 'john@corp.com', status: 'DISCOVERED', confidence: 0.72 },
  { id: 6, sourceSgName: 'SG_ITSM_SelfService', sourceSystem: 'AD', applicationService: 'ITSM-Portal', businessApp: 'ITSM', owner: 'svcdesk@corp.com', status: 'GOVERNED', confidence: 0.99 },
  { id: 7, sourceSgName: 'SG_HR_Workday_Read', sourceSystem: 'AD', applicationService: 'Workday-HR', businessApp: 'HR-Suite', owner: 'hr-admin@corp.com', status: 'GOVERNED', confidence: 0.97 },
  { id: 8, sourceSgName: 'SG_Unknown_Legacy_42', sourceSystem: 'AD', applicationService: null, businessApp: null, owner: null, status: 'ORPHAN', confidence: 0.0 },
  { id: 9, sourceSgName: 'SG_Unknown_Legacy_88', sourceSystem: 'AD', applicationService: null, businessApp: null, owner: null, status: 'ORPHAN', confidence: 0.0 },
  { id: 10, sourceSgName: 'SG_AD_DL_Finance_All', sourceSystem: 'AD', applicationService: null, businessApp: null, owner: null, status: 'ORPHAN', confidence: 0.10 },
  { id: 11, sourceSgName: 'SG_Citrix_Finance_App', sourceSystem: 'AD', applicationService: null, businessApp: 'SAP-Finance', owner: null, status: 'DISCOVERED', confidence: 0.55 },
  { id: 12, sourceSgName: 'SG_VPN_CorpNetwork', sourceSystem: 'AD', applicationService: null, businessApp: null, owner: 'netops@corp.com', status: 'MAPPED', confidence: 0.60 },
]

export default function Discovery() {
  const [entitlements, setEntitlements] = useState(DEMO_ENTITLEMENTS)
  const [filter, setFilter] = useState('ALL')
  const [discoverInput, setDiscoverInput] = useState('')
  const [discovering, setDiscovering] = useState(false)
  const [summary, setSummary] = useState(null)

  useEffect(() => {
    api.entitlementSummary().then(setSummary).catch(() => {})
  }, [])

  const filtered = filter === 'ALL' ? entitlements : entitlements.filter(e => e.status === filter)

  async function handleDiscover() {
    if (!discoverInput.trim()) return
    setDiscovering(true)
    try {
      const sgNames = discoverInput.split(',').map(s => s.trim()).filter(Boolean)
      await api.discover(sgNames)
      setDiscoverInput('')
    } catch {
      // demo mode — simulate discovery
      const newEntitlements = discoverInput.split(',').map((sg, i) => ({
        id: entitlements.length + i + 1,
        sourceSgName: sg.trim(),
        sourceSystem: 'AD',
        applicationService: null,
        businessApp: null,
        owner: null,
        status: 'DISCOVERED',
        confidence: 0.45,
      }))
      setEntitlements([...newEntitlements, ...entitlements])
      setDiscoverInput('')
    } finally {
      setDiscovering(false)
    }
  }

  const s = summary || { total: entitlements.length, governed: 4, mapped: 2, discovered: 3, orphan: 3 }

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-white">Entitlement Discovery</h1>
          <p className="text-gray-400 text-sm mt-1">Discover, classify, and map every Security Group across your environment</p>
        </div>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-5 gap-3 mb-6">
        {[
          { label: 'Total SGs', count: s.total, color: 'text-white' },
          { label: 'Governed', count: s.governed, color: 'text-emerald-400' },
          { label: 'Mapped', count: s.mapped, color: 'text-blue-400' },
          { label: 'Discovered', count: s.discovered, color: 'text-amber-400' },
          { label: 'Orphan', count: s.orphan, color: 'text-red-400' },
        ].map(c => (
          <div key={c.label} className="card-hover text-center cursor-pointer" onClick={() => setFilter(c.label === 'Total SGs' ? 'ALL' : c.label.toUpperCase())}>
            <div className={`text-2xl font-bold ${c.color}`}>{c.count}</div>
            <div className="text-xs text-gray-400 mt-1">{c.label}</div>
          </div>
        ))}
      </div>

      {/* Discovery input */}
      <div className="card mb-6">
        <h3 className="text-sm font-medium text-gray-400 mb-3">Discover New Security Groups</h3>
        <div className="flex gap-3">
          <input
            value={discoverInput}
            onChange={e => setDiscoverInput(e.target.value)}
            placeholder="Enter SG names (comma-separated): SG_NEW_APP_READ, SG_NEW_APP_WRITE"
            className="flex-1 bg-forge-800 border border-forge-700 rounded-lg px-4 py-2.5 text-white text-sm focus:outline-none focus:border-forge-500"
          />
          <button onClick={handleDiscover} disabled={discovering} className="btn-primary whitespace-nowrap">
            {discovering ? 'Scanning...' : 'Run Discovery'}
          </button>
        </div>
        <p className="text-xs text-gray-600 mt-2">Cross-references AD, SailPoint, CMDB, and MDT to classify each SG</p>
      </div>

      {/* Filter tabs */}
      <div className="flex gap-2 mb-4">
        {['ALL', 'GOVERNED', 'MAPPED', 'DISCOVERED', 'ORPHAN'].map(f => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
              filter === f ? 'bg-forge-600 text-white' : 'bg-forge-800/50 text-gray-400 hover:text-white'
            }`}
          >
            {f === 'ALL' ? 'All' : f.charAt(0) + f.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {/* Entitlement table */}
      <div className="card overflow-hidden p-0">
        <table className="w-full">
          <thead>
            <tr className="bg-forge-800/30">
              <th className="table-header px-4 py-3">Security Group</th>
              <th className="table-header px-4 py-3">Source</th>
              <th className="table-header px-4 py-3">Application</th>
              <th className="table-header px-4 py-3">Business App</th>
              <th className="table-header px-4 py-3">Owner</th>
              <th className="table-header px-4 py-3">Status</th>
              <th className="table-header px-4 py-3">Confidence</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map(e => {
              const cfg = STATUS_CONFIG[e.status] || STATUS_CONFIG.ORPHAN
              return (
                <tr key={e.id} className="table-row">
                  <td className="px-4 py-3 text-sm font-mono text-gray-200">{e.sourceSgName}</td>
                  <td className="px-4 py-3 text-sm text-gray-400">{e.sourceSystem}</td>
                  <td className="px-4 py-3 text-sm text-gray-300">{e.applicationService || '—'}</td>
                  <td className="px-4 py-3 text-sm text-gray-300">{e.businessApp || '—'}</td>
                  <td className="px-4 py-3 text-sm text-gray-400">{e.owner || <span className="text-red-400">Unowned</span>}</td>
                  <td className="px-4 py-3"><span className={cfg.badge}>{cfg.label}</span></td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <div className="w-16 progress-bar">
                        <div
                          className={`progress-fill ${e.confidence >= 0.8 ? 'bg-emerald-500' : e.confidence >= 0.5 ? 'bg-amber-500' : 'bg-red-500'}`}
                          style={{ width: `${e.confidence * 100}%` }}
                        />
                      </div>
                      <span className="text-xs text-gray-500">{(e.confidence * 100).toFixed(0)}%</span>
                    </div>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}
